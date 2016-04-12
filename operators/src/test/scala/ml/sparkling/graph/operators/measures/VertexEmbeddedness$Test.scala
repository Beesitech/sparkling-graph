package ml.sparkling.graph.operators.measures

import ml.sparkling.graph.api.operators.measures.VertexMeasureConfiguration
import ml.sparkling.graph.operators.MeasureTest
import org.apache.spark.SparkContext
import org.apache.spark.graphx.Graph

/**
 * Created by Roman Bartusiak (roman.bartusiak@pwr.edu.pl http://riomus.github.io).
 */
class VertexEmbeddedness$Test (implicit sc:SparkContext) extends MeasureTest {



  "Vertex embeddedness for directed line graph" should "be correctly calculated" in {
    Given("graph")
    val filePath = getClass.getResource("/graphs/5_nodes_directed")
    val graph: Graph[Int, Int] = loadGraph(filePath.toString)
    When("Computes Vertex embeddedness ")
    val result = VertexEmbeddedness.compute(graph)
    Then("Should calculate Vertex embeddedness  correctly")
    val verticesSortedById=result.vertices.collect().sortBy{case (vId,data)=>vId}
    verticesSortedById.map{case (vId,data)=>data}  should equal (Array(
      0d,0d,0d,0d,0d
    ))
  }

  "Vertex embeddedness for undirected line graph" should "be correctly calculated" in {
    Given("graph")
    val filePath = getClass.getResource("/graphs/5_nodes_directed")
    val graph: Graph[Int, Int] = loadGraph(filePath.toString)
    When("Computes Vertex embeddedness ")
    val result = VertexEmbeddedness.compute(graph,VertexMeasureConfiguration[Int,Int](true))
    Then("Should calculate Vertex embeddedness  correctly")
    val verticesSortedById=result.vertices.collect().sortBy{case (vId,data)=>vId}
    verticesSortedById .map{case (vId,data)=>data} should equal (Array(
      0d,0d,0d,0d,0d
    ))
  }

  "Vertex embeddedness for full 4 node directed graph" should "be correctly calculated" in{
    Given("graph")
    val filePath = getClass.getResource("/graphs/4_nodes_full")
    val graph:Graph[Int,Int]=loadGraph(filePath.toString)
    When("Computes Vertex embeddedness")
    val result= VertexEmbeddedness.compute(graph)
    Then("Should calculate Vertex embeddedness correctly")
    val verticesSortedById=result.vertices.collect().sortBy{case (vId,data)=>vId}
    verticesSortedById .map{case (vId,data)=>data} should equal (Array(
      0.25,0,0,1d/6
    ))
  }

  "Vertex embeddednessy for full 4 node undirected graph" should "be correctly calculated" in{
    Given("graph")
    val filePath = getClass.getResource("/graphs/4_nodes_full")
    val graph:Graph[Int,Int]=loadGraph(filePath.toString)
    When("Computes Vertex embeddedness")
    val result= VertexEmbeddedness.compute(graph,VertexMeasureConfiguration[Int,Int](true))
    Then("Should calculate Vertex embeddedness correctly")
    val verticesSortedById=result.vertices.collect().sortBy{case (vId,data)=>vId}
    verticesSortedById .map{case (vId,data)=>data}  should equal (Array(
     0.5,0.5,0.5,0.5
    ))
  }

  "Vertex embeddedness for full 4 node directed graph" should "be correctly calculated using iterative approach" in{
    Given("graph")
    val filePath = getClass.getResource("/graphs/4_nodes_full")
    val graph:Graph[Int,Int]=loadGraph(filePath.toString)
    When("Computes Vertex embeddedness")
    val result= VertexEmbeddedness.compute(graph)
    val resultIterative= VertexEmbeddedness.compute(graph,VertexMeasureConfiguration[Int,Int]((g:Graph[Int,Int])=>1l))
    Then("Should calculate Vertex embeddedness correctly")
    val verticesSortedById=result.vertices.collect().sortBy{case (vId,data)=>vId}
    verticesSortedById .map{case (vId,data)=>data}  should equal (result.vertices.collect().sortBy{case (vId,data)=>vId}.map{case (vId,data)=>data} )
  }
}
