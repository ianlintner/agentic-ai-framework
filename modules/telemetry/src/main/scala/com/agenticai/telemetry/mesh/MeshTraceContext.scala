package com.agenticai.telemetry.mesh

import com.agenticai.mesh.protocol.MessageEnvelope
import com.agenticai.telemetry.core.TelemetryProvider
import io.opentelemetry.api.trace.Span
import io.opentelemetry.context.Context
import io.opentelemetry.context.propagation.{TextMapGetter, TextMapSetter}
import zio.*

import scala.jdk.CollectionConverters.*
import java.util.{HashMap => JHashMap}

/**
 * Provides utilities for propagating distributed trace context across agent boundaries.
 * This enables tracking of end-to-end operations that span multiple agents in the mesh.
 */
object MeshTraceContext {

  private val TRACE_CONTEXT_PREFIX = "trace."

  /**
   * TextMapGetter implementation for extracting trace context from message metadata
   */
  private val messageMetadataGetter = new TextMapGetter[Map[String, String]] {
    override def keys(carrier: Map[String, String]): java.lang.Iterable[String] =
      carrier.keys.filter(_.startsWith(TRACE_CONTEXT_PREFIX)).asJava

    override def get(carrier: Map[String, String], key: String): String =
      carrier.getOrElse(key, null)
  }

  /**
   * TextMapSetter implementation for injecting trace context into message metadata
   */
  private val messageMetadataSetter = new TextMapSetter[JHashMap[String, String]] {
    override def set(carrier: JHashMap[String, String], key: String, value: String): Unit =
      carrier.put(TRACE_CONTEXT_PREFIX + key, value)
  }

  /**
   * Extract trace context from a message envelope
   *
   * @param envelope The message envelope containing trace context in its metadata
   * @return ZIO effect with the extracted Context
   */
  def extractTraceContext(envelope: MessageEnvelope): ZIO[TelemetryProvider, Nothing, Context] = {
    for {
      telemetry <- ZIO.service[TelemetryProvider]
      baseContext <- telemetry.currentContext
      contextMetadata = envelope.metadata.filter(_._1.startsWith(TRACE_CONTEXT_PREFIX))
                                          .map { case (k, v) => (k.substring(TRACE_CONTEXT_PREFIX.length), v) }
      traceContext = io.opentelemetry.api.GlobalOpenTelemetry.getPropagators()
                        .getTextMapPropagator()
                        .extract(baseContext, contextMetadata, messageMetadataGetter)
    } yield traceContext
  }

  /**
   * Inject the current trace context into a message envelope
   *
   * @param envelope The message envelope to inject context into
   * @return ZIO effect with the updated MessageEnvelope containing trace context
   */
  def injectTraceContext(envelope: MessageEnvelope): ZIO[TelemetryProvider, Nothing, MessageEnvelope] = {
    for {
      telemetry <- ZIO.service[TelemetryProvider]
      currentContext <- telemetry.currentContext
      
      // Create a mutable map for the propagator
      metadataMap = new JHashMap[String, String]()
      
      // Inject the current context
      _ = io.opentelemetry.api.GlobalOpenTelemetry.getPropagators()
            .getTextMapPropagator()
            .inject(currentContext, metadataMap, messageMetadataSetter)
            
      // Convert the Java map to Scala and merge with existing metadata
      contextMap = metadataMap.asScala.toMap
      updatedEnvelope = envelope.copy(
        metadata = envelope.metadata ++ contextMap
      )
    } yield updatedEnvelope
  }

  /**
   * Create a new span as a child of the context extracted from a message envelope
   *
   * @param envelope Message envelope containing parent context
   * @param spanName Name of the span to create
   * @param kind The span kind
   * @param attributes Optional attributes to add to the span
   * @return ZIO effect with the created span
   */
  def createChildSpan(
    envelope: MessageEnvelope,
    spanName: String,
    kind: io.opentelemetry.api.trace.SpanKind,
    attributes: Map[String, String] = Map.empty
  ): ZIO[TelemetryProvider, Nothing, Span] = {
    for {
      telemetry <- ZIO.service[TelemetryProvider]
      tracer <- telemetry.tracer("agentic-ai.mesh")
      parentContext <- extractTraceContext(envelope)
      
      // Create span builder
      spanBuilder = tracer.spanBuilder(spanName)
        .setSpanKind(kind)
        .setParent(parentContext)
      
      // Add attributes
      _ = attributes.foreach { case (k, v) => spanBuilder.setAttribute(k, v) }
      
      // Start the span
      span = spanBuilder.startSpan()
    } yield span
  }

  /**
   * Execute an effect with the trace context from a message envelope
   *
   * @param envelope Message envelope containing trace context
   * @param zio The effect to execute with the extracted context
   * @return The effect with trace context applied
   */
  def withMessageContext[R, E, A](envelope: MessageEnvelope)(zio: ZIO[R, E, A]): ZIO[R & TelemetryProvider, E, A] = {
    for {
      telemetry <- ZIO.service[TelemetryProvider]
      extractedContext <- extractTraceContext(envelope)
      result <- telemetry.withContext(extractedContext)(zio)
    } yield result
  }
}