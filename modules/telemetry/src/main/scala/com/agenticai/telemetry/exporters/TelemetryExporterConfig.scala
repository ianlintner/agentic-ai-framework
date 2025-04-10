package com.agenticai.telemetry.exporters

import com.agenticai.telemetry.core.TelemetryConfig
import com.typesafe.config.Config
import io.opentelemetry.sdk.trace.SpanProcessor
import io.opentelemetry.sdk.trace.SdkTracerProvider
import zio.*

/**
 * Configuration for telemetry with support for multiple exporters.
 * Extends the base TelemetryConfig with exporter-specific settings.
 */
object TelemetryExporterConfig {

  /**
   * Create a TelemetryConfig from a Typesafe Config
   */
  def fromConfig(config: Config): Task[TelemetryConfig] = {
    ZIO.attempt {
      val serviceName = config.getString("telemetry.service-name")
      val samplingRatio = config.getDouble("telemetry.sampling-ratio")
      
      // Exporter configuration
      val exporterConfig = Exporters.ExporterConfig(
        enablePrometheus = config.getBoolean("telemetry.exporters.prometheus.enabled"),
        enableJaeger = config.getBoolean("telemetry.exporters.jaeger.enabled"),
        enableOtlp = config.getBoolean("telemetry.exporters.otlp.enabled"),
        enableConsole = config.getBoolean("telemetry.exporters.console.enabled"),
        prometheusPort = config.getInt("telemetry.exporters.prometheus.port"),
        jaegerEndpoint = config.getString("telemetry.exporters.jaeger.endpoint"),
        otlpEndpoint = config.getString("telemetry.exporters.otlp.endpoint")
      )
      
      // Create a dummy span processor initially - this will be replaced when the 
      // OpenTelemetry SDK is properly initialized with the selected exporters
      val dummySpanProcessor = SdkTracerProvider.builder()
        .build()
        .getSpanProcessor()
      
      TelemetryConfig(
        spanProcessor = dummySpanProcessor,
        serviceName = serviceName,
        samplingRatio = samplingRatio
      )
    }
  }
  
  /**
   * Default configuration for local development
   */
  val local: ULayer[TelemetryConfig] = ZLayer.succeed {
    // Create a dummy span processor initially
    val dummySpanProcessor = SdkTracerProvider.builder()
      .build()
      .getSpanProcessor()
      
    TelemetryConfig(
      spanProcessor = dummySpanProcessor,
      serviceName = "agentic-ai-local",
      samplingRatio = 1.0
    )
  }
  
  /**
   * Sample reference.conf for telemetry configuration:
   *
   * telemetry {
   *   service-name = "agentic-ai-service"
   *   sampling-ratio = 1.0
   *   
   *   exporters {
   *     prometheus {
   *       enabled = true
   *       port = 9464
   *     }
   *     
   *     jaeger {
   *       enabled = true
   *       endpoint = "http://localhost:14250"
   *     }
   *     
   *     otlp {
   *       enabled = true
   *       endpoint = "http://localhost:4317"
   *     }
   *     
   *     console {
   *       enabled = true
   *     }
   *   }
   * }
   */
}