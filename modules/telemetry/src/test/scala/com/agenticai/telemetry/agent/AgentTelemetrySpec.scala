package com.agenticai.telemetry.agent

import com.agenticai.telemetry.core.{TelemetryConfig, TelemetryProvider}
import io.opentelemetry.sdk.metrics.data.MetricData
import io.opentelemetry.sdk.trace.data.SpanData
import io.opentelemetry.sdk.testing.exporter.InMemoryMetricExporter
import io.opentelemetry.sdk.testing.exporter.InMemorySpanExporter
import io.opentelemetry.sdk.trace.SdkTracerProvider
import io.opentelemetry.sdk.trace.SpanProcessor
import scala.jdk.CollectionConverters.*
import zio.*
import zio.test.*
import zio.test.Assertion.*
import zio.test.TestAspect.*

object AgentTelemetrySpec extends ZIOSpecDefault {

  def spec = suite("AgentTelemetrySpec")(
    test("records agent execution metrics") {
      for {
        // Setup test environment
        metricExporter <- ZIO.succeed(InMemoryMetricExporter.create())
        spanExporter <- ZIO.succeed(InMemorySpanExporter.create())
        spanProcessor = SdkTracerProvider.builder()
          .addSpanProcessor(spanExporter)
          .build()
          .getSpanProcessor()
        config = TelemetryConfig(spanProcessor)
        
        // Run test
        _ <- ZIO.serviceWithZIO[AgentTelemetry] { telemetry =>
          telemetry.recordAgentExecution("test-agent", "process", 100L)
        }
        
        // Verify metrics
        metrics <- ZIO.succeed(metricExporter.getFinishedMetricItems)
      } yield assert(metrics)(hasSize(equalTo(2))) && // Duration histogram and operations counter
        assert(metrics.asScala.exists(_.getName == "agent.execution.duration"))(isTrue) &&
        assert(metrics.asScala.exists(_.getName == "agent.operations"))(isTrue)
    }.provide(
      TelemetryProvider.live,
      AgentTelemetry.live,
      ZLayer.succeed(TelemetryConfig(SdkTracerProvider.builder()
        .addSpanProcessor(InMemorySpanExporter.create())
        .build()
        .getSpanProcessor()))
    ),

    test("records LLM token usage and costs") {
      for {
        metricExporter <- ZIO.succeed(InMemoryMetricExporter.create())
        spanExporter <- ZIO.succeed(InMemorySpanExporter.create())
        spanProcessor = SdkTracerProvider.builder()
          .addSpanProcessor(spanExporter)
          .build()
          .getSpanProcessor()
        config = TelemetryConfig(spanProcessor)
        
        _ <- ZIO.serviceWithZIO[AgentTelemetry] { telemetry =>
          telemetry.recordLLMTokens("test-agent", 100L, 50L, 0.015)
        }
        
        metrics <- ZIO.succeed(metricExporter.getFinishedMetricItems)
      } yield assert(metrics)(hasSize(equalTo(2))) && // Token counter and cost counter
        assert(metrics.asScala.exists(_.getName == "llm.tokens"))(isTrue) &&
        assert(metrics.asScala.exists(_.getName == "llm.cost"))(isTrue)
    }.provide(
      TelemetryProvider.live,
      AgentTelemetry.live,
      ZLayer.succeed(TelemetryConfig(SdkTracerProvider.builder()
        .addSpanProcessor(InMemorySpanExporter.create())
        .build()
        .getSpanProcessor()))
    ),

    test("records workflow step metrics") {
      for {
        metricExporter <- ZIO.succeed(InMemoryMetricExporter.create())
        spanExporter <- ZIO.succeed(InMemorySpanExporter.create())
        spanProcessor = SdkTracerProvider.builder()
          .addSpanProcessor(spanExporter)
          .build()
          .getSpanProcessor()
        config = TelemetryConfig(spanProcessor)
        
        _ <- ZIO.serviceWithZIO[AgentTelemetry] { telemetry =>
          telemetry.recordWorkflowStep("workflow-1", "step-1", "completed")
        }
        
        metrics <- ZIO.succeed(metricExporter.getFinishedMetricItems)
      } yield assert(metrics)(hasSize(equalTo(1))) && // Workflow steps counter
        assert(metrics.asScala.exists(_.getName == "workflow.steps"))(isTrue)
    }.provide(
      TelemetryProvider.live,
      AgentTelemetry.live,
      ZLayer.succeed(TelemetryConfig(SdkTracerProvider.builder()
        .addSpanProcessor(InMemorySpanExporter.create())
        .build()
        .getSpanProcessor()))
    ),

    test("agent telemetry aspect instruments operations") {
      for {
        spanExporter <- ZIO.succeed(InMemorySpanExporter.create())
        spanProcessor = SdkTracerProvider.builder()
          .addSpanProcessor(spanExporter)
          .build()
          .getSpanProcessor()
        config = TelemetryConfig(spanProcessor)
        
        // Test operation with instrumentation
        result <- ZIO.succeed("test").pipe(
          AgentTelemetryAspect.instrumentAgent("test-agent", "test-op")
        )
        
        spans <- ZIO.succeed(spanExporter.getFinishedSpanItems)
      } yield assert(spans)(hasSize(equalTo(1))) &&
        assert(spans.get(0).getName)(equalTo("agent.test-op")) &&
        assert(spans.get(0).getAttributes.get("agent_id"))(equalTo("test-agent"))
    }.provide(
      TelemetryProvider.live,
      AgentTelemetry.live,
      ZLayer.succeed(TelemetryConfig(SdkTracerProvider.builder()
        .addSpanProcessor(InMemorySpanExporter.create())
        .build()
        .getSpanProcessor()))
    ),

    test("workflow step aspect records step lifecycle") {
      for {
        metricExporter <- ZIO.succeed(InMemoryMetricExporter.create())
        spanExporter <- ZIO.succeed(InMemorySpanExporter.create())
        spanProcessor = SdkTracerProvider.builder()
          .addSpanProcessor(spanExporter)
          .build()
          .getSpanProcessor()
        config = TelemetryConfig(spanProcessor)
        
        // Test workflow step with instrumentation
        result <- ZIO.succeed("test").pipe(
          AgentTelemetryAspect.instrumentWorkflowStep("workflow-1", "step-1")
        )
        
        metrics <- ZIO.succeed(metricExporter.getFinishedMetricItems)
        spans <- ZIO.succeed(spanExporter.getFinishedSpanItems)
      } yield assert(metrics.asScala.count(_.getName == "workflow.steps"))(equalTo(2)) && // Started and completed
        assert(spans)(hasSize(equalTo(1))) &&
        assert(spans.get(0).getName)(equalTo("workflow.step"))
    }.provide(
      TelemetryProvider.live,
      AgentTelemetry.live,
      ZLayer.succeed(TelemetryConfig(SdkTracerProvider.builder()
        .addSpanProcessor(InMemorySpanExporter.create())
        .build()
        .getSpanProcessor()))
    )
  ) @@ sequential
}