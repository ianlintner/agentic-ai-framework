package com.agenticai.telemetry.exporters

import com.agenticai.telemetry.core.TelemetryConfig
import io.opentelemetry.api.common.Attributes
import io.opentelemetry.sdk.resources.Resource
import io.opentelemetry.semconv.ResourceAttributes
import io.opentelemetry.sdk.trace.SdkTracerProvider
import io.opentelemetry.sdk.trace.SpanProcessor
import io.opentelemetry.sdk.trace.SimpleSpanProcessor
import io.opentelemetry.sdk.trace.BatchSpanProcessor
import io.opentelemetry.sdk.metrics.SdkMeterProvider
import io.opentelemetry.sdk.metrics.PeriodicMetricReader
import io.opentelemetry.exporter.otlp.trace.OtlpGrpcSpanExporter
import io.opentelemetry.exporter.otlp.metrics.OtlpGrpcMetricExporter
import io.opentelemetry.exporter.logging.LoggingSpanExporter
import io.opentelemetry.exporter.jaeger.JaegerGrpcSpanExporter
import io.opentelemetry.exporter.prometheus.PrometheusHttpServer
import io.opentelemetry.sdk.OpenTelemetrySdk
import io.opentelemetry.context.propagation.ContextPropagators
import io.opentelemetry.api.trace.propagation.W3CTraceContextPropagator
import io.opentelemetry.api.baggage.propagation.W3CBaggagePropagator
import zio.*

import java.time.Duration

/**
 * Factory for creating OpenTelemetry SDK instances configured with different exporters.
 * Supports configuration for Prometheus, Jaeger, OTLP, and Console exporters.
 */
object Exporters {
  
  /**
   * Configuration for telemetry exporters
   *
   * @param enablePrometheus Enable Prometheus metrics exporter
   * @param enableJaeger Enable Jaeger tracing exporter
   * @param enableOtlp Enable OTLP exporter
   * @param enableConsole Enable console logging for telemetry
   * @param prometheusPort Port for Prometheus server (default: 9464)
   * @param jaegerEndpoint Jaeger collector endpoint (default: http://localhost:14250)
   * @param otlpEndpoint OTLP collector endpoint (default: http://localhost:4317)
   * @param samplingRatio Sampling ratio for traces (0.0-1.0)
   */
  final case class ExporterConfig(
    enablePrometheus: Boolean = false,
    enableJaeger: Boolean = false,
    enableOtlp: Boolean = false,
    enableConsole: Boolean = false,
    prometheusPort: Int = 9464,
    jaegerEndpoint: String = "http://localhost:14250",
    otlpEndpoint: String = "http://localhost:4317",
    samplingRatio: Double = 1.0
  )
  
  /**
   * Creates a configured OpenTelemetry SDK with the specified exporters
   */
  def createTelemetry(
    serviceName: String,
    config: ExporterConfig
  ): ZIO[Scope, Throwable, OpenTelemetrySdk] = {
    ZIO.acquireRelease(
      ZIO.attempt {
        // Create resource with service information
        val resource = Resource.getDefault()
          .merge(Resource.create(Attributes.builder()
            .put(ResourceAttributes.SERVICE_NAME, serviceName)
            .build()))
         
        // Build tracer provider with configured exporters
        val tracerProviderBuilder = SdkTracerProvider.builder()
          .setResource(resource)
          
        // Add span processors for each enabled exporter
        if (config.enableJaeger) {
          tracerProviderBuilder.addSpanProcessor(
            BatchSpanProcessor.builder(createJaegerExporter(config.jaegerEndpoint)).build()
          )
        }
        
        if (config.enableOtlp) {
          tracerProviderBuilder.addSpanProcessor(
            BatchSpanProcessor.builder(createOtlpTraceExporter(config.otlpEndpoint)).build()
          )
        }
        
        if (config.enableConsole) {
          tracerProviderBuilder.addSpanProcessor(
            SimpleSpanProcessor.create(createConsoleSpanExporter())
          )
        }
        
        val tracerProvider = tracerProviderBuilder.build()
        
        // Build meter provider with configured exporters
        val meterProviderBuilder = SdkMeterProvider.builder()
          .setResource(resource)
        
        if (config.enablePrometheus) {
          meterProviderBuilder.registerMetricReader(createPrometheusReader(config.prometheusPort))
        }
        
        if (config.enableOtlp) {
          meterProviderBuilder.registerMetricReader(
            PeriodicMetricReader.builder(createOtlpMetricExporter(config.otlpEndpoint))
              .setInterval(Duration.ofSeconds(5))
              .build()
          )
        }
        
        val meterProvider = meterProviderBuilder.build()
        
        // Create the OpenTelemetry SDK with configured components
        OpenTelemetrySdk.builder()
          .setTracerProvider(tracerProvider)
          .setMeterProvider(meterProvider)
          .setPropagators(ContextPropagators.create(
            W3CTraceContextPropagator.getInstance()
          ))
          .buildAndRegisterGlobal()
      }
    )(sdk => ZIO.succeed(sdk.close()).orDie)
  }
  
  /**
   * Creates a Jaeger exporter for distributed tracing
   */
  private def createJaegerExporter(endpoint: String): JaegerGrpcSpanExporter = {
    JaegerGrpcSpanExporter.builder()
      .setEndpoint(endpoint)
      .build()
  }
  
  /**
   * Creates a Prometheus metrics reader
   */
  private def createPrometheusReader(port: Int): PeriodicMetricReader = {
    val prometheusExporter = PrometheusHttpServer.builder()
      .setPort(port)
      .build()
      
    PeriodicMetricReader.builder(prometheusExporter)
      .setInterval(Duration.ofSeconds(1))
      .build()
  }
  
  /**
   * Creates an OTLP trace exporter
   */
  private def createOtlpTraceExporter(endpoint: String): OtlpGrpcSpanExporter = {
    OtlpGrpcSpanExporter.builder()
      .setEndpoint(endpoint)
      .build()
  }
  
  /**
   * Creates an OTLP metric exporter
   */
  private def createOtlpMetricExporter(endpoint: String): OtlpGrpcMetricExporter = {
    OtlpGrpcMetricExporter.builder()
      .setEndpoint(endpoint)
      .build()
  }
  
  /**
   * Creates a console logging span exporter for development
   */
  private def createConsoleSpanExporter(): LoggingSpanExporter = {
    LoggingSpanExporter.create()
  }
}