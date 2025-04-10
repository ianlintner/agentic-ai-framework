package com.agenticai.telemetry.examples

import com.agenticai.telemetry.core.*
import com.typesafe.config.ConfigFactory
import zio.*
import io.opentelemetry.api.trace.StatusCode

/**
 * Example demonstrating the usage of the telemetry system.
 * Shows how to configure telemetry and use tracing aspects.
 */
object TelemetryExample extends ZIOAppDefault {

  /**
   * Simulates a service operation that we want to trace
   */
  def processData(data: String): ZIO[TelemetryProvider, Nothing, String] =
    ZIO.succeed(s"Processed: $data")
      .inject(TelemetryAspect.traced(
        operationName = "process-data",
        attributes = Map(
          "data.size" -> data.length.toString,
          "data.type" -> "string"
        )
      ))

  /**
   * Simulates a workflow with multiple traced operations
   */
  def simulateWorkflow: ZIO[TelemetryProvider, Nothing, Unit] = {
    val workflow = for {
      _ <- ZIO.succeed("Starting workflow")
        .inject(TelemetryAspect.traced("workflow-start"))
      
      data <- ZIO.succeed("example-data")
        .inject(TelemetryAspect.traced("data-preparation"))
      
      result <- processData(data)
      
      _ <- ZIO.succeed(s"Completed with: $result")
        .inject(TelemetryAspect.traced("workflow-complete"))
    } yield ()

    // Add workflow-level tracing
    workflow.inject(TelemetryAspect.traced(
      operationName = "example-workflow",
      attributes = Map(
        "workflow.type" -> "example",
        "workflow.version" -> "1.0"
      )
    ))
  }

  override def run: ZIO[Any, Nothing, Unit] = {
    // Load configuration
    val config = ConfigFactory.load()
    
    // Create program with required layers
    val program = simulateWorkflow.provide(
      // Provide telemetry configuration
      TelemetryConfig.live,
      // Provide telemetry service
      TelemetryProvider.live,
      // Provide config
      ZLayer.succeed(config)
    )

    // Run the example
    program.tap(_ => 
      Console.printLine("Telemetry example completed. Check your OpenTelemetry collector for traces.")
    )
  }
}