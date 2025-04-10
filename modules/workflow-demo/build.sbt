// No need to import dependencies

name := "agentic-ai-workflow-demo"
organization := "com.agenticai"

lazy val workflowDemo = project
  .in(file("."))
  .settings(
    scalaVersion := "3.3.1",
    libraryDependencies ++= Seq(
      // ZIO Core
      "dev.zio" %% "zio" % "2.0.19",
      "dev.zio" %% "zio-streams" % "2.0.19",
      
      // Web and API
      "dev.zio" %% "zio-http" % "3.0.0-RC4",  // Updated to latest RC version
      "dev.zio" %% "zio-json" % "0.6.2",
      
      // Configuration and Logging
      "dev.zio" %% "zio-config" % "4.0.3",
      "dev.zio" %% "zio-config-typesafe" % "4.0.3",
      "dev.zio" %% "zio-logging" % "2.1.14",
      "dev.zio" %% "zio-logging-slf4j" % "2.1.14",
      
      // Google Cloud dependencies
      "com.google.cloud" % "google-cloud-aiplatform" % "3.49.0",
      "com.google.cloud" % "google-cloud-storage" % "2.27.1",
      
      // Logging implementation
      "ch.qos.logback" % "logback-classic" % "1.5.17",
      
      // Test dependencies
      "dev.zio" %% "zio-test" % "2.0.19" % Test,
      "dev.zio" %% "zio-test-sbt" % "2.0.19" % Test,
      "dev.zio" %% "zio-test-junit" % "2.0.19" % Test
    ),
    Compile / resourceDirectory := baseDirectory.value / "src" / "main" / "resources",
    
    // Add resources to class path
    Compile / unmanagedResourceDirectories += baseDirectory.value / "src" / "main" / "resources" / "public",
    
    // Test configuration
    testFrameworks += new TestFramework("zio.test.sbt.ZTestFramework"),
    
    // Set the default main class to fix the selectMainClass issue
    Compile / mainClass := Some("com.agenticai.workflow.WorkflowDemoLauncher")
  )
  // Temporarily remove dependencies on core and memory to make the demo work
  // In a production setup, we'd properly configure the module dependencies