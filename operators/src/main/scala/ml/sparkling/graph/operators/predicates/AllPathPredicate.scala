package ml.sparkling.graph.operators.predicates

import ml.sparkling.graph.api.operators.IterativeComputation
import ml.sparkling.graph.api.operators.IterativeComputation.VertexPredicate
import org.apache.spark.graphx.VertexId

/**
 * Created by Roman Bartusiak (roman.bartusiak@pwr.edu.pl http://riomus.github.io).
 * Always true predicate
 */
object AllPathPredicate extends VertexPredicate with Serializable {
  override def apply(v1: VertexId): Boolean = true
}
