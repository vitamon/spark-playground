name := "spark-example-project"

version := "1.0.0-SNAPSHOT"

// All Spark Packages need a license
licenses := Seq("Apache-2.0" -> url("http://opensource.org/licenses/Apache-2.0"))

scalaVersion := "2.11.12"
// force scalaVersion
//ivyScala := ivyScala.value map { _.copy(overrideScalaVersion = true) }

val sparkVersion = "2.2.1"

// Needed as SBT's classloader doesn't work well with Spark
fork := true

// BUG: unfortunately, it's not supported right now
fork in console := true

// Java version
javacOptions ++= Seq("-source", "1.8", "-target", "1.8")

// add a JVM option to use when forking a JVM for 'run'
javaOptions ++= Seq("-Xmx4G")

// append -deprecation to the options passed to the Scala compiler
scalacOptions ++= Seq("-deprecation", "-unchecked")

// Use local repositories by default
resolvers ++= Seq(
  Resolver.defaultLocal,
  Resolver.mavenLocal,
  // make sure default maven local repository is added... Resolver.mavenLocal has bugs.
  "Local Maven Repository" at "file://" + Path.userHome.absolutePath + "/.m2/repository",
  // For Typesafe goodies, if not available through maven
  // "Typesafe" at "http://repo.typesafe.com/typesafe/releases",
  // For Spark development versions, if you don't want to build spark yourself
  "Apache Staging" at "https://repository.apache.org/content/repositories/staging/"
)

// copy all dependencies into lib_managed/
//retrieveManaged := true

val sparkDependencyScope = "compile"

libraryDependencies ++= Seq(
  "org.apache.spark" %% "spark-core" % sparkVersion,
  "org.apache.spark" %% "spark-sql" % sparkVersion,
  "org.apache.spark" %% "spark-mllib" % sparkVersion,
  "org.apache.spark" %% "spark-streaming" % sparkVersion,
  "org.apache.spark" %% "spark-hive" % sparkVersion,

  "io.spray" %% "spray-json" % "1.3.4",
  "net.virtual-void" %% "json-lenses" % "0.6.2",
  "com.typesafe.scala-logging" %% "scala-logging" % "3.8.0",
  "com.google.guava" % "guava" % "23.0" force(),
  "org.apache.hadoop" % "hadoop-mapreduce-client-core" % "2.9.0" force(),
  "org.apache.hadoop" % "hadoop-common" % "2.9.0"
)

/// console

// define the statements initially evaluated when entering 'console', 'consoleQuick', or 'consoleProject'
// but still keep the console settings in the sbt-spark-package plugin

// If you want to use yarn-client for spark cluster mode, override the environment variable
// SPARK_MODE=yarn-client <cmd>
val sparkMode = sys.env.getOrElse("SPARK_MODE", "local[4]")

initialCommands in console :=
  s"""
     |import org.apache.spark.SparkConf
     |import org.apache.spark.SparkContext
     |import org.apache.spark.SparkContext._
     |
    |@transient val sc = new SparkContext(
     |  new SparkConf()
     |    .setMaster("$sparkMode")
     |    .setAppName("Console test"))
     |implicit def sparkContext = sc
     |import sc._
     |
    |@transient val sqlc = new org.apache.spark.sql.SQLContext(sc)
     |implicit def sqlContext = sqlc
     |import sqlc._
     |
    |def time[T](f: => T): T = {
     |  import System.{currentTimeMillis => now}
     |  val start = now
     |  try { f } finally { println("Elapsed: " + (now - start)/1000.0 + " s") }
     |}
     |
    |""".stripMargin

cleanupCommands in console :=
  s"""
     |sc.stop()
   """.stripMargin


/// scaladoc
scalacOptions in(Compile, doc) ++= Seq("-groups", "-implicits",
  // NOTE: remember to change the JVM path that works on your system.
  // Current setting should work for JDK7 on OSX and Linux (Ubuntu)
  "-doc-external-doc:/Library/Java/JavaVirtualMachines/jdk1.7.0_60.jdk/Contents/Home/jre/lib/rt.jar#http://docs.oracle.com/javase/7/docs/api",
  "-doc-external-doc:/usr/lib/jvm/java-7-openjdk-amd64/jre/lib/rt.jar#http://docs.oracle.com/javase/7/docs/api"
)

autoAPIMappings := true

