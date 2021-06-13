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

object MainSparkSqlZio extends zio.App {

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
      airportsF <- effectBlocking(readCsv(airportsFile, spark)).map(_.createOrReplaceTempView("airports")).fork
      countriesF <- effectBlocking(readCsv(countriesFile, spark)).map(_.createOrReplaceTempView("countries")).fork
      runWaysF <- effectBlocking(readCsv(runWaysFile, spark)).map(_.createOrReplaceTempView("runways")).fork

      _ <- ZIO.collectAll(Seq(airportsF.join, countriesF.join, runWaysF.join))

      f1 <- effectBlocking(writeToCsv(reportOutputDir + "/report1", Queries.countriesToAirports)).fork
      f2 <- effectBlocking(writeToCsv(reportOutputDir + "/report2", Queries.topAirports)).fork
      f3 <- effectBlocking(writeToCsv(reportOutputDir + "/report3", Queries.topRunways)).fork
      _ <- Task.collectAll(Seq(f1.join, f2.join, f3.join))
    } yield ()

}
