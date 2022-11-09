import org.apache.spark._
import org.apache.spark.rdd._

import org.apache.spark.SparkContext
import org.apache.spark.SparkConf

object Graph {
   def main ( args: Array[String] ) {
      val conf = new SparkConf().setAppName("Graph")
      val sc = new SparkContext(conf)

      // A graph is a dataset of vertices, where each vertex is a triple
      //   (group,id,adj) where id is the vertex id, group is the group id
      //   (initially equal to id), and adj is the list of outgoing neighbors
      var graph: RDD[ ( Long, Long, List[Long] ) ]
      =  sc.textFile(args(0)).map(line => {
		   val input = line.split(",")                                    
		   val adjArray = new Array[Long](input.length - 1)

         for (index <- 1 to input.length-1) {
            adjArray(index-1) = input(index).toLong
         }

		   (input(0).toLong, input(0).toLong, adjArray.toList)        
      })

      for ( i <- 1 to 5 ) {
         // For each vertex (group,id,adj) generate the candidate (id,group)
         //    and for each x in adj generate the candidate (x,group).
         // Then for each vertex, its new group number is the minimum candidate
         val groups: RDD[ ( Long, Long ) ]
         = graph
            .flatMap {  
               case(group,id,adj) =>
               val groupLen: Int =(adj.length) 
               val candidate = new Array[(Long, Long)](groupLen+1)
               candidate(0) = (id, group)
               val adjVertex: Array[Long] = adj.toArray
               for (index <- 0 to groupLen-1) {
			         candidate(index + 1) = (adjVertex(index), group)
               }
               candidate	
            }
            .reduceByKey(
               (a, b) => { 
                  if (a <= b){
                     a
                  }
                  else {
                     b
                  }
            })
      

         // reconstruct the graph using the new group numbers
         graph 
         = groups              
            .join (
               graph.map { 
                   case(a) => (a._2, a)
               })
            .map { 
               case(a,b) => 
		         val adjacent=b._2
               var connectedVertex =  (b._1,a,adjacent._3) 	
		         connectedVertex	}
      }

      // print the group sizes
      graph 
         .map { case(group,id,adj) => (group, 1) }
         .reduceByKey(_+_)
         .sortByKey(true, 0)
         .map { case ((group,size)) =>group+"\t"+size}
         .collect().foreach( println )

      sc.stop()
   }
}