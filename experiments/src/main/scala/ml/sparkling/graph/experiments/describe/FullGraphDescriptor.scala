package ml.sparkling.graph.experiments.describe

import ml.sparkling.graph.api.operators.measures.{VertexMeasureConfiguration, VertexMeasure}
import ml.sparkling.graph.operators.measures.{VertexEmbeddedness, NeighborConnectivity, Degree}
import ml.sparkling.graph.operators.measures.closenes.Closeness
import ml.sparkling.graph.operators.measures.clustering.LocalClustering
import ml.sparkling.graph.operators.measures.eigenvector.Eigenvector
import ml.sparkling.graph.operators.measures.hits.Hits
import org.apache.spark.graphx.{VertexId,  Graph}
import scala.reflect.ClassTag


/**
 * Created by Roman Bartusiak (roman.bartusiak@pwr.edu.pl http://riomus.github.io).
 */
object FullGraphDescriptor {
  private val measures: List[(String, VertexMeasure[_ >: Double with (Double, Double) with (Int, Int)])] = List(
    ("Eigenvector", Eigenvector),
    ("Hits", Hits),
    ("NeighborConnectivity", NeighborConnectivity),
    ("Closeness", Closeness),
    ("Degree", Degree),
    ("VertexEmbeddedness", VertexEmbeddedness),
    ("LocalClustering", LocalClustering)
  )

  def describeGraph[VD: ClassTag, ED: ClassTag](graph: Graph[VD, ED], vertexMeasureConfiguration: VertexMeasureConfiguration[VD, ED])(implicit num: Numeric[ED]) = {
    val cachedGraph = graph.cache()
    var outGraph: Graph[List[Any], ED] = cachedGraph.mapVertices((vId, data) => List(data))

    measures.foreach { case (measureName, measure) => {
      val graphMeasures = measure match {
        case m: VertexMeasure[Any@unchecked] => m.compute(cachedGraph, vertexMeasureConfiguration)
      }
      graphMeasures.unpersist()
      outGraph = outGraph.joinVertices(graphMeasures.vertices)(extendValueList)
    }
    }
    outGraph
  }


  def describeGraphToDirectory[VD: ClassTag, ED: ClassTag](graph: Graph[VD, ED], directory: String, vertexMeasureConfiguration: VertexMeasureConfiguration[VD, ED])(implicit num: Numeric[ED]) = {
    val cachedGraph = graph.cache()
    val outGraph: Graph[List[Any], ED] = cachedGraph.mapVertices((vId, data) => List(data))
    measures.foreach { case (measureName, measure) => {
      val graphMeasures = measure match {
        case m: VertexMeasure[Any@unchecked] => m.compute(cachedGraph, vertexMeasureConfiguration)
      }
      outGraph.outerJoinVertices(graphMeasures.vertices)(extendValueList)
        .vertices.map(t => s"${t._1};${t._2.reverse.mkString(";")}").saveAsTextFile(s"${directory}/${measureName}")
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
