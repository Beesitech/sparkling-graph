package ml.sparkling.graph.operators

import ml.sparkling.graph.api.operators.algorithms.community.CommunityDetection._
import ml.sparkling.graph.api.operators.measures.VertexMeasureConfiguration
import ml.sparkling.graph.operators.algorithms.community.pscan.PSCAN._
import ml.sparkling.graph.operators.partitioning.CommunityBasedPartitioning._
import ml.sparkling.graph.operators.measures.{VertexEmbeddedness, NeighborhoodConnectivity, Degree}
import ml.sparkling.graph.operators.measures.closenes.Closeness
import ml.sparkling.graph.operators.measures.eigenvector.EigenvectorCentrality
import ml.sparkling.graph.operators.measures.hits.Hits
import org.apache.spark.SparkContext
import org.apache.spark.graphx.Graph
import scala.reflect.ClassTag

/**
 * Created by Roman Bartusiak (roman.bartusiak@pwr.edu.pl http://riomus.github.io).
 */
object OperatorsDSL {

  implicit class DSL[VD:ClassTag,ED:ClassTag](graph:Graph[VD,ED]){
    def PSCAN(epsilon:Double=0.1):Graph[ComponentID,ED]={
      computeConnectedComponents(graph,epsilon)
    }

    def closeness(vertexMeasureConfiguration: VertexMeasureConfiguration[VD, ED])(implicit num:Numeric[ED])=Closeness.compute(graph,vertexMeasureConfiguration)

    def eigenvectorCentrality(vertexMeasureConfiguration: VertexMeasureConfiguration[VD, ED])(implicit num:Numeric[ED])=EigenvectorCentrality.compute(graph,vertexMeasureConfiguration)

    def hits(vertexMeasureConfiguration: VertexMeasureConfiguration[VD, ED])(implicit num:Numeric[ED])=Hits.compute(graph,vertexMeasureConfiguration)

    def degree(vertexMeasureConfiguration: VertexMeasureConfiguration[VD, ED])(implicit num:Numeric[ED])=Degree.compute(graph,vertexMeasureConfiguration)

    def neighborhoodConnectivity(vertexMeasureConfiguration: VertexMeasureConfiguration[VD, ED])(implicit num:Numeric[ED])=NeighborhoodConnectivity.compute(graph,vertexMeasureConfiguration)

    def vertexEmbeddedness(vertexMeasureConfiguration: VertexMeasureConfiguration[VD, ED])(implicit num:Numeric[ED])=VertexEmbeddedness.compute(graph,vertexMeasureConfiguration)

    def partitionBy(communityDetectionMethod:CommunityDetectionMethod[VD,ED])(implicit sc:SparkContext)=partitionGraphBy(graph,communityDetectionMethod)

    def partitionBy(communityDetectionMethod:CommunityDetectionAlgorithm)(implicit sc:SparkContext)=partitionGraphBy(graph,communityDetectionMethod)

  }
}
