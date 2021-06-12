package my.spark.playground

import java.io.File
import org.apache.commons.io.FileUtils
import org.apache.spark.sql.{DataFrame, SaveMode, SparkSession}

object Main2 extends App with SparkSqlAndCsvIo {

  val resultDir = "./data/results"

  FileUtils.deleteDirectory(new File(resultDir))

  implicit val spark: SparkSession = prepareSessionAndData(
    "./data/airports.csv",
    "./data/countries.csv",
    "./data/runways.csv"
  )

  doReports(resultDir)
}

trait SparkSqlAndCsvIo {

  def prepareSessionAndData(
    airportsFile: String,
    countriesFile: String,
    runWaysFile: String): SparkSession = {

    val spark: SparkSession = SparkSession.builder()
      .appName("Test App")
      .master("local[*]")
      .getOrCreate()

    val airportsDF = readCsv(airportsFile, spark)
    val countriesDF = readCsv(countriesFile, spark)
    val runWaysDF = readCsv(runWaysFile, spark)

    airportsDF.createOrReplaceTempView("airports")
    countriesDF.createOrReplaceTempView("countries")
    runWaysDF.createOrReplaceTempView("runways")

    spark
  }

  def readCsv(fname: String, spark: SparkSession): DataFrame = {
    spark.read
      .format("com.databricks.spark.csv")
      .option("header", "true") // Use first line of all files as header
      .option("inferSchema", "true") // Automatically infer data types
      .option("quote", "\"") //escape the quotes
      .option("ignoreLeadingWhiteSpace", value = true) // escape space before your data
      .load(fname)
  }

  def writeToCsv(folder: String, query: String)(implicit spark: SparkSession): Unit = {
    spark.sql(query).coalesce(1)
      .write
      .mode(SaveMode.Overwrite)
      .option("mapreduce.fileoutputcommitter.marksuccessfuljobs", "false") //Avoid creating of crc files
      .option("header", "true")
      .csv(folder)
  }

  def doReports(reportOutputDir: String)(implicit spark: SparkSession): Unit = {
    // 10 countries with highest number of airports (with count) and countries with lowest number of airports
    writeToCsv(reportOutputDir + "/report1",
      """
      (SELECT countries.name, COUNT(DISTINCT airports.name) as cnt
      FROM countries, airports
      WHERE
         airports.iso_country = countries.code
      GROUP BY
         countries.name
      ORDER BY cnt DESC
      LIMIT 10)
      UNION
      (SELECT countries.name, COUNT(DISTINCT airports.name) as cnt
      FROM countries, airports
      WHERE
         airports.iso_country = countries.code
      GROUP BY
         countries.name
      ORDER BY cnt ASC
      LIMIT 10)
      ORDER BY cnt DESC
     """
    )

    // Type of runways (as indicated in "surface" column) per country
    writeToCsv(reportOutputDir + "/report2",
      """
      SELECT countries.name, runways.surface
      FROM countries, runways, airports
      WHERE
         airports.iso_country = countries.code
         AND airports.id = runways.airport_ref
      GROUP BY
         countries.name,
         runways.surface
      ORDER BY countries.name
     """
    )

    // Top 10 most common runway identifications (indicated in "le_ident" column)
    writeToCsv(reportOutputDir + "/report3",
      """
      SELECT le_ident, COUNT(le_ident) as cnt
      FROM runways
      GROUP BY
         le_ident
      ORDER BY cnt DESC
      LIMIT 10
    """
    )
  }


}
