package com.agenticai.telemetry.examples

import com.agenticai.telemetry.agent.AgentTelemetry
import com.agenticai.telemetry.core.*
import com.agenticai.telemetry.exporters.*
import com.agenticai.telemetry.mesh.MeshTelemetry
import com.typesafe.config.ConfigFactory
import io.opentelemetry.api.trace.SpanKind
import zio.*

/**
 * Example demonstrating the telemetry system with exporters.
 * This example shows how to:
 * 1. Configure and set up exporters for telemetry data
 * 2. Run a simple instrumented workflow
 * 3. Export metrics and traces to monitoring systems
 */
object TelemetryExporterExample extends ZIOAppDefault {

  // Simulated agent types for the example
  sealed trait AgentType
  case object TextProcessor extends AgentType
  case object DataAnalyzer extends AgentType
  case object Summarizer extends AgentType

  // Simple message for the example
  case class Message(content: String, size: Int)

  // Simulated agent operation
  def processWithAgent(agentType: AgentType, input: Message): ZIO[AgentTelemetry, Nothing, Message] = {
    for {
      _ <- AgentTelemetry.recordAgentStart(agentType.toString)
      
      // Simulate processing time based on agent type
      _ <- ZIO.sleep(
        agentType match {
          case TextProcessor => 100.millis
          case DataAnalyzer => 200.millis
          case Summarizer => 150.millis
        }
      )
      
      // Record agent memory usage
      _ <- AgentTelemetry.recordAgentMemoryUsage(
        agentType.toString,
        (input.size * 1.5).toLong
      )
      
      // Simulate successful operation
      result = Message(
        s"Processed by ${agentType}: ${input.content}",
        input.size + 20
      )
      
      // Record agent operation
      _ <- AgentTelemetry.recordAgentOperation(
        agentType.toString,
        "process",
        success = true
      )
      
      // Record agent completion
      _ <- AgentTelemetry.recordAgentCompletion(agentType.toString)
    } yield result
  }

  // Simulated mesh communication
  def sendThroughMesh(from: String, to: String, message: Message): ZIO[MeshTelemetry, Nothing, Duration] = {
    for {
      startTime <- Clock.currentTime(TimeUnit.MILLISECONDS)
      
      // Record message sent
      _ <- MeshTelemetry.recordMessageSent(
        sourceNodeId = from,
        destinationNodeId = to,
        messageType = "text",
        messageSizeBytes = message.size.toLong
      )
      
      // Simulate network latency
      latency = (math.random() * 50).toLong + 10
      _ <- ZIO.sleep(latency.millis)
      
      // Record message received
      _ <- MeshTelemetry.recordMessageReceived(
        sourceNodeId = from,
        destinationNodeId = to,
        messageType = "text",
        messageSizeBytes = message.size.toLong
      )
      
      endTime <- Clock.currentTime(TimeUnit.MILLISECONDS)
      duration = Duration.fromMillis(endTime - startTime)
      
      // Record communication latency
      _ <- MeshTelemetry.recordCommunicationLatency(
        sourceNodeId = from,
        destinationNodeId = to,
        messageType = "text",
        latencyMs = duration.toMillis.toDouble
      )
    } yield duration
  }

  // Simulated LLM usage
  def callLLM(prompt: String): ZIO[TelemetryProvider, Nothing, String] = {
    ZIO.serviceWithZIO[TelemetryProvider] { telemetry =>
      for {
        tracer <- telemetry.tracer("agentic-ai.llm")
        
        result <- ZIO.acquireReleaseWith(
          ZIO.succeed(tracer.spanBuilder("llm.call")
            .setSpanKind(SpanKind.CLIENT)
            .setAttribute("llm.provider", "OpenAI")
            .setAttribute("llm.model", "gpt-4")
            .setAttribute("llm.prompt.tokens", prompt.split("\\s+").length)
            .startSpan())
        )(span => ZIO.succeed(span.end())) { span =>
          // Simulate LLM call latency
          for {
            _ <- ZIO.sleep((math.random() * 500).toLong.millis + 200.millis)
            response = s"LLM response to: $prompt"
            _ = span.setAttribute("llm.completion.tokens", response.split("\\s+").length)
          } yield response
        }
      } yield result
    }
  }

  // Main workflow that uses instrumented agents and mesh communication
  def simulateWorkflow(input: String): ZIO[AgentTelemetry & MeshTelemetry & TelemetryProvider, Nothing, String] = {
    for {
      tracer <- ZIO.serviceWithZIO[TelemetryProvider](_.tracer("agentic-ai.workflow"))
      
      result <- ZIO.acquireReleaseWith(
        ZIO.succeed(tracer.spanBuilder("workflow.example")
          .setSpanKind(SpanKind.INTERNAL)
          .setAttribute("workflow.name", "telemetry-example")
          .setAttribute("workflow.version", "1.0")
          .startSpan())
      )(span => ZIO.succeed(span.end())) { span =>
        for {
          // Initial message
          initialMsg = Message(input, input.length)
          
          // Process with text processor
          _ = span.addEvent("Starting TextProcessor agent")
          processedMsg <- processWithAgent(TextProcessor, initialMsg)
          
          // Send through mesh to data analyzer
          _ = span.addEvent("Sending to DataAnalyzer agent")
          meshLatency1 <- sendThroughMesh("node1", "node2", processedMsg)
          _ = span.setAttribute("mesh.latency.ms", meshLatency1.toMillis)
          
          // Process with data analyzer
          _ = span.addEvent("Starting DataAnalyzer agent")
          analyzedMsg <- processWithAgent(DataAnalyzer, processedMsg)
          
          // Call LLM for enhancement
          _ = span.addEvent("Calling LLM")
          llmResponse <- callLLM(analyzedMsg.content)
          enhancedMsg = Message(llmResponse, llmResponse.length)
          
          // Send through mesh to summarizer
          _ = span.addEvent("Sending to Summarizer agent")
          meshLatency2 <- sendThroughMesh("node2", "node3", enhancedMsg)
          _ = span.setAttribute("mesh.latency2.ms", meshLatency2.toMillis)
          
          // Process with summarizer
          _ = span.addEvent("Starting Summarizer agent")
          finalMsg <- processWithAgent(Summarizer, enhancedMsg)
          
          // Final result
          _ = span.addEvent("Workflow completed")
          _ = span.setAttribute("result.size", finalMsg.size)
        } yield finalMsg.content
      }
    } yield result
  }

  /**
   * Main program that demonstrates the telemetry exporters
   */
  override def run: ZIO[Any, Any, Unit] = {
    // Load configuration
    val config = ConfigFactory.load()
    
    // Create exporter configuration
    val exporterConfig = Exporters.ExporterConfig(
      enablePrometheus = true,
      enableJaeger = true,
      enableOtlp = false,
      enableConsole = true,
      prometheusPort = 9464 
    )

    // Create pipeline of 10 sequential workflow executions
    val workflowPipeline = ZIO.foreach(1 to 10) { i =>
      for {
        _ <- Console.printLine(s"Starting workflow execution #$i")
        startTime <- Clock.currentTime(TimeUnit.MILLISECONDS)
        result <- simulateWorkflow(s"Input data for workflow execution #$i")
        endTime <- Clock.currentTime(TimeUnit.MILLISECONDS)
        duration = Duration.fromMillis(endTime - startTime)
        _ <- Console.printLine(s"Workflow execution #$i completed in ${duration.toMillis}ms with result: $result")
      } yield ()
    }

    // Create program with required layers
    val program = workflowPipeline.provide(
      // Provide telemetry configuration
      TelemetryExporterConfig.local,
      
      // Provide telemetry provider with exporters
      TelemetryExporterProvider.localDocker,
      
      // Provide agent telemetry
      AgentTelemetry.live,
      
      // Provide mesh telemetry
      MeshTelemetry.live
    )

    // Run the example
    program.tap(_ => 
      Console.printLine("""
        |Telemetry example completed.
        |
        |Metrics are available at:
        |- Prometheus: http://localhost:9464/metrics
        |
        |If you've started the monitoring stack with run-monitoring.sh:
        |- Grafana: http://localhost:3000 (admin/admin)
        |- Jaeger: http://localhost:16686
        |""".stripMargin)
    )
  }
}