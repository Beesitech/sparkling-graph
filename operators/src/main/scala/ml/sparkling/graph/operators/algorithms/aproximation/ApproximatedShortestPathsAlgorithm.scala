package ml.sparkling.graph.operators.algorithms.aproximation

import it.unimi.dsi.fastutil.longs.Long2DoubleOpenHashMap
import ml.sparkling.graph.api.operators.IterativeComputation._
import ml.sparkling.graph.api.operators.algorithms.coarsening.CoarseningAlgorithm.Component
import ml.sparkling.graph.api.operators.algorithms.shortestpaths.ShortestPathsTypes.{JDouble, JLong, JMap}
import ml.sparkling.graph.operators.algorithms.coarsening.labelpropagation.LPCoarsening
import ml.sparkling.graph.operators.algorithms.shortestpaths.pathprocessors.{PathProcessor, SingleVertexProcessor}
import ml.sparkling.graph.operators.algorithms.shortestpaths.pathprocessors.fastutils.{FastUtilWithDistance, FastUtilWithPath}
import ml.sparkling.graph.operators.predicates.{AllPathPredicate, ByIdPredicate, ByIdsPredicate}
import org.apache.spark.graphx.{EdgeTriplet, Graph, _}
import ml.sparkling.graph.operators.algorithms.shortestpaths.ShortestPathsAlgorithm
import ml.sparkling.graph.operators.algorithms.shortestpaths.pathprocessors.fastutils.FastUtilWithDistance.DataMap

import scala.collection.JavaConversions._
import scala.reflect.ClassTag

/**
  * Created by  Roman Bartusiak <riomus@gmail.com> on 07.02.17.
  */
object AproximatedShortestPathsAlgorithm  {

  type PathModifier=(VertexId,VertexId,JDouble)=>JDouble

  val DefaultPathModifier:PathModifier=(fromVertex:VertexId,toVertex:VertexId,path:JDouble)=>3*path+2

  def computeShortestPathsLengthsUsing[VD, ED: ClassTag](graph: Graph[VD, ED], vertexPredicate: VertexPredicate = AllPathPredicate, treatAsUndirected: Boolean = false,modifier:PathModifier=DefaultPathModifier)(implicit num: Numeric[ED]) = {
    val coarsedGraph=LPCoarsening.coarse(graph,treatAsUndirected)
    val coarsedShortestPaths: Graph[DataMap, ED] =ShortestPathsAlgorithm.computeShortestPathsLengths(coarsedGraph,vertexPredicate,treatAsUndirected)
    aproximatePaths(graph, coarsedGraph, coarsedShortestPaths,modifier)
  }

  def aproximatePaths[ED: ClassTag, VD](graph: Graph[VD, ED], coarsedGraph: Graph[Component, ED], coarsedShortestPaths: Graph[DataMap, ED],modifier:PathModifier=DefaultPathModifier): Graph[DataMap, ED] = {
    val modifiedCoarsedPaths = coarsedShortestPaths.vertices.map {
      case (vertexId: VertexId, paths: DataMap) => {
        val out = paths.clone()
        paths.keySet().foreach { (key: JLong) => {
          out.put(key, modifier(vertexId, key, paths.get(key)))
        }
        }
        (vertexId, out)
      }
    }
    val estimatedPaths = coarsedGraph.vertices.join(modifiedCoarsedPaths).flatMap {
      case (coarsedComponentId, (component, paths)) => component.map((componentPart) => (componentPart, paths))
    }
    Graph(estimatedPaths, graph.edges)
  }

  def computeSingleShortestPathsLengths[VD, ED: ClassTag](graph: Graph[VD, ED], vertexId: VertexId, treatAsUndirected: Boolean = false, modifier:PathModifier=DefaultPathModifier)(implicit num: Numeric[ED]) = {
    computeShortestPathsLengthsUsing(graph,(vId:VertexId)=>vId==vertexId,treatAsUndirected,modifier=DefaultPathModifier)
  }

  def computeShortestPaths[VD, ED: ClassTag](graph: Graph[VD, ED], vertexPredicate: VertexPredicate = AllPathPredicate, treatAsUndirected: Boolean = false,modifier:PathModifier=DefaultPathModifier)(implicit num: Numeric[ED]) = {
    computeShortestPathsLengthsUsing(graph,vertexPredicate,treatAsUndirected,modifier=DefaultPathModifier)
  }

  def computeShortestPathsLengthsIterative[VD: ClassTag, ED: ClassTag](graph: Graph[VD, ED], bucketSizeProvider: BucketSizeProvider[VD,ED], treatAsUndirected: Boolean = false,modifier:PathModifier=DefaultPathModifier)(implicit num: Numeric[ED]) = {
    val coarsedGraph=LPCoarsening.coarse(graph,treatAsUndirected)
    val coarsedShortestPaths: Graph[DataMap, ED] =ShortestPathsAlgorithm.computeShortestPathsLengthsIterative(coarsedGraph,bucketSizeProvider,treatAsUndirected)
    aproximatePaths(graph, coarsedGraph, coarsedShortestPaths,modifier)
  }

}
