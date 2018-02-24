package me.soulmachine.spark

import java.io.File
import org.apache.commons.io.FileUtils
import org.apache.spark._
import spray.json.DefaultJsonProtocol._
import spray.json._
import spray.json.lenses.JsonLenses._

object Main extends App {

  val inputFile = new File("/Users/data/input/", "data.csv").getAbsolutePath

  val outputDir = new File("target/output/")

  FileUtils.deleteDirectory(outputDir)

  new SparkExample().execute(
    input = inputFile,
    output = outputDir.getAbsolutePath
  )
}

object SparkExample {

  def parseConfigs(json: JsObject): Seq[Set[String]] = {
    val wires = json.extract[String]("objects" / * / "value" / "hvac_pins".?)
    wires.map(_.toUpperCase.split(',').filter(_.nonEmpty).toSet)
  }

  def extractEmail(json: JsObject): String = {
    json.extract[String]("objects" / * / "value" / "email".?).headOption.getOrElse("")
  }

  def extractPostal(json: JsObject): String = {
    json.extract[String]("objects" / * / "value" / "postal_code".?).headOption.getOrElse("")
  }
}

class SparkExample {

  import SparkExample._

  def execute(input: String, output: String, master: Option[String] = Some("local[2]")): Unit = {

    val sc = {
      val conf = new SparkConf().setAppName("Spark Example")
      master.foreach(conf.setMaster)
      new SparkContext(conf)
    }

    val results = sc.textFile(input).
      filter(_.contains("hvac_pins")).
      flatMap { str =>
        val Array(ind, json) = str.split(",", 2)
        val js = json.parseJson.asJsObject

        val email = extractEmail(js)
        val post = extractPostal(js)

        parseConfigs(js).map { hv =>
          (ind, hv, email, post)
        }

      }.
      filter(_._2.nonEmpty).
      map {
        case (ind, hvacs, email, postal) =>
          Seq(ind, hvacs.mkString("-"), email, postal).mkString(",")
      }

    results.coalesce(1).saveAsTextFile(output)
    sc.stop()
  }


}
