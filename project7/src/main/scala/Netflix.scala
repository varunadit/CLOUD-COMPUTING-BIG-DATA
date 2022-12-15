import org.apache.spark._
import org.apache.spark.sql._

object Netflix {

  def main ( args: Array[ String ]): Unit = {
    val conf = new SparkConf().setAppName("Netflix")
    val spark = SparkSession.builder().config(conf).getOrCreate()
    import spark.implicits._

    /* ... */
    val sqlContext = spark.sqlContext
    val userRatingDF = spark.sparkContext.textFile(args(0))
      .filter(!_.endsWith(":"))
      .map(_.split(","))
      .map(attributes => (attributes(0).toLong, attributes(1).toDouble))
      .toDF()
    
    userRatingDF.createOrReplaceTempView("userRating")

    val userDF = spark.sql("""SELECT floor(avg(_2)*10)/10 as AVERAGE, _1 as ID from userRating GROUP BY _1""")
    userDF.createOrReplaceTempView("AverCount")
    val averDF = spark.sql("""SELECT AVERAGE, count(ID) as COUNT from AverCount GROUP BY AVERAGE ORDER BY AVERAGE""")

    averDF.show(42);


  }
}
