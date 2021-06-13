package my.spark.playground.zio

import my.spark.playground.model.Queries
import my.spark.playground.util.MiscFileUtils.{ writeToCsv, _ }
import org.apache.commons.io.FileUtils
import org.apache.spark.sql.SparkSession
import zio.{ ExitCode, Task, UIO, URIO, ZIO }
import zio.blocking.effectBlocking
import zio.clock.currentTime
import zio.console._

import java.io.File
import java.util.concurrent.TimeUnit

object MainDatasetsZio extends zio.App {

  def run(args: List[String]): URIO[zio.ZEnv, ExitCode] = myAppLogic.exitCode

  def buildSparkSession(): SparkSession =
    SparkSession
      .builder()
      .appName("Test App")
      .master("local[*]")
      .getOrCreate()

  val resultDir = "./data/results"

  val airportsFile  = "./data/airports.csv"
  val countriesFile = "./data/countries.csv"
  val runWaysFile   = "./data/runways.csv"

  val myAppLogic: ZIO[zio.ZEnv, Throwable, Unit] = for {
    startTime <- currentTime(TimeUnit.MILLISECONDS)

    _ <- Task(FileUtils.deleteDirectory(new File(resultDir)))

    _ <- ZIO.bracket(
          acquire = Task(buildSparkSession()),
          release = { s: SparkSession =>
            UIO(s.close())
          },
          use = { s: SparkSession =>
            processData(resultDir)(s)
          }
        )

    endTime <- currentTime(TimeUnit.MILLISECONDS)
    _ <- putStrLn(s"Process Finished in ${endTime - startTime} millis")
  } yield ()

  def processData(reportOutputDir: String)(implicit spark: SparkSession): ZIO[zio.ZEnv, Throwable, Unit] =
    for {
      airportsF <- effectBlocking(readCsv(airportsFile, spark)).fork
      countriesF <- effectBlocking(readCsv(countriesFile, spark)).fork
      runWaysF <- effectBlocking(readCsv(runWaysFile, spark)).fork

      airports <- airportsF.join
      countries <- countriesF.join
      runWays <- runWaysF.join

      result1 <- Task(Queries.queryCountriesToAirports(countries, airports))
      result2 <- Task(Queries.queryAirports(countries, runWays, airports))
      result3 <- Task(Queries.queryRunways(runWays))

      _ <- effectBlocking(writeToCsv(reportOutputDir + "/report1", result1))
      _ <- effectBlocking(writeToCsv(reportOutputDir + "/report2", result2))
      _ <- effectBlocking(writeToCsv(reportOutputDir + "/report3", result3))
    } yield ()

}
