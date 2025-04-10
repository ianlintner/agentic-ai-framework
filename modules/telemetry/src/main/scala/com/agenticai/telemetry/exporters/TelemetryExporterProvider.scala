package com.agenticai.telemetry.exporters

import com.agenticai.telemetry.core.{TelemetryConfig, TelemetryProvider}
import io.opentelemetry.api.OpenTelemetry
import io.opentelemetry.api.trace.Tracer
import io.opentelemetry.context.Context
import io.opentelemetry.sdk.OpenTelemetrySdk
import zio.*

/**
 * Telemetry provider implementation that supports multiple exporters.
 * This extends the base TelemetryProvider with enhanced export capabilities.
 */
object TelemetryExporterProvider {

  /**
   * Creates a live TelemetryProvider layer with configured exporters.
   */
  def live: ZLayer[TelemetryConfig, Throwable, TelemetryProvider] = ZLayer.scoped {
    for {
      config <- ZIO.service[TelemetryConfig]
      exporterConfig = Exporters.ExporterConfig(
        enablePrometheus = true,
        enableJaeger = true,
        enableOtlp = true,
        enableConsole = true
      )
      sdk <- Exporters.createTelemetry(config.serviceName, exporterConfig)
    } yield new TelemetryProvider {
      override def tracer(instrumentationName: String): UIO[Tracer] =
        ZIO.succeed(sdk.getTracer(instrumentationName))

      override def currentContext: UIO[Context] =
        ZIO.succeed(Context.current())

      override def withContext[R, E, A](context: Context)(zio: ZIO[R, E, A]): ZIO[R, E, A] =
        ZIO.succeed(context.makeCurrent()).acquireRelease(_ => ZIO.succeed(Context.current().close()))(_ => zio)
    }
  }

  /**
   * Creates a TelemetryProvider layer with custom exporter configuration.
   */
  def custom(exporterConfig: Exporters.ExporterConfig): ZLayer[TelemetryConfig, Throwable, TelemetryProvider] = 
    ZLayer.scoped {
      for {
        config <- ZIO.service[TelemetryConfig]
        sdk <- Exporters.createTelemetry(config.serviceName, exporterConfig)
      } yield new TelemetryProvider {
        override def tracer(instrumentationName: String): UIO[Tracer] =
          ZIO.succeed(sdk.getTracer(instrumentationName))

        override def currentContext: UIO[Context] =
          ZIO.succeed(Context.current())

        override def withContext[R, E, A](context: Context)(zio: ZIO[R, E, A]): ZIO[R, E, A] =
          ZIO.succeed(context.makeCurrent()).acquireRelease(_ => ZIO.succeed(Context.current().close()))(_ => zio)
      }
    }
    
  /**
   * Creates a development configuration with console logging only.
   */
  def development: ZLayer[TelemetryConfig, Throwable, TelemetryProvider] = {
    val devConfig = Exporters.ExporterConfig(
      enablePrometheus = false,
      enableJaeger = false,
      enableOtlp = false,
      enableConsole = true
    )
    custom(devConfig)
  }
  
  /**
   * Creates a production configuration with all exporters except console.
   */
  def production: ZLayer[TelemetryConfig, Throwable, TelemetryProvider] = {
    val prodConfig = Exporters.ExporterConfig(
      enablePrometheus = true,
      enableJaeger = true,
      enableOtlp = true,
      enableConsole = false
    )
    custom(prodConfig)
  }
  
  /**
   * Creates a local docker-based development configuration with 
   * Prometheus and Jaeger enabled at localhost ports.
   */
  def localDocker: ZLayer[TelemetryConfig, Throwable, TelemetryProvider] = {
    val localConfig = Exporters.ExporterConfig(
      enablePrometheus = true,
      enableJaeger = true,
      enableOtlp = false,
      enableConsole = true,
      prometheusPort = 9464,
      jaegerEndpoint = "http://localhost:14250"
    )
    custom(localConfig)
  }
}