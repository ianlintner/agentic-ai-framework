import Dependencies._
import Langchain4jDependencies._

ThisBuild / scalaVersion := "3.3.1"
ThisBuild / version := "0.1.0-SNAPSHOT"
ThisBuild / organization := "com.agenticai"

// Repository configuration - explicitly add Maven Central and other repos
ThisBuild / resolvers ++= Seq(
  Resolver.mavenLocal,
  Resolver.mavenCentral,
  "Sonatype Releases" at "https://oss.sonatype.org/content/repositories/releases",
  "Typesafe repository" at "https://repo.typesafe.com/typesafe/releases/",
  "Google Maven Central" at "https://maven.google.com",
  "Google Cloud Maven" at "https://storage.googleapis.com/cloud-opensource-java-dashboard/com.google.cloud",
  "Google Cloud Platform Libraries" at "https://dl.google.com/dl/android/maven2/",
  "ZIO Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots",
  "ZIO Releases" at "https://s01.oss.sonatype.org/content/releases",
  "Maven Central" at "https://repo1.maven.org/maven2/"
)

// Common settings for all projects
lazy val commonSettings = Seq(
  scalacOptions ++= Seq(
    "-Xfatal-warnings",
    "-deprecation",
    "-feature",
    "-unchecked",
    "-language:postfixOps",
    "-language:implicitConversions",
    "-Ykind-projector"
  ),
  testFrameworks += new TestFramework("zio.test.sbt.ZTestFramework"),
  libraryDependencies ++= commonDependencies,
  // Ensure test dependencies are applied to all modules
  Test / libraryDependencies ++= Seq(
    "dev.zio" %% "zio-test" % zioTestVersion,
    "dev.zio" %% "zio-test-sbt" % zioTestVersion,
    "dev.zio" %% "zio" % zioVersion
  )
)

// Custom task for integration tests
lazy val IntegrationTest = config("it") extend Test

// Root project that aggregates all subprojects
lazy val root = (project in file("."))
  .settings(
    name := "agentic-ai-framework",
    // Don't publish the root project
    publish / skip := true,
    // Add common dependencies to all modules
    libraryDependencies ++= commonDependencies,
    // Add test dependencies to all modules
    Test / libraryDependencies ++= Seq(
      "dev.zio" %% "zio-test" % zioTestVersion,
      "dev.zio" %% "zio-test-sbt" % zioTestVersion,
      "dev.zio" %% "zio" % zioVersion
    ),
    // Configure test framework
    testFrameworks += new TestFramework("zio.test.sbt.ZTestFramework")
  )
  .aggregate(
    core,
    memory,
    agents,
    http,
    dashboard,
    examples,
    langchain4j
  )

// Core module - essential base definitions and interfaces
lazy val core = (project in file("modules/core"))
  .settings(
    commonSettings,
    name := "agentic-ai-core",
    description := "Core interfaces and abstractions for the Agentic AI Framework",
    libraryDependencies ++= commonDependencies ++ Seq(
      // Add Google Cloud dependencies
      "com.google.cloud" % "google-cloud-aiplatform" % vertexAiVersion,
      "com.google.auth" % "google-auth-library-oauth2-http" % googleAuthVersion,
      "com.google.cloud" % "google-cloud-core" % googleCloudVersion,
      "com.google.api.grpc" % "proto-google-cloud-aiplatform-v1" % vertexAiVersion,
      "com.google.api" % "gax" % "2.37.0",
      // Add ZIO Config dependencies
      "dev.zio" %% "zio-config" % zioConfigVersion,
      "dev.zio" %% "zio-config-magnolia" % zioConfigVersion,
      "dev.zio" %% "zio-config-typesafe" % zioConfigVersion
    ),
    // Configure tests
    Test / testOptions += Tests.Argument("-oD"),  // Show test duration
    Test / libraryDependencies ++= Seq(
      "dev.zio" %% "zio-test" % zioTestVersion,
      "dev.zio" %% "zio-test-sbt" % zioTestVersion,
      "dev.zio" %% "zio" % zioVersion
    ),
    // Add source directories for tests
    Test / unmanagedSourceDirectories += baseDirectory.value / "src" / "test" / "scala",
    Test / unmanagedResourceDirectories += baseDirectory.value / "src" / "test" / "resources"
  )

// Memory module - memory system implementation
lazy val memory = (project in file("modules/memory"))
  .settings(
    commonSettings,
    name := "agentic-ai-memory",
    description := "Memory system for the Agentic AI Framework",
    libraryDependencies ++= commonDependencies
  )
  .dependsOn(core)

// Agents module - agent implementations
lazy val agents = (project in file("modules/agents"))
  .settings(
    commonSettings,
    name := "agentic-ai-agents",
    description := "Agent implementations for the Agentic AI Framework",
    libraryDependencies ++= commonDependencies
  )
  .dependsOn(core, memory)

// HTTP module - web API implementation
lazy val http = (project in file("modules/http"))
  .settings(
    commonSettings,
    name := "agentic-ai-http",
    description := "HTTP API for the Agentic AI Framework",
    libraryDependencies ++= commonDependencies
  )
  .dependsOn(core, memory, agents)

// Dashboard module - web UI and visualizations
lazy val dashboard = (project in file("modules/dashboard"))
  .settings(
    commonSettings,
    name := "agentic-ai-dashboard",
    description := "Web dashboard and visualizations for the Agentic AI Framework",
    libraryDependencies ++= commonDependencies
  )
  .dependsOn(core, memory, agents, http)

// Langchain4j integration module
lazy val langchain4j = (project in file("modules/langchain4j"))
  .settings(
    commonSettings,
    name := "agentic-ai-langchain4j",
    description := "Langchain4j integration for the Agentic AI Framework",
    libraryDependencies ++= commonDependencies ++ langchain4jDependencies
  )
  .dependsOn(core)

// Examples module - example applications
lazy val examples = (project in file("modules/examples"))
  .settings(
    commonSettings,
    name := "agentic-ai-examples",
    description := "Example applications using the Agentic AI Framework",
    // Don't publish examples
    publish / skip := true,
    // Add test configuration
    Test / testOptions += Tests.Argument("-oD"),  // Show test duration
    Test / testOptions += Tests.Argument("-l", "integration"), // Exclude integration tests by default
    // Additional example-specific settings
    Compile / run / fork := true,
    Compile / run / javaOptions ++= Seq("-Xms512m", "-Xmx2g"),
    // Ensure test dependencies are properly configured
    Test / libraryDependencies ++= Seq(
      "dev.zio" %% "zio-test" % zioTestVersion,
      "dev.zio" %% "zio-test-sbt" % zioTestVersion,
      "dev.zio" %% "zio" % zioVersion
    ),
    // Add source directories for tests
    Test / unmanagedSourceDirectories += baseDirectory.value / "src" / "test" / "scala",
    Test / unmanagedResourceDirectories += baseDirectory.value / "src" / "test" / "resources"
  )
  .dependsOn(core, memory, agents, http, dashboard, langchain4j)

// Demo module - standalone demos with minimal dependencies
lazy val demo = (project in file("modules/demo"))
  .settings(
    commonSettings,
    name := "agentic-ai-demo",
    description := "Standalone demos for the Agentic AI Framework",
    libraryDependencies ++= Seq(
      "org.scala-lang" % "scala-library" % scalaVersion.value,
      // Add ZIO dependencies for Scala 3
      "dev.zio" %% "zio" % zioVersion,
      "dev.zio" %% "zio-streams" % zioStreamVersion
    ),
    Compile / run / fork := true,
    Compile / run / javaOptions ++= Seq("-Xms512m", "-Xmx2g")
  )

// Custom task to run integration tests
lazy val integrationTest = taskKey[Unit]("Runs integration tests")

integrationTest := (examples / Test / testOnly).toTask(" -- -n integration").value

// Custom task to run Vertex AI connectivity test
lazy val testVertexConnection = taskKey[Unit]("Tests connection to Vertex AI")

testVertexConnection := (examples / Compile / runMain).toTask(" com.agenticai.demo.VertexAIClaudeDemoStandalone").value

// Custom task to run Claude 3.7 example
lazy val runClaudeExample = taskKey[Unit]("Runs simple Claude 3.7 example")

runClaudeExample := (examples / Compile / runMain).toTask(" com.agenticai.examples.SimpleClaudeExample").value

// Custom tasks for Langchain4j examples
lazy val runLangchainClaudeExample = taskKey[Unit]("Runs Langchain4j Claude example")
lazy val runLangchainVertexAIExample = taskKey[Unit]("Runs Langchain4j Vertex AI example")
lazy val runLangchainGoogleAIExample = taskKey[Unit]("Runs Langchain4j Google AI example")

runLangchainClaudeExample := (langchain4j / Compile / runMain).toTask(" com.agenticai.core.llm.langchain.examples.SimpleClaudeExample").value
runLangchainVertexAIExample := (langchain4j / Compile / runMain).toTask(" com.agenticai.core.llm.langchain.examples.SimpleVertexAIExample").value
runLangchainGoogleAIExample := (langchain4j / Compile / runMain).toTask(" com.agenticai.core.llm.langchain.examples.SimpleGoogleAIGeminiExample").value

// Integration test configuration
lazy val it = project
  .in(file("it"))
  .configs(IntegrationTest)
  .settings(
    name := "integration-tests",
    commonSettings,
    Defaults.itSettings,
    libraryDependencies ++= commonDependencies
  )
  .dependsOn(core, memory, agents, http, dashboard, langchain4j)
