package com.agenticai.telemetry.core

import com.typesafe.config.Config
import io.opentelemetry.sdk.trace.SpanProcessor
import io.opentelemetry.sdk.trace.SdkTracerProvider
import io.opentelemetry.exporter.otlp.trace.OtlpGrpcSpanExporter
import zio.*

/**
 * Configuration for the telemetry system.
 * Handles OpenTelemetry configuration including exporters and sampling.
 *
 * @param spanProcessor The configured SpanProcessor for trace export
 * @param serviceName The name of the service for identification
 * @param samplingRatio The sampling ratio for traces (0.0 to 1.0)
 */
final case class TelemetryConfig(
  spanProcessor: SpanProcessor,
  serviceName: String,
  samplingRatio: Double
)

object TelemetryConfig {
  def fromConfig(config: Config): Task[TelemetryConfig] = {
    ZIO.attempt {
      val serviceName = config.getString("telemetry.service-name")
      val endpoint = config.getString("telemetry.otlp.endpoint")
      val samplingRatio = config.getDouble("telemetry.sampling-ratio")

      val spanExporter = OtlpGrpcSpanExporter.builder()
        .setEndpoint(endpoint)
        .build()

      val spanProcessor = SdkTracerProvider.builder()
        .addSpanProcessor(spanExporter)
        .build()
        .getSpanProcessor()

      TelemetryConfig(
        spanProcessor = spanProcessor,
        serviceName = serviceName,
        samplingRatio = samplingRatio
      )
    }
  }

  def live: ZLayer[Config, Throwable, TelemetryConfig] =
    ZLayer.fromZIO(ZIO.service[Config].flatMap(fromConfig))

  // Default configuration for testing
  val test: ULayer[TelemetryConfig] = ZLayer.succeed {
    val spanExporter = OtlpGrpcSpanExporter.builder()
      .setEndpoint("http://localhost:4317")
      .build()

    val spanProcessor = SdkTracerProvider.builder()
      .addSpanProcessor(spanExporter)
      .build()
      .getSpanProcessor()

    TelemetryConfig(
      spanProcessor = spanProcessor,
      serviceName = "test-service",
      samplingRatio = 1.0
    )
  }
}