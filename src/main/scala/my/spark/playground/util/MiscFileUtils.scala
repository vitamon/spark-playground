package my.spark.playground.util

import org.apache.spark.sql.{ DataFrame, SaveMode, SparkSession }

object MiscFileUtils {

  def readCsv(fname: String, spark: SparkSession): DataFrame =
    spark.read
      .option("header", "true") // Use first line of all files as header
      .option("inferSchema", "true") // Automatically infer data types
      .option("quote", "\"") //escape the quotes
      .option("ignoreLeadingWhiteSpace", value = true) // escape space before your data
      .csv(fname)

  def writeToCsv(folder: String, query: String)(implicit spark: SparkSession): Unit =
    spark
      .sql(query)
      .coalesce(1)
      .write
      .mode(SaveMode.Overwrite)
      .option("mapreduce.fileoutputcommitter.marksuccessfuljobs", "false") //Avoid creating of crc files
      .option("header", "true")
      .csv(folder)

}
