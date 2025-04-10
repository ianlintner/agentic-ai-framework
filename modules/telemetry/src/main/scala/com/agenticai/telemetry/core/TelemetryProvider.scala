package com.agenticai.telemetry.core

import io.opentelemetry.api.OpenTelemetry
import io.opentelemetry.api.trace.Tracer
import io.opentelemetry.context.Context
import io.opentelemetry.sdk.OpenTelemetrySdk
import io.opentelemetry.sdk.trace.SdkTracerProvider
import io.opentelemetry.sdk.trace.SpanProcessor
import io.opentelemetry.sdk.metrics.SdkMeterProvider
import zio.*

/**
 * Core telemetry service that manages OpenTelemetry SDK integration.
 * Provides pure ZIO interfaces for telemetry operations while maintaining referential transparency.
 */
trait TelemetryProvider {
  def tracer(instrumentationName: String): UIO[Tracer]
  def currentContext: UIO[Context]
  def withContext[R, E, A](context: Context)(zio: ZIO[R, E, A]): ZIO[R, E, A]
}

object TelemetryProvider {
  def live: ZLayer[TelemetryConfig, Nothing, TelemetryProvider] = ZLayer.scoped {
    for {
      config <- ZIO.service[TelemetryConfig]
      sdk <- ZIO.acquireRelease(
        ZIO.attempt {
          val tracerProvider = SdkTracerProvider.builder()
            .addSpanProcessor(config.spanProcessor)
            .build()

          val meterProvider = SdkMeterProvider.builder()
            .build()

          OpenTelemetrySdk.builder()
            .setTracerProvider(tracerProvider)
            .setMeterProvider(meterProvider)
            .buildAndRegisterGlobal()
        }.orDie
      )(sdk => ZIO.succeed(sdk.close()).orDie)
    } yield new TelemetryProvider {
      override def tracer(instrumentationName: String): UIO[Tracer] =
        ZIO.succeed(sdk.getTracer(instrumentationName))

      override def currentContext: UIO[Context] =
        ZIO.succeed(Context.current())

      override def withContext[R, E, A](context: Context)(zio: ZIO[R, E, A]): ZIO[R, E, A] =
        ZIO.succeed(context.makeCurrent()).acquireRelease(_ => ZIO.succeed(Context.current().close()))(_ => zio)
    }
  }

  // Accessor methods
  def tracer(instrumentationName: String): URIO[TelemetryProvider, Tracer] =
    ZIO.serviceWithZIO(_.tracer(instrumentationName))

  def currentContext: URIO[TelemetryProvider, Context] =
    ZIO.serviceWithZIO(_.currentContext)

  def withContext[R, E, A](context: Context)(zio: ZIO[R, E, A]): ZIO[R & TelemetryProvider, E, A] =
    ZIO.serviceWithZIO(_.withContext(context)(zio))
}