package ml.sparkling.graph.experiments.describe

import ml.sparkling.graph.api.operators.IterativeComputation._
import ml.sparkling.graph.api.operators.algorithms.coarsening.CoarseningAlgorithm.Component
import ml.sparkling.graph.api.operators.measures.{VertexMeasure, VertexMeasureConfiguration}
import ml.sparkling.graph.operators.algorithms.aproximation.ApproximatedShortestPathsAlgorithm
import ml.sparkling.graph.operators.algorithms.coarsening.labelpropagation.LPCoarsening
import ml.sparkling.graph.operators.algorithms.community.pscan.PSCAN
import ml.sparkling.graph.operators.algorithms.shortestpaths.ShortestPathsAlgorithm
import ml.sparkling.graph.operators.measures.vertex.{Degree, NeighborhoodConnectivity, VertexEmbeddedness}
import ml.sparkling.graph.operators.measures.vertex.closenes.Closeness
import ml.sparkling.graph.operators.measures.vertex.clustering.LocalClustering
import ml.sparkling.graph.operators.measures.vertex.eigenvector.EigenvectorCentrality
import ml.sparkling.graph.operators.measures.vertex.hits.Hits
import org.apache.log4j.Logger
import org.apache.spark.graphx.{Graph, VertexId}

import scala.reflect.ClassTag


/**
 * Created by Roman Bartusiak (roman.bartusiak@pwr.edu.pl http://riomus.github.io).
 */
object FullGraphDescriptor {
  val logger=Logger.getLogger(FullGraphDescriptor.getClass)
  private val measures = List(
    ("Eigenvector", EigenvectorCentrality),
    ("Hits", Hits),
    ("NeighborConnectivity", NeighborhoodConnectivity),
    ("Degree", Degree),
    ("VertexEmbeddedness", VertexEmbeddedness),
    ("LocalClustering", LocalClustering),
    ("Closeness", Closeness),
    ("Shortest paths", ShortestPathsAlgorithm),
    ("Approximated shortest paths", ApproximatedShortestPathsAlgorithm),
    ("Label propagation coarsening", LPCoarsening),
    ("PSCAN", PSCAN)
  )
  def time[T](str: String)(thunk: => T): (T,Long) = {
    logger.warn(str + "... ")
    val t1 = System.currentTimeMillis
    val x = thunk
    val t2 = System.currentTimeMillis
    val diff=t2 - t1
    logger.warn(diff + " msecs")
    (x,diff)
  }




  def describeGraph[VD: ClassTag, ED: ClassTag](graph: Graph[VD, ED], vertexMeasureConfiguration: VertexMeasureConfiguration[VD, ED])(implicit num: Numeric[ED]) = {
    val cachedGraph = graph.cache()
    val outGraph: Graph[List[Any], ED] = cachedGraph.mapVertices((vId, data) => List(data))

    measures.foldLeft(outGraph) {
      case (acc, (measureName, measure)) => {
        val graphMeasures = executeOperator(graph, vertexMeasureConfiguration, cachedGraph, measure,measureName)
        graphMeasures.unpersist()
        acc.joinVertices(graphMeasures.vertices)(extendValueList)
      }
    }
  }


  def executeOperator[ED: ClassTag, VD: ClassTag](graph: Graph[VD, ED], vertexMeasureConfiguration: VertexMeasureConfiguration[VD, ED], cachedGraph: Graph[VD, ED], measure: Object,name:String)(implicit num: Numeric[ED]) = {
    time(name)(measure match {
      case m: VertexMeasure[Any@unchecked] => m.compute(cachedGraph, vertexMeasureConfiguration)
      case m@ShortestPathsAlgorithm => m.computeShortestPathsLengthsIterative(cachedGraph, vertexMeasureConfiguration.bucketSizeProvider)
      case m@ApproximatedShortestPathsAlgorithm => m.computeShortestPathsLengthsIterative(cachedGraph, wholeGraphBucket[Component, ED] _, vertexMeasureConfiguration.treatAsUndirected)
      case m@LPCoarsening => m.coarse(cachedGraph, vertexMeasureConfiguration.treatAsUndirected)
      case m@PSCAN => m.detectCommunities(graph)
    })._1
  }

  def describeGraphToDirectory[VD: ClassTag, ED: ClassTag](graph: Graph[VD, ED], directory: String, vertexMeasureConfiguration: VertexMeasureConfiguration[VD, ED])(implicit num: Numeric[ED]) = {
    val cachedGraph = graph.cache()
    val outGraph: Graph[List[Any], ED] = cachedGraph.mapVertices((vId, data) => List(data))
    measures.foreach { case (measureName, measure) => {
      val graphMeasures = executeOperator(graph, vertexMeasureConfiguration, cachedGraph, measure,measureName)
      val outputCSV = outGraph.outerJoinVertices(graphMeasures.vertices)(extendValueList)
        .vertices.map {
        case (id, data) => s"${id};${data.reverse.mkString(";")}"
      }
      outputCSV.saveAsTextFile(s"${directory}/${measureName}")
      graphMeasures.unpersist()
    }
    }
  }

  private def extendValueList(vId: VertexId, oldValue: List[Any], newValue: Any) = {
    newValue match {
      case None => oldValue
      case Some(v: String) => v :: oldValue
      case Some(v: Double) => v :: oldValue
      case Some(v: Int) => v :: oldValue
      case Some((v1: Int, v2: Int)) => v1 :: v2 :: oldValue
      case Some((v1: Double, v2: Double)) => v1 :: v2 :: oldValue
    }
  }
}
