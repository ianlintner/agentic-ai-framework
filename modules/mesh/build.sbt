import Dependencies._

name := "agentic-ai-mesh"
description := "Distributed agent mesh network for the Agentic AI Framework"

libraryDependencies ++= Seq(
  // ZIO Core
  "dev.zio" %% "zio" % zioVersion,
  "dev.zio" %% "zio-streams" % zioVersion,
  
  // ZIO HTTP
  "dev.zio" %% "zio-http" % zioHttpVersion,
  
  // JSON
  "dev.zio" %% "zio-json" % zioJsonVersion,
  
  // Testing
  "dev.zio" %% "zio-test" % zioVersion % Test,
  "dev.zio" %% "zio-test-sbt" % zioVersion % Test,
  "dev.zio" %% "zio-test-magnolia" % zioVersion % Test
)

testFrameworks += new TestFramework("zio.test.sbt.ZTestFramework")