package ml.sparkling.graph.operators.partitioning

import ml.sparkling.graph.api.operators.algorithms.community.CommunityDetection.ComponentID
import org.apache.log4j.Logger
import org.apache.spark.SparkContext
import org.apache.spark.graphx.{Graph, VertexId, VertexRDD}

import scala.reflect.ClassTag

/**
 * Created by Roman Bartusiak (roman.bartusiak@pwr.edu.pl http://riomus.github.io).
 * First approach to community based graph partitioning. It is not efficient due to need of gathering vertex to component id on driver node.
 */
object PropagationBasedPartitioning {

  val logger=Logger.getLogger(PropagationBasedPartitioning.getClass())

  def precomputePartitions[VD:ClassTag,ED:ClassTag](graph:Graph[VD,ED],numParts:Int= -1,checkpointingFrequency:Int=50)(implicit sc:SparkContext):((Map[VertexId, Int], Int),Map[VertexId, ComponentID])={
    val numberOfPartitions=if (numParts== -1) sc.defaultParallelism else numParts
    var operationGraph=graph.mapVertices{
      case (vId,_)=>vId
    }.cache()
    var oldComponents: VertexRDD[VertexId] =operationGraph.vertices;

    var numberOfComponents=graph.numVertices;
    var oldNumberOfComponents=Long.MaxValue;
    var iteration=0;
    while ((numberOfComponents>numberOfPartitions && numberOfComponents!=1 && oldNumberOfComponents!=numberOfComponents) || oldNumberOfComponents>Int.MaxValue){
      logger.info(s"Propagation based partitioning: iteration:$iteration, last number of components:$oldNumberOfComponents, current number of components:$numberOfComponents")
      iteration=iteration+1;
      oldComponents.unpersist(false)
      oldComponents=operationGraph.vertices
      val newIds=operationGraph.aggregateMessages[VertexId](ctx=>{
        if(ctx.srcAttr<ctx.dstAttr){
          ctx.sendToDst(ctx.srcAttr)
        }else if(ctx.dstAttr<ctx.srcAttr){
          ctx.sendToSrc(ctx.dstAttr)
        }
      },math.min)

      val newOperationGraph=operationGraph.outerJoinVertices(newIds){
        case (_,oldData,newData)=>newData.getOrElse(oldData)
      }.cache()
      operationGraph.unpersist(false)
      operationGraph=newOperationGraph
      oldNumberOfComponents=numberOfComponents
      numberOfComponents=operationGraph.vertices.map(_._2).distinct().count()
      if(iteration%checkpointingFrequency==0){
        oldComponents.checkpoint();
        operationGraph.checkpoint();
        operationGraph.vertices.foreachPartition((_)=>{})
        operationGraph.edges.foreachPartition((_)=>{})
        oldComponents.foreachPartition((_)=>{})
      }
    }
    val (communities,numberOfCommunities)=(oldComponents,oldNumberOfComponents)
    val vertexToCommunityId: Map[VertexId, ComponentID] = communities.treeAggregate(Map[VertexId,VertexId]())((agg,data)=>{agg+(data._1->data._2)},(agg1,agg2)=>agg1++agg2)
    communities.unpersist(false)
    return (PartitioningUtils.coarsePartitions(numberOfPartitions, numberOfCommunities, vertexToCommunityId),vertexToCommunityId)
  }

  def partitionGraphBy[VD:ClassTag,ED:ClassTag](graph:Graph[VD,ED],numParts:Int= -1,checkpointingFrequency:Int=50)(implicit sc:SparkContext): Graph[VD, ED] ={
    val (vertexMap: Map[VertexId, Int], newNumberOfCummunities: Int, vertexToCommunityId: Map[VertexId, ComponentID], strategy: ByComponentIdPartitionStrategy) = buildPartitioningStrategy(graph, numParts, checkpointingFrequency)
    logger.info(s"Partitioning graph using coarsed map with ${vertexMap.size} entries (${vertexToCommunityId.size} before coarse) and ${newNumberOfCummunities} partitions")
    val out=new CustomGraphPartitioningImplementation[VD,ED](graph).partitionBy(strategy).cache()
    out.edges.count()
    out.vertices.count()
    graph.unpersist(false)
    out
  }

  private def buildPartitioningStrategy[ED: ClassTag, VD: ClassTag](graph: Graph[VD, ED], numParts: Int, checkpointingFrequency: Int)(implicit sc:SparkContext) = {
    val ((vertexMap, newNumberOfCummunities), vertexToCommunityId) = precomputePartitions(graph, numParts, checkpointingFrequency);
    val strategy = ByComponentIdPartitionStrategy(vertexMap, newNumberOfCummunities)
    (vertexMap, newNumberOfCummunities, vertexToCommunityId, strategy)
  }
}
