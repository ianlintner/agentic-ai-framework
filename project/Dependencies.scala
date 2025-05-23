import sbt._

object Dependencies {
  // Versions
  val zioVersion = "2.1.14"
  val zioConfigVersion = "4.0.3" // REQUIRES 4.x.x or it won't work with scala 3.x.x
  val zioLoggingVersion = "2.2.3"
  val zioMetricsVersion = "2.3.1"
  val zioHttpVersion = "3.0.0-RC4"
  val zioJsonVersion = "0.6.2"
  val zioPreludeVersion = "1.0.0-RC39"
  val zioSchemaVersion = "0.4.17"
  val zioStreamVersion = "2.1.14"
  val zioTestVersion = "2.1.14"
  val vertexAiVersion = "3.35.0"
  val googleCloudVersion = "2.30.0"
  val googleAuthVersion = "1.20.0"
  val scalaTestVersion = "3.2.17"
  val logbackVersion = "1.5.17"
  val logstashLogbackEncoder = "8.0"
  val quillVersion = "4.8.6"
  val postgresqlVersion = "42.7.5"
  val flywayVersion = "9.16.0"
  val testContainersVersion = "1.19.7"
  val openTelemetryVersion = "1.47.0"
  val openTelemetrySemconvVersion = "1.25.0-alpha"
  val zioTelemetryVersion = "3.1.2"
  val tapirVersion = "1.11.10"

  // Libraries
  val zio = "dev.zio" %% "zio" % zioVersion
  val zioTest = "dev.zio" %% "zio-test" % zioTestVersion
  val zioTestSbt = "dev.zio" %% "zio-test-sbt" % zioTestVersion
  val zioStreams = "dev.zio" %% "zio-streams" % zioStreamVersion
  val zioLogging = "dev.zio" %% "zio-logging" % zioLoggingVersion
  val zioMetrics = "dev.zio" %% "zio-metrics-connectors" % zioMetricsVersion
  val zioHttp = "dev.zio" %% "zio-http" % zioHttpVersion
  val zioJson = "dev.zio" %% "zio-json" % zioJsonVersion
  val zioPrelude = "dev.zio" %% "zio-prelude" % zioPreludeVersion
  val zioSchema = "dev.zio" %% "zio-schema" % zioSchemaVersion
  val zioConfig = "dev.zio" %% "zio-config" % zioConfigVersion
  val zioConfigTypesafe = "dev.zio" %% "zio-config-typesafe" % zioConfigVersion
  val zioConfigMagnolia = "dev.zio" %% "zio-config-magnolia" % zioConfigVersion
  val zioConfigYaml = "dev.zio" %% "zio-config-yaml" % zioConfigVersion
  val zioTelemetry = "dev.zio" %% "zio-opentelemetry" % zioTelemetryVersion

  // Google Cloud dependencies
  val vertexAi = "com.google.cloud" % "google-cloud-aiplatform" % vertexAiVersion
  val googleCloud = "com.google.cloud" % "google-cloud-core" % googleCloudVersion
  val googleAuth = "com.google.auth" % "google-auth-library-oauth2-http" % googleAuthVersion
  val googleProtoAiPlatform = "com.google.api.grpc" % "proto-google-cloud-aiplatform-v1" % vertexAiVersion
  val googleGax = "com.google.api" % "gax" % "2.23.0"
  val scalaTest = "org.scalatest" %% "scalatest" % scalaTestVersion
  val logback = "ch.qos.logback" % "logback-classic" % logbackVersion
  val logstashEncoder = "net.logstash.logback" % "logstash-logback-encoder" % logstashLogbackEncoder
  val quill = "io.getquill" %% "quill-jdbc-zio" % quillVersion
  val postgresql = "org.postgresql" % "postgresql" % postgresqlVersion
  val flyway = "org.flywaydb" % "flyway-core" % flywayVersion
  val testContainers = "org.testcontainers" % "testcontainers" % testContainersVersion
  val openTelemetry = "io.opentelemetry" % "opentelemetry-api" % openTelemetryVersion
  val openTelemetrySemconv = "io.opentelemetry.semconv" % "opentelemetry-semconv" % openTelemetrySemconvVersion
  val tapir = "com.softwaremill.sttp.tapir" %% "tapir-core" % tapirVersion

  // Core dependencies - minimal set needed for basic functionality
  val coreDependencies = Seq(
    zio,
    zioStreams,
    zioLogging,
    logback
  )
  
  // LLM integration dependencies
  val llmDependencies = Seq(
    vertexAi,
    googleCloud,
    googleAuth,
    googleProtoAiPlatform,
    googleGax
  )
  
  // Testing dependencies
  val testDependencies = Seq(
    zioTest % Test,
    zioTestSbt % Test,
    scalaTest % Test,
    testContainers % Test
  )
  
  // Web dependencies
  val webDependencies = Seq(
    zioHttp,
    zioJson,
    tapir
  )
  
  // Database dependencies
  val dbDependencies = Seq(
    quill,
    postgresql,
    flyway
  )
  
  // Monitoring dependencies
  val monitoringDependencies = Seq(
    zioMetrics,
    zioTelemetry,
    openTelemetry,
    openTelemetrySemconv,
    logstashEncoder
  )
  
  // Configuration dependencies
  val configDependencies = Seq(
    zioConfig,
    zioConfigTypesafe,
    zioConfigMagnolia,
    zioConfigYaml
  )
  
  // All common dependencies - for backward compatibility
  val commonDependencies = coreDependencies ++
                          testDependencies ++
                          llmDependencies ++
                          webDependencies ++
                          dbDependencies ++
                          monitoringDependencies ++
                          configDependencies ++ Seq(
    zioPrelude,
    zioSchema
  )
}