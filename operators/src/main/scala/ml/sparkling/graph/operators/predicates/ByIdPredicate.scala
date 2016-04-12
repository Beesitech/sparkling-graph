package ml.sparkling.graph.operators.predicates

import ml.sparkling.graph.api.operators.IterativeComputation
import ml.sparkling.graph.api.operators.IterativeComputation.VertexPredicate
import org.apache.spark.graphx.VertexId

/**
 * Created by Roman Bartusiak (roman.bartusiak@pwr.edu.pl http://riomus.github.io).
 * Predicate for single vertexId
 */
case class ByIdPredicate(vertex:VertexId) extends VertexPredicate with Serializable {
  override def apply(v1: VertexId): Boolean = v1==vertex

}
