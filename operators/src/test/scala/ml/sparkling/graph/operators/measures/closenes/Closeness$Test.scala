package ml.sparkling.graph.operators.measures.closenes

import ml.sparkling.graph.api.operators.measures.VertexMeasureConfiguration
import ml.sparkling.graph.operators.{MeasureTest, SparkTest}
import ml.sparkling.graph.operators.measures.clustering.LocalClustering
import ml.sparkling.graph.operators.measures.eigenvector.EigenvectorCentrality
import org.apache.spark.SparkContext
import org.apache.spark.graphx.lib.ShortestPaths
import org.apache.spark.graphx.{PartitionStrategy, GraphLoader, Graph}
import org.scalatest.FunSuite

/**
 * Created by Roman Bartusiak (roman.bartusiak@pwr.edu.pl http://riomus.github.io).
 */
class Closeness$Test(implicit sc:SparkContext)   extends MeasureTest  {



  "Closeness  for line graph" should "be correctly calculated" in{
    Given("graph")
    val filePath = getClass.getResource("/graphs/5_nodes_directed")
    val graph:Graph[Int,Int]=loadGraph(filePath.toString)
    When("Computes closeness")
    val result=Closeness.compute(graph)
    Then("Should calculate closeness correctly")
    val verticesSortedById=result.vertices.collect().sortBy{case (vId,data)=>vId}
    verticesSortedById .map{case (vId,data)=>data} .zip(Array(
      0.4, 0.375, 1d/3, 0.25, 0d
    )).foreach{case (a,b)=>{a should be (b +- 1e-5 )}}
  }




  "Closeness for full directed graph " should "be correctly calculated" in{
    Given("graph")
    val filePath = getClass.getResource("/graphs/4_nodes_full")
    val graph:Graph[Int,Int]=loadGraph(filePath.toString)
    When("Computes Closeness")
    val result=Closeness.compute(graph)
    Then("Should calculate Closeness correctly")
    val verticesSortedById=result.vertices.collect().sortBy{case (vId,data)=>vId}
    verticesSortedById .map{case (vId,data)=>data} .zip(Array(
      0.75, 0.5, 0.6, 0.75
    )).foreach{case (a,b)=>{a should be (b +- 1e-5 )}}
  }

  "Closeness for full undirected graph " should "be correctly calculated" in{
    Given("graph")
    val filePath = getClass.getResource("/graphs/4_nodes_full")
    val graph:Graph[Int,Int]=loadGraph(filePath.toString)
    When("Computes Closeness")
    val result=Closeness.compute(graph,VertexMeasureConfiguration[Int,Int](true))
    Then("Should calculate Closeness correctly")
    val verticesSortedById=result.vertices.collect().sortBy{case (vId,data)=>vId}
    verticesSortedById .map{case (vId,data)=>data} .zip(Array(
      1d,1d,1d,1d
    )).foreach{case (a,b)=>{a should be (b +- 1e-5 )}}
  }


}