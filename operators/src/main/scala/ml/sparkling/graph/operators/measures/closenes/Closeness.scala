package ml.sparkling.graph.operators.measures.closenes

import ml.sparkling.graph.api.operators.measures.{VertexMeasure, VertexMeasureConfiguration}
import ml.sparkling.graph.operators.algorithms.shortestpaths.ShortestPathsAlgorithm
import ml.sparkling.graph.operators.measures.closenes.ClosenessUtils._
import org.apache.spark.graphx.Graph

import scala.reflect.ClassTag

/**
 * Created by Roman Bartusiak (roman.bartusiak@pwr.edu.pl http://riomus.github.io).
 * Computes closeness centrality in standard and harmonic versions
 */
object Closeness extends VertexMeasure[Double] {
  /**
   * Generic closeness computation method, should be used for extensions. Computations are done using super-step approach
   * @param graph - computation graph
   * @param closenessFunction - function that calculates closeness for vertex
   * @param vertexMeasureConfiguration - configuration of computation
   * @param num - numeric for @ED
   * @tparam VD - vertex data type
   * @tparam ED - edge data type
   * @return graph where each vertex is associated with its  closeness centrality computed using @closenessFunction
   */
  def computeUsing[VD: ClassTag, ED: ClassTag](graph: Graph[VD, ED],
                                               closenessFunction: ClosenessFunction,
                                               vertexMeasureConfiguration: VertexMeasureConfiguration[VD, ED])(implicit num: Numeric[ED]): Graph[Double, ED] = {
    val graphSize = graph.numVertices
    var distanceSumGraph = graph.mapVertices((vId, data) => (0l, 0d))
    (1l until graphSize + 1).foreach(startVid => {
      val shortestPaths = ShortestPathsAlgorithm.computeSingleShortestPathsLengths(graph, startVid, treatAsUndirected = vertexMeasureConfiguration.treatAsUndirected)
      distanceSumGraph = distanceSumGraph.outerJoinVertices(shortestPaths.vertices)((vId, oldValue, newValue) => {
        val newValueMapped = newValue.map {
          case 0d => (0l, 0d)
          case other => (1l, other)
        }
        (oldValue, newValueMapped) match {
          case ((oldPathsCount, oldPathsSum), Some((newPathsCount, newPathsSum))) => {
            val pathCountOut = oldPathsCount + newPathsCount
            val pathLengthSumOut = oldPathsSum + newPathsSum
            (pathCountOut, pathLengthSumOut)
          }
          case (_, None) => oldValue
        }
      })
      shortestPaths.unpersist()
    })
    distanceSumGraph.mapVertices((vId, sum) => closenessFunction.tupled(sum))
  }

  /**
   * Computes harmonic closeness centrality
   * @param graph - computation graph
   * @param vertexMeasureConfiguration - configuration of computation
   * @param num - numeric for @ED
   * @tparam VD - vertex data type
   * @tparam ED - edge data type
   * @return graph where each vertex is associated with its harmonic closeness centrality
   */
  def computeHarmonic[VD: ClassTag, ED: ClassTag](graph: Graph[VD, ED], vertexMeasureConfiguration: VertexMeasureConfiguration[VD, ED])(implicit num: Numeric[ED]) = {
    computeUsing(graph, harmonicCloseness(graph.numVertices) _, vertexMeasureConfiguration)
  }

  /**
   * Computes standard closeness centrality
   * @param graph - computation graph
   * @param vertexMeasureConfiguration - configuration of computation
   * @param num - numeric for @ED
   * @tparam VD - vertex data type
   * @tparam ED - edge data type
   * @return graph where each vertex is associated with its standard closeness centrality
   */
  override def compute[VD: ClassTag, ED: ClassTag](graph: Graph[VD, ED], vertexMeasureConfiguration: VertexMeasureConfiguration[VD, ED])(implicit num: Numeric[ED]): Graph[Double, ED] = computeUsing(graph, standardCloseness(graph.numVertices) _, vertexMeasureConfiguration)
}
