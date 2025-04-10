package com.agenticai.telemetry.examples

import com.agenticai.telemetry.agent.{AgentTelemetry, AgentTelemetryAspect}
import com.agenticai.telemetry.core.{TelemetryConfig, TelemetryProvider}
import io.opentelemetry.sdk.trace.SdkTracerProvider
import zio.*

/**
 * Example demonstrating how to instrument an agent with telemetry.
 * Shows usage of agent telemetry aspects and metrics recording.
 */
object InstrumentedAgentExample extends ZIOAppDefault {

  // Example agent that processes text
  class TextProcessingAgent(id: String) {
    def process(text: String): ZIO[AgentTelemetry & TelemetryProvider, Nothing, String] =
      ZIO.succeed(text.toUpperCase)
        .pipe(AgentTelemetryAspect.instrumentAgent(id, "process"))

    def summarize(text: String): ZIO[AgentTelemetry & TelemetryProvider, Nothing, String] =
      // Simulate LLM call
      ZIO.succeed(s"Summary of: $text")
        .pipe(AgentTelemetryAspect.instrumentAgent(id, "summarize"))
        .tap(_ => AgentTelemetry.recordLLMTokens(id, 50, 20, 0.001))
  }

  // Example workflow that uses multiple agents
  class DocumentWorkflow(workflowId: String) {
    private val processingAgent = new TextProcessingAgent("processor-1")
    private val summarizationAgent = new TextProcessingAgent("summarizer-1")

    def processDocument(text: String): ZIO[AgentTelemetry & TelemetryProvider, Nothing, String] = {
      for {
        // Process text with first agent
        processed <- processingAgent.process(text)
          .pipe(AgentTelemetryAspect.instrumentWorkflowStep(workflowId, "process"))

        // Summarize with second agent  
        summary <- summarizationAgent.summarize(processed)
          .pipe(AgentTelemetryAspect.instrumentWorkflowStep(workflowId, "summarize"))
      } yield summary
    }
  }

  override def run = {
    val program = for {
      // Create workflow
      workflow = new DocumentWorkflow("workflow-1")
      
      // Process a document
      _ <- Console.printLine("Processing document...")
      result <- workflow.processDocument("Example text to process and summarize")
      _ <- Console.printLine(s"Result: $result")
    } yield ()

    // Provide telemetry layers
    program.provide(
      // Core telemetry
      TelemetryProvider.live,
      ZLayer.succeed(TelemetryConfig(
        SdkTracerProvider.builder().build().getSpanProcessor()
      )),
      
      // Agent telemetry
      AgentTelemetry.live
    )
  }
}