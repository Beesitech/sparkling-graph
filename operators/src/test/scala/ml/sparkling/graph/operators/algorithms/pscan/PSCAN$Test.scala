package ml.sparkling.graph.operators.algorithms.pscan

import ml.sparkling.graph.operators.MeasureTest
import ml.sparkling.graph.operators.algorithms.pscan.PSCAN.ComponentID
import org.apache.spark.SparkContext
import org.apache.spark.graphx.Graph
import org.scalatest.FunSuite

/**
 * Created by Roman Bartusiak (roman.bartusiak@pwr.edu.pl http://riomus.github.io).
 */
class PSCAN$Test (implicit sc:SparkContext)   extends MeasureTest {

  "Components for super simple graph" should  " be computed" in{
    Given("graph")
    val filePath = getClass.getResource("/graphs/4_nodes_full")
    val graph:Graph[Int,Int]=loadGraph(filePath.toString)
    When("Computes components")
    val components: Graph[ComponentID, Int] = PSCAN.computeConnectedComponents(graph)
    Then("Should compute components correctly")
    println(components.vertices.collect().toList)
  }

}
