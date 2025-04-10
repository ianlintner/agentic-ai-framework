import Dependencies._

ThisBuild / organization := "com.agenticai"
ThisBuild / scalaVersion := "3.3.1"

lazy val telemetry = (project in file("."))
  .settings(
    name := "agentic-telemetry",
    libraryDependencies ++= Seq(
      "dev.zio" %% "zio" % "2.0.19",
      "dev.zio" %% "zio-test" % "2.0.19" % Test,
      "dev.zio" %% "zio-test-sbt" % "2.0.19" % Test,
      "io.opentelemetry" % "opentelemetry-api" % "1.34.1",
      "io.opentelemetry" % "opentelemetry-sdk" % "1.34.1",
      "io.opentelemetry" % "opentelemetry-exporter-otlp" % "1.34.1",
      "io.opentelemetry" % "opentelemetry-context" % "1.34.1",
      "io.opentelemetry.semconv" % "opentelemetry-semconv" % "1.23.1-alpha",
      "com.typesafe" % "config" % "1.4.3",
      
      // Exporter dependencies
      "io.opentelemetry" % "opentelemetry-exporter-prometheus" % "1.34.1-alpha",
      "io.opentelemetry" % "opentelemetry-exporter-jaeger" % "1.34.1",
      "io.opentelemetry" % "opentelemetry-exporter-logging" % "1.34.1",
      "io.prometheus" % "simpleclient_hotspot" % "0.16.0",
      "io.prometheus" % "simpleclient_common" % "0.16.0"
    ),
    testFrameworks += new TestFramework("zio.test.sbt.ZTestFramework")
  )