import sbt.Keys.libraryDependencies

organization := "com.candao"
name := "data_flow"
version := "0.1"
scalaVersion := "2.11.12"

lazy val commonSettings = Seq(
  organization := "com.candao",
  name := "data_flow",
  version := "0.1",
  scalaVersion := "2.11.12",
  target := {
    baseDirectory.value / "target"
  }
)

lazy val core = (project in file("core"))
  .settings(
    commonSettings,
    commonLib
    // other settings
  )
////
lazy val data_flow_jyj = project.dependsOn(core)
lazy val data_flow_lxj = project.dependsOn(core)



lazy val commonLib = Seq(
  // spark
  libraryDependencies += "org.apache.spark" %% "spark-sql" % "2.3.0",
  libraryDependencies += "org.apache.spark" %% "spark-core" % "2.3.0",
  libraryDependencies += "org.apache.spark" %% "spark-hive-thriftserver" % "2.3.0",
  libraryDependencies += "org.apache.spark" % "spark-streaming_2.11" % "2.3.0",
  libraryDependencies += ("org.apache.spark" %% "spark-streaming-kafka-0-10" % "2.3.0").exclude("io.netty", "netty"),
  libraryDependencies += "org.mongodb.spark" %% "mongo-spark-connector" % "2.3.0",
  libraryDependencies += "mysql" % "mysql-connector-java" % "5.1.47",
  //
  libraryDependencies += "org.slf4j" % "slf4j-api" % "1.7.25",
  libraryDependencies += "io.thekraken" % "grok" % "0.1.5",
  //test
  libraryDependencies ++= Seq("junit" % "junit" % "4.8.1" % "test")
)