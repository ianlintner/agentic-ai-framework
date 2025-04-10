import Dependencies._
import Langchain4jDependencies._

ThisBuild / scalaVersion := "3.3.1"
ThisBuild / version := "0.1.0-SNAPSHOT"
ThisBuild / organization := "com.agenticai"

//-----------------------------------------------------------------------------
// PROJECTS
//-----------------------------------------------------------------------------

lazy val root = (project in file("."))
  .settings(name := "agentic-ai-framework")
  .settings(commonSettings)
  .settings(
    libraryDependencies ++= commonDependencies
  )
  .aggregate(
    mesh,
    core,
    memory,
    agents,
    examples,
    dashboard,
    http,
    langchain4j,
    demo,
    workflowDemo
  )

lazy val mesh = (project in file("modules/mesh"))
  .settings(name := "agentic-ai-mesh")
  .settings(commonSettings)
  .settings(
    libraryDependencies ++= commonDependencies ++ Seq(
      // add dependencies here
    )
  ).dependsOn(core)

lazy val core = (project in file("modules/core"))
  .settings(name := "agentic-ai-core")
  .settings(commonSettings)
  .settings(
    libraryDependencies ++= commonDependencies ++ Seq(
      "dev.zio" %% "zio-json" % "0.6.2",
      "dev.zio" %% "zio-logging" % "2.1.14",
      "com.softwaremill.sttp.client3" %% "core" % "3.9.1",
      "com.softwaremill.sttp.client3" %% "zio" % "3.9.1",
      "com.google.cloud" % "google-cloud-aiplatform" % "3.49.0",
      "com.google.cloud" % "google-cloud-storage" % "2.27.1"
    )
  )

lazy val memory = (project in file("modules/memory"))
  .settings(name := "agentic-ai-memory")
  .settings(commonSettings)
  .settings(
    libraryDependencies ++= commonDependencies ++ Seq(
      // add dependencies here
    )
  ).dependsOn(core)

lazy val agents = (project in file("modules/agents"))
  .settings(name := "agentic-ai-agents")
  .settings(commonSettings)
  .settings(
    libraryDependencies ++= commonDependencies ++ Seq(
      // add dependencies here
    )
  ).dependsOn(core, memory)

lazy val examples = (project in file("modules/examples"))
  .settings(name := "agentic-ai-examples")
  .settings(commonSettings)
  .settings(
    libraryDependencies ++= commonDependencies ++ Seq(
      // add dependencies here
    )
  ).dependsOn(core, memory, langchain4j, mesh)

lazy val dashboard = (project in file("modules/dashboard"))
  .settings(name := "agentic-ai-dashboard")
  .settings(commonSettings)
  .settings(
    libraryDependencies ++= commonDependencies ++ Seq(
      "dev.zio" %% "zio-http" % "3.0.0-RC2",
      "dev.zio" %% "zio-json" % "0.6.2"
    )
  ).dependsOn(core)

lazy val http = (project in file("modules/http"))
  .settings(name := "agentic-ai-http")
  .settings(commonSettings)
  .settings(
    libraryDependencies ++= commonDependencies ++ Seq(
      "dev.zio" %% "zio-http" % "3.0.0-RC2",
      "dev.zio" %% "zio-json" % "0.6.2"
    )
  ).dependsOn(core)

lazy val langchain4j = (project in file("modules/langchain4j"))
  .settings(name := "agentic-ai-langchain4j")
  .settings(commonSettings)
  .settings(
    libraryDependencies ++= commonDependencies ++ Seq(
      Langchain4jDependencies.langchain4jCore,
      Langchain4jDependencies.langchain4jVertexAi,
      Langchain4jDependencies.langchain4jAnthropic,
      Langchain4jDependencies.langchain4jHttpClient,
      Langchain4jDependencies.langchain4jHttpClientJdk
    )
  ).dependsOn(core)

lazy val demo = (project in file("modules/demo"))
  .settings(name := "agentic-ai-demo")
  .settings(commonSettings)
  .settings(
    libraryDependencies ++= commonDependencies ++ Seq(
      "dev.zio" %% "zio-http" % "3.0.0-RC2"
    )
  ).dependsOn(core, memory, langchain4j)

lazy val workflowDemo = (project in file("modules/workflow-demo"))
  .settings(name := "agentic-ai-workflow-demo")
  .settings(commonSettings)
  .settings(
    libraryDependencies ++= commonDependencies ++ Seq(
      "dev.zio" %% "zio-http" % "3.0.0-RC2",
      "dev.zio" %% "zio-json" % "0.6.2"
    )
  ).dependsOn(core, memory)

lazy val it = (project in file("it"))
  .settings(name := "agentic-ai-it")
  .settings(commonSettings)
  .settings(
    libraryDependencies ++= commonDependencies ++ Seq(
      // add dependencies here
    )
  ).dependsOn(core)

//-----------------------------------------------------------------------------
// DEPENDENCIES
//-----------------------------------------------------------------------------

lazy val commonDependencies = Seq(
  // ZIO
  "dev.zio" %% "zio" % "2.0.19",
  "dev.zio" %% "zio-streams" % "2.0.19",
  
  // Testing
  "dev.zio" %% "zio-test" % "2.0.19" % Test,
  "dev.zio" %% "zio-test-sbt" % "2.0.19" % Test,
  "org.scalactic" %% "scalactic" % "3.2.17" % Test,
  "org.scalatest" %% "scalatest" % "3.2.17" % Test
)

//-----------------------------------------------------------------------------
// COMMON SETTINGS
//-----------------------------------------------------------------------------

lazy val commonSettings = Seq(
  Compile / scalacOptions ++= Seq(
    "-source:3.3",
    "-deprecation",
    "-feature",
    "-unchecked",
    "-Xcheck-macros",
    "-Xfatal-warnings"
  ),
  Compile / console / scalacOptions --= Seq("-Xfatal-warnings"),
  Test / testOptions += Tests.Argument("-oD"),
  Test / fork := true,
  Test / javaOptions ++= Seq("-Xms512M", "-Xmx2048M"),
  Test / parallelExecution := false
)
