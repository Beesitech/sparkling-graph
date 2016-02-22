package ml.sparkling.graph.loaders.csv.providers

import ml.sparkling.graph.loaders.csv.types.Types
import org.apache.spark.graphx._
import org.apache.spark.rdd.RDD
import org.apache.spark.sql.{DataFrame, Row}
import org.apache.spark.storage.StorageLevel
import Types.ToVertexId

import scala.reflect.ClassTag

/**
 * Created by Roman Bartusiak (roman.bartusiak@pwr.edu.pl http://riomus.github.io).
 */
object GraphProviders {
  val DefaultStorageLevel=StorageLevel.MEMORY_ONLY
  def simpleGraphBuilder[VD: ClassTag, ED: ClassTag](defaultVertex: VD,
                                                     vertexProvider: Row => Seq[(VertexId, VD)],
                                                     edgeProvider: Row => Seq[Edge[ED]],
                                                     edgeStorageLevel: StorageLevel = DefaultStorageLevel,
                                                     vertexStorageLevel: StorageLevel =DefaultStorageLevel)
                                                    (dataFrame: DataFrame): Graph[VD, ED] = {
    def mapRows[MT: ClassTag](mappingFunction: (Row) => Seq[MT]): RDD[MT] = {
      dataFrame.rdd.mapPartitionsWithIndex((id, rowIterator) => {
        rowIterator.flatMap { case row => mappingFunction(row) }
      })
    }

    val vertices: RDD[(VertexId, VD)] = mapRows(vertexProvider)
    val edges: RDD[Edge[ED]] = mapRows(edgeProvider)
    Graph(vertices,edges,defaultVertex,edgeStorageLevel,vertexStorageLevel)
  }

  def indexedGraphBuilder[VD: ClassTag, ED: ClassTag](defaultVertex: VD,
                                                      vertexProvider: (Row, ToVertexId[VD]) => Seq[(VertexId, VD)],
                                                      edgeProvider: (Row, ToVertexId[VD]) => Seq[Edge[ED]],
                                                      columnsToIndex: Seq[Int],
                                                      edgeStorageLevel: StorageLevel = DefaultStorageLevel,
                                                      vertexStorageLevel: StorageLevel = DefaultStorageLevel)
                                                     (dataFrame: DataFrame): Graph[VD, ED] = {

    val index = dataFrame.flatMap(row => columnsToIndex.map(row(_)))
      .map(Set(_))
      .reduce((a, b) => a ++ b).toList.zipWithIndex.toMap
    def extractIdFromIndex(vertex: VD) = index(vertex)
    simpleGraphBuilder(defaultVertex,
      vertexProvider(_: Row, extractIdFromIndex _),
      edgeProvider(_: Row, extractIdFromIndex _),
      edgeStorageLevel,
      vertexStorageLevel)(dataFrame)

  }
}