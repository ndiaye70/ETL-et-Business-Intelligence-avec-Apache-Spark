ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "2.12.18"

lazy val root = (project in file("."))
  .settings(
    name := "etl-adventureworks"

  )

libraryDependencies ++= Seq(
  "org.apache.spark" %% "spark-core" % "3.5.0",
  "org.apache.spark" %% "spark-sql" % "3.5.0",
  "com.microsoft.sqlserver" % "mssql-jdbc" % "12.10.0.jre11",
  "org.postgresql" % "postgresql" % "42.7.6",


)


javaOptions ++= Seq(
  "--add-opens=java.base/java.util=ALL-UNNAMED",
  "--add-opens=java.base/java.lang.invoke=ALL-UNNAMED",
  "--add-opens=java.base/sun.nio.ch=ALL-UNNAMED", // Important pour Spark
  "--add-opens=java.base/java.lang=ALL-UNNAMED"
)

ThisBuild / fork := true
