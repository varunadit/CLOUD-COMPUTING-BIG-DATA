import org.apache.spark._
import org.apache.spark.rdd._

import org.apache.spark.SparkContext
import org.apache.spark.SparkConf

object Add {
  val rows = 100
  val columns = 100

  case class Block ( data: Array[Double] ) {
    override
    def toString (): String = {
      var s = "\n"
      for ( i <- 0 until rows ) {
        for ( j <- 0 until columns )
          s += "\t%.3f".format(data(i*rows+j))
        s += "\n"
      }
      s
    }
  }

   def toBlock ( triples: List[(Int,Int,Double)] ): Block = {
    var b = new Array[Double](100*100)
    for((i,j,v) <- triples) {
      b(i*rows+j) = v
    }
    var block = new Block(b)
    block
  }


  def blockAdd ( m: Block, n: Block ): Block = {
    var b = new Array[Double](100*100)
    for ( i <- 0 until rows ) {
      for ( j <- 0 until columns ) {
        b(i*rows+j)=m.data(i*rows+j)+n.data(i*rows+j)
      }
    }
    var block = new Block(b)
    block
  }



  /* Read a sparse matrix from a file and convert it to a block matrix */
  def createBlockMatrix ( sc: SparkContext, file: String ): RDD[((Int,Int),Block)] = {
    val mat = sc.textFile(file).map( line => { 	val a = line.split(",") 
    (a(0).toInt, a(1).toInt, a(2).toDouble )})
    val matrix = mat.map({ case (i, j, v) => ((i/rows,j/columns),(i%rows, j%columns, v))}).groupBy(_._1).mapValues(_.map(_._2))
    val blockMatrix = matrix.map({ case (a,b)=>(a, toBlock(b.toList))})
    blockMatrix
  }

  def main ( args: Array[String] ) {

    val conf = new SparkConf().setAppName("Multiply")
    
    val sc = new SparkContext(conf)

    val mMatrix = createBlockMatrix(sc, args(0))
    val nMatrix = createBlockMatrix(sc, args(1))

    //val sumMatrix = mMatrix.join(nMatrix).map { case (k,(a,p)) => "("+k+","+blockAdd(a,p)+")" }

    val sumMatrix = mMatrix.union(nMatrix).reduceByKey(blockAdd(_,_))

    //val sumMatrix = mMatrix.join(nMatrix).reduceByKey{(case(sq) => blockAdd(sq._1,sq._2))}
    //val sumMatrix = mMatrix.union(nMatrix).reduceByKey { (a,b) => blockAdd(a,b) }
    //val result = sumMatrix.reduceByKey((x,y)  => blockAdd(x._1,y._2))

    //val result = 

    
    sumMatrix.saveAsTextFile(args(2))
  
    sc.stop()

  }
}
