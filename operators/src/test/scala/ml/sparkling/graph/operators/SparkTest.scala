package ml.sparkling.graph.operators

import ml.sparkling.graph.operators.algorithms.coarsening.labelpropagation.LPCoarsening$Test
import ml.sparkling.graph.operators.algorithms.community.pscan.PSCAN$Test
import ml.sparkling.graph.operators.algorithms.link.BasicLinkPredictor$Test
import ml.sparkling.graph.operators.algorithms.shortestpaths.ShortestPathsAlgorithm$Test
import ml.sparkling.graph.operators.measures.edge.AdamicAdar$Test
import ml.sparkling.graph.operators.measures.graph.{FreemanCentrality$Test, Modularity$Test}
import ml.sparkling.graph.operators.measures.vertex.closenes.Closeness$Test
import ml.sparkling.graph.operators.measures.vertex.clustering.LocalClustering$Test
import ml.sparkling.graph.operators.measures.vertex.eigenvector.EigenvectorCentrality$Test
import ml.sparkling.graph.operators.measures.vertex.hits.Hits$Test
import ml.sparkling.graph.operators.measures.{NeighborhoodConnectivity$Test, VertexEmbeddedness$Test}
import ml.sparkling.graph.operators.partitioning.CommunityBasedPartitioning$Test
import org.apache.spark.{SparkConf, SparkContext}
import org.scalatest._

/**
 * Created by Roman Bartusiak (roman.bartusiak@pwr.edu.pl http://riomus.github.io).
 */
class SparkTest extends Spec with BeforeAndAfterAll {

  val master = "local[*]"

  def appName: String = "operators-tests"

  implicit val sc: SparkContext = {
    val conf = new SparkConf()
      .setMaster(master)
      .setAppName(appName)
    new SparkContext(conf)
  }

  override def afterAll() = {
    sc.stop()
  }

  override def nestedSuites = {
    Vector(
      new VertexEmbeddedness$Test,
      new NeighborhoodConnectivity$Test,
      new Hits$Test,
      new EigenvectorCentrality$Test,
      new LocalClustering$Test,
      new Closeness$Test,
      new ShortestPathsAlgorithm$Test,
      new PSCAN$Test,
      new CommunityBasedPartitioning$Test,
      new FreemanCentrality$Test,
      new Modularity$Test,
      new AdamicAdar$Test,
      new BasicLinkPredictor$Test,
      new LPCoarsening$Test
    )
  }


}