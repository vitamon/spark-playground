package my.spark.playground

import my.spark.playground.model.Queries
import my.spark.playground.util.MiscFileUtils._
import org.apache.commons.io.FileUtils
import org.apache.spark.sql.SparkSession

import java.io.File

object Main2 extends App with SparkSqlAndCsvIo {
  doReports()
}

trait SparkSqlAndCsvIo {

  def prepareSession(): SparkSession =
    SparkSession
      .builder()
      .appName("Test App")
      .master("local[*]")
      .getOrCreate()

  def doReports(): Unit = {

    val startTime = System.currentTimeMillis()

    val reportOutputDir = "./data/results"

    FileUtils.deleteDirectory(new File(reportOutputDir))

    implicit val spark: SparkSession = prepareSession()

    val airportsDF  = readCsv("./data/airports.csv", spark)
    val countriesDF = readCsv("./data/countries.csv", spark)
    val runWaysDF   = readCsv("./data/runways.csv", spark)

    airportsDF.createOrReplaceTempView("airports")
    countriesDF.createOrReplaceTempView("countries")
    runWaysDF.createOrReplaceTempView("runways")

    writeToCsv(reportOutputDir + "/report1", Queries.countriesToAirports)
    writeToCsv(reportOutputDir + "/report2", Queries.topAirports)
    writeToCsv(reportOutputDir + "/report3", Queries.topRunways)

    spark.close()
    println(s"Process finished in ${System.currentTimeMillis() - startTime} millis")

  }

}
