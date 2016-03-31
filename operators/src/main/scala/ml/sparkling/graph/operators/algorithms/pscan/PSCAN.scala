package ml.sparkling.graph.operators.algorithms.pscan

import ml.sparkling.graph.operators.measures.utils.CollectionsUtils._
import ml.sparkling.graph.operators.measures.utils.NeighboursUtils
import ml.sparkling.graph.operators.measures.utils.NeighboursUtils.NeighbourSet
import org.apache.spark.graphx.Graph

import scala.reflect.ClassTag
import scala.util.Try

/**
 * Created by Roman Bartusiak (roman.bartusiak@pwr.edu.pl http://riomus.github.io).
 */
object PSCAN {

  implicit class DSL[VD:ClassTag,ED:ClassTag](graph:Graph[VD,ED]){
    def PSCAN(epsilon:Double=0.72):Graph[ComponentID,ED]={
      computeConnectedComponents(graph,epsilon)
    }
  }
  

  type ComponentID=Long
  case class PSCANData(componentID: ComponentID,isActive:Boolean)


  def computeConnectedComponents[VD:ClassTag,ED:ClassTag](graph:Graph[VD,ED],epsilon:Double=0.72):Graph[ComponentID,ED]={

    val neighbours: Graph[NeighbourSet, ED] = NeighboursUtils.getWithNeighbours(graph,treatAsUndirected = true)
    val edgesWithSimilarity=neighbours.mapTriplets(edge=>{
      val sizeOfIntersection=intersectSize(edge.srcAttr,edge.dstAttr)
      val denominator = edge.srcAttr.size()*edge.dstAttr.size()
      sizeOfIntersection/Math.sqrt(denominator)
    })
    val cutOffGraph=edgesWithSimilarity.filter[NeighbourSet, Double](
      preprocess=g=>g,
      epred=edge=>{
      edge.attr>epsilon
    })

    val startGraph=cutOffGraph.mapVertices{
      case (vId,data)=>PSCANData(vId,true)
    }

     val graphWithComponents=startGraph.pregel[List[ComponentID]](List.empty[Long])(
      vprog=(vId,data,messages)=>{
       val out= Try(messages.min).map(newId=>{
        if(newId<data.componentID)
          PSCANData(newId,true)
          else
          PSCANData(data.componentID,false)
        }
        ).getOrElse(PSCANData(data.componentID,data.isActive))
        out
      },
      sendMsg=(edge)=>{
        if(edge.srcAttr.isActive){
          Iterator((edge.dstId,List(edge.srcAttr.componentID)),(edge.srcId,List(edge.srcAttr.componentID)))
          }
        else
          Iterator()
      },
      mergeMsg=(a,b)=>a:::b
      )
    val verticesWithComponents=graphWithComponents.mapVertices{
      case (vId,data)=>data.componentID
    }.vertices

    graph.outerJoinVertices(verticesWithComponents)((vId,oldData,newData)=>newData.get)
  }

}
