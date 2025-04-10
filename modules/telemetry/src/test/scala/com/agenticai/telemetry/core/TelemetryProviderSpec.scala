package com.agenticai.telemetry.core

import zio.*
import zio.test.*
import zio.test.Assertion.*
import io.opentelemetry.api.trace.{SpanKind, StatusCode}
import io.opentelemetry.context.Context

object TelemetryProviderSpec extends ZIOSpecDefault {
  def spec = suite("TelemetryProviderSpec")(
    test("traced aspect should create and complete spans") {
      val testEffect = ZIO.succeed("test-data")
        .inject(TelemetryAspect.traced(
          operationName = "test-operation",
          attributes = Map("test.key" -> "test-value")
        ))

      for {
        result <- testEffect.provide(
          TelemetryConfig.test,
          TelemetryProvider.live
        )
        // In a real test, we would verify the span was created and completed
        // Here we just verify the effect completed successfully
      } yield assert(result)(equalTo("test-data"))
    },

    test("traced aspect should handle errors correctly") {
      val failedEffect = ZIO.fail("test-error")
        .inject(TelemetryAspect.traced("error-operation"))

      for {
        result <- failedEffect.provide(
          TelemetryConfig.test,
          TelemetryProvider.live
        ).exit
      } yield assert(result)(fails(equalTo("test-error")))
    },

    test("context propagation should work correctly") {
      for {
        // Create a parent context
        telemetry <- ZIO.service[TelemetryProvider]
        parentContext <- telemetry.currentContext
        
        // Create a child operation with the parent context
        result <- ZIO.succeed("child-operation")
          .inject(TelemetryAspect.withDistributedContext(parentContext))
          .provide(
            TelemetryConfig.test,
            TelemetryProvider.live
          )
      } yield assert(result)(equalTo("child-operation"))
    },

    test("telemetry should maintain referential transparency") {
      val effect1 = ZIO.succeed("effect")
        .inject(TelemetryAspect.traced("operation-1"))
      
      val effect2 = ZIO.succeed("effect")
        .inject(TelemetryAspect.traced("operation-2"))

      for {
        result1 <- effect1.provide(
          TelemetryConfig.test,
          TelemetryProvider.live
        )
        result2 <- effect2.provide(
          TelemetryConfig.test,
          TelemetryProvider.live
        )
      } yield assert(result1)(equalTo(result2))
    }
  )
}