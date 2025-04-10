package com.agenticai.telemetry.mesh

import com.agenticai.telemetry.core.{TelemetryConfig, TelemetryProvider}
import io.opentelemetry.sdk.trace.SdkTracerProvider
import zio.*
import zio.test.*
import zio.test.Assertion.*

object MeshTelemetrySpec extends ZIOSpecDefault {

  // Test implementation of trace context ID to make testing deterministic
  val TEST_SOURCE_ID = "source-test-node"
  val TEST_DEST_ID = "dest-test-node"

  def spec = suite("MeshTelemetry")(
    test("records message sent metrics") {
      for {
        // Exercise the metrics
        _ <- MeshTelemetry.recordMessageSent(
          TEST_SOURCE_ID,
          TEST_DEST_ID,
          "TEST_MESSAGE",
          1024L
        )
      } yield assertCompletes
    },

    test("records message received metrics") {
      for {
        // Exercise the metrics
        _ <- MeshTelemetry.recordMessageReceived(
          TEST_SOURCE_ID,
          TEST_DEST_ID,
          "TEST_MESSAGE",
          1024L
        )
      } yield assertCompletes
    },

    test("records communication latency metrics") {
      for {
        // Exercise the metrics
        _ <- MeshTelemetry.recordCommunicationLatency(
          TEST_SOURCE_ID,
          TEST_DEST_ID,
          "TEST_MESSAGE",
          50.0
        )
      } yield assertCompletes
    },

    test("records connection attempt metrics") {
      for {
        // Exercise the metrics for success case
        _ <- MeshTelemetry.recordConnectionAttempt(
          TEST_SOURCE_ID,
          TEST_DEST_ID,
          true
        )
        
        // Exercise the metrics for failure case
        _ <- MeshTelemetry.recordConnectionAttempt(
          TEST_SOURCE_ID,
          TEST_DEST_ID,
          false
        )
      } yield assertCompletes
    },

    test("records agent discovery metrics") {
      for {
        // Exercise the metrics
        _ <- MeshTelemetry.recordAgentDiscovery(
          "capability",
          5,
          120L
        )
      } yield assertCompletes
    },

    test("records node health metrics") {
      for {
        // Exercise the metrics
        _ <- MeshTelemetry.recordNodeHealth(
          TEST_SOURCE_ID,
          3,
          "ACTIVE",
          0.7
        )
      } yield assertCompletes
    },

    test("MeshTelemetryAspect can instrument message send") {
      val testEffect = ZIO.succeed("Test message")
      
      for {
        // Apply aspect and run effect
        result <- testEffect.pipe(
          MeshTelemetryAspect.instrumentMessageSend(
            TEST_SOURCE_ID,
            TEST_DEST_ID,
            "TEST_MESSAGE"
          )
        )
      } yield assertTrue(result == "Test message")
    },

    test("MeshTraceContext propagates trace context") {
      // Create a test message envelope
      val testEnvelope = MessageEnvelope(
        java.util.UUID.randomUUID(),
        "TEST_MESSAGE",
        "Hello World".getBytes(),
        Map("key" -> "value")
      )
      
      for {
        // Inject context
        enrichedEnvelope <- MeshTraceContext.injectTraceContext(testEnvelope)
        
        // Extract context
        _ <- MeshTraceContext.extractTraceContext(enrichedEnvelope)
      } yield {
        // Verify trace context was added to metadata
        assert(enrichedEnvelope.metadata)(
          hasKey(startsWith("trace."))
        )
      }
    }
  ).provide(
    // Test dependencies
    MeshTelemetry.live,
    TelemetryProvider.live,
    ZLayer.succeed(TelemetryConfig(
      SdkTracerProvider.builder().build().getSpanProcessor()
    ))
  )
}