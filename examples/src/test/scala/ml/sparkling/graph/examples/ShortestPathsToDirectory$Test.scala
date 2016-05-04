package ml.sparkling.graph.examples

import java.nio.file.{Files, Path}

import org.scalatest._

import scala.io.Source


/**
  * Created by riomus on 21.04.16.
  */
class ShortestPathsToDirectory$Test extends FlatSpec with BeforeAndAfterAll with GivenWhenThen with Matchers {
val file: Path =Files.createTempDirectory("tempDir")


  "Standard paths" should "be computed correctly" in {
    Given("Using given graph")
    val graph=getClass.getResource("/examples_graphs/5_nodes_directed")
    When("Computes paths")
    ShortestPathsToDirectory.main(Array("--withIndexing","false","--master","local[*]","--load-partitions","1","--bucket-size", "1000", "--treat-as-undirected", "false",graph.toString, file.toString))
    Then("Should correctly compute")
    val result: List[String] = Source.fromFile(s"${file.toString}/from_1/part-00000").getLines().toList.sortBy(_.split(",")(0))
    result should equal(
      List( "1;0;1;2;3;4",
            "2;0;0;1;2;3",
            "3;0;0;0;1;2",
            "4;0;0;0;0;1",
            "5;0;0;0;0;0"))
  }

}
