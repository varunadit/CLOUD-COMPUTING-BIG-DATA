import org.apache.spark.graphx.{Graph,Edge,EdgeTriplet,VertexId}
import org.apache.spark.graphx.util.GraphGenerators
import org.apache.spark.rdd.RDD
import org.apache.spark.SparkContext
import org.apache.spark.SparkConf

object ConnectedComponents {
  def main ( args: Array[String] ) {
    val conf = new SparkConf().setAppName("Connected Components")
    val sc = new SparkContext(conf)

    // A graph G is a dataset of vertices, where each vertex is (id,adj),
    // where id is the vertex id and adj is the list of outgoing neighbors
    var G: RDD[ ( Long, List[Long] ) ]
    // read the graph from the file args(0)
       = sc.textFile(args(0))
          .map(
            line => { val a = line.split(",")
            (a(0).toLong, a.slice(1, a.length).toList.map(_.toLong))
          }
        )

    // graph edges have attribute values 0
    val edges: RDD[Edge[Int]] = 
      G.flatMap{ case(id,adj) => adj.map(y => (id, y)) }
				.map{ case (x,y) => Edge(x, y, x.toInt) }

    // a vertex (id,group) has initial group equal to id
    val vertices: RDD[(Long,Long)] = G.map{ case (id,adj) => (id, id)}

    // the GraphX graph
    val graph: Graph[Long,Int] = Graph(vertices,edges,0L)

    // find the vertex new group # from its current group # and the group # from the incoming neighbors
    def newValue ( id: VertexId, currentGroup: Long, incomingGroup: Long ): Long
      = {
          if(currentGroup < incomingGroup)
            currentGroup

          else 
            incomingGroup
        }
    // send the vertex group # to the outgoing neighbors
    def sendMessage ( triplet: EdgeTriplet[Long,Int]): Iterator[(VertexId,Long)]
      = {
			    if(triplet.attr < triplet.dstAttr)
				    Iterator((triplet.dstId, triplet.attr))

          else if(triplet.srcAttr < triplet.attr)
				    Iterator((triplet.dstId, triplet.srcAttr))
			    
          else
				    Iterator.empty;			    
		    }

    def mergeValues ( x: Long, y: Long ): Long
      = {
          if(x < y)
           x

          else 
            y
        }

    // derive connected components using pregel
    val comps = graph.pregel (Long.MaxValue,5) (   // repeat 5 times
                      newValue,
                      sendMessage,
                      mergeValues
                   )

    // print the group sizes (sorted by group #)
    comps.vertices
    .map(graph => (graph._2, 1))
    .reduceByKey(_ + _)
    .sortByKey()
    .map { case ((group,size)) =>group+"\t"+size}
    .collect().foreach(println)

  }
}
