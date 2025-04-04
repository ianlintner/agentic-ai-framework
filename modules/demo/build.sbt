name := "agentic-ai-demo"
description := "Standalone demos for the Agentic AI Framework"

// Use Scala 3.3.1 to match the main project requirement
scalaVersion := "3.3.1"

// Explicitly add standard repositories
resolvers ++= Seq(
  Resolver.mavenLocal,
  Resolver.mavenCentral, 
  "Sonatype Releases" at "https://oss.sonatype.org/content/repositories/releases",
  "Typesafe repository" at "https://repo.typesafe.com/typesafe/releases/",
  "Scala Library" at "https://scala-lang.org/files/archive/",
  "Scala 3" at "https://repo1.maven.org/maven2/org/scala-lang/"
) 

// Dependencies for running Claude demos
val zioVersion = "2.0.19" 
val googleCloudVersion = "3.5.0"
val googleAuthVersion = "1.19.0"

libraryDependencies ++= Seq(
  // ZIO dependencies (must specify cross-compilation for Scala 3)
  "dev.zio" %% "zio" % zioVersion,
  "dev.zio" %% "zio-streams" % zioVersion,

  // Google Cloud dependencies for Vertex AI
  "com.google.cloud" % "google-cloud-aiplatform" % googleCloudVersion,
  "com.google.auth" % "google-auth-library-oauth2-http" % googleAuthVersion,
  
  // Terminal visualization and ANSI colors
  "com.lihaoyi" %% "fansi" % "0.4.0",
  
  // Depend on our own core module for circuit patterns
  "com.agenticai" %% "agentic-ai-core" % "0.1.0-SNAPSHOT"
)

// Use Coursier for dependency resolution (modern SBT default)
ThisBuild / useCoursier := true

// Basic settings for running
Compile / run / fork := true
Compile / run / javaOptions ++= Seq("-Xms512m", "-Xmx2g")

// For running the standalone demo
ThisBuild / connectInput := true