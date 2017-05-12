package ml.sparkling.graph.operators.partitioning

import ml.sparkling.graph.api.operators.algorithms.community.CommunityDetection.{CommunityDetectionAlgorithm, CommunityDetectionMethod, ComponentID}
import org.apache.spark.SparkContext
import org.apache.spark.broadcast.Broadcast
import org.apache.spark.graphx.{Graph, PartitionID, PartitionStrategy, VertexId}

import scala.reflect.ClassTag

/**
 * Created by Roman Bartusiak (roman.bartusiak@pwr.edu.pl http://riomus.github.io).
 * First approach to community based graph partitioning. It is not efficient due to need of gathering vertex to component id on driver node.
 */
object CommunityBasedPartitioning {


  def partitionGraphBy[VD:ClassTag,ED:ClassTag](graph:Graph[VD,ED],communityDetectionMethod:CommunityDetectionMethod[VD,ED],numParts:Int= -1)(implicit sc:SparkContext): Graph[VD, ED] ={
    val numberOfPartitions=if (numParts== -1) sc.defaultParallelism else numParts
    val communities: Graph[ComponentID, ED] = communityDetectionMethod(graph)
    val numberOfCommunities=communities.vertices.values.distinct().collect().size
    val vertexToCommunityId: Map[VertexId, ComponentID] = communities.vertices.collect().toMap
    val (coarsedVertexMap,coarsedNumberOfPartitions) = PartitioningUtils.coarsePartitions(numberOfPartitions,numberOfCommunities,vertexToCommunityId)
    val strategy=ByComponentIdPartitionStrategy(coarsedVertexMap)
    graph.partitionBy(strategy,coarsedNumberOfPartitions)
  }


  def partitionGraphUsing[VD:ClassTag,ED:ClassTag](graph:Graph[VD,ED],communityDetectionMethod:CommunityDetectionAlgorithm,numParts:Int= -1)(implicit sc:SparkContext): Graph[VD, ED] ={
    partitionGraphBy(graph,communityDetectionMethod.detectCommunities[VD,ED](_),numParts)
  }


   case class ByComponentIdPartitionStrategy(idMap:Map[VertexId, ComponentID]) extends PartitionStrategy{
    override def getPartition(src: VertexId, dst: VertexId, numParts: PartitionID): PartitionID = {
      val vertex1Component: ComponentID = idMap(src)
      val vertex2Component: ComponentID = idMap(dst)
      Math.min(vertex1Component,vertex2Component).toInt
    }
  }
}
