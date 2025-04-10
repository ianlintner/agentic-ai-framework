package com.agenticai.telemetry.mesh

import com.agenticai.telemetry.core.{TelemetryAspect, TelemetryConfig, TelemetryProvider}
import io.opentelemetry.api.trace.{SpanKind, StatusCode}
import io.opentelemetry.context.propagation.{TextMapGetter, TextMapSetter}
import zio.*

import scala.jdk.CollectionConverters.*
import java.util.UUID

/**
 * Provides ZIO aspects for instrumenting mesh operations non-invasively.
 * These aspects can be applied to existing mesh operations to track telemetry
 * without modifying the underlying implementation.
 */
object MeshTelemetryAspect {

  /**
   * Instrument a mesh message send operation
   *
   * @param sourceId Source node/agent ID
   * @param destinationId Destination node/agent ID
   * @param messageType Type of message being sent
   * @return An aspect that wraps the original effect with telemetry
   */
  def instrumentMessageSend[R, E, A](
    sourceId: String,
    destinationId: String,
    messageType: String
  ): ZIOAspect[R, E, A, R & MeshTelemetry & TelemetryProvider, E, A] =
    new ZIOAspect[R, E, A, R & MeshTelemetry & TelemetryProvider, E, A] {
      override def apply[R1 <: R, E1 >: E](zio: ZIO[R1, E1, A])(implicit trace: Trace): ZIO[R1 & MeshTelemetry & TelemetryProvider, E1, A] = {
        for {
          // Start timing
          startTime <- Clock.currentTime(TimeUnit.MILLISECONDS)
          
          // Create span
          span <- TelemetryAspect.createSpan(
            s"mesh.send.$messageType",
            SpanKind.CLIENT,
            Map(
              "source.id" -> sourceId,
              "destination.id" -> destinationId,
              "message.type" -> messageType
            )
          )
          
          // Execute the original effect with the span
          result <- TelemetryAspect.withSpan(span)(zio).catchAll { error =>
            span.setStatus(StatusCode.ERROR, error.toString)
            ZIO.fail(error)
          }
          
          // Get end time for latency calculation
          endTime <- Clock.currentTime(TimeUnit.MILLISECONDS)
          
          // Record metrics
          _ <- MeshTelemetry.recordMessageSent(
            sourceId, 
            destinationId, 
            messageType,
            // If result has a size attribute, use it, otherwise default to 0
            result match {
              case sized: { def size: Long } => sized.size
              case bytes: Array[Byte] => bytes.length.toLong
              case str: String => str.getBytes.length.toLong
              case _ => 0L
            }
          )
          
          // Record communication latency
          _ <- MeshTelemetry.recordCommunicationLatency(
            sourceId, 
            destinationId, 
            messageType, 
            (endTime - startTime).toDouble
          )
        } yield result
      }
    }

  /**
   * Instrument a mesh message receive operation
   *
   * @param sourceId Source node/agent ID
   * @param destinationId Destination node/agent ID
   * @param messageType Type of message being received
   * @return An aspect that wraps the original effect with telemetry
   */
  def instrumentMessageReceive[R, E, A](
    sourceId: String,
    destinationId: String,
    messageType: String
  ): ZIOAspect[R, E, A, R & MeshTelemetry & TelemetryProvider, E, A] =
    new ZIOAspect[R, E, A, R & MeshTelemetry & TelemetryProvider, E, A] {
      override def apply[R1 <: R, E1 >: E](zio: ZIO[R1, E1, A])(implicit trace: Trace): ZIO[R1 & MeshTelemetry & TelemetryProvider, E1, A] = {
        for {
          // Start timing
          startTime <- Clock.currentTime(TimeUnit.MILLISECONDS)
          
          // Create span
          span <- TelemetryAspect.createSpan(
            s"mesh.receive.$messageType",
            SpanKind.SERVER,
            Map(
              "source.id" -> sourceId,
              "destination.id" -> destinationId,
              "message.type" -> messageType
            )
          )
          
          // Execute the original effect with the span
          result <- TelemetryAspect.withSpan(span)(zio).catchAll { error =>
            span.setStatus(StatusCode.ERROR, error.toString)
            ZIO.fail(error)
          }
          
          // Get end time for latency calculation
          endTime <- Clock.currentTime(TimeUnit.MILLISECONDS)
          
          // Record metrics
          _ <- MeshTelemetry.recordMessageReceived(
            sourceId, 
            destinationId, 
            messageType,
            // If result has a size attribute, use it, otherwise default to 0
            result match {
              case sized: { def size: Long } => sized.size
              case bytes: Array[Byte] => bytes.length.toLong
              case str: String => str.getBytes.length.toLong
              case _ => 0L
            }
          )
        } yield result
      }
    }

  /**
   * Instrument an agent discovery operation
   *
   * @param queryType Type of query (capability, id, etc)
   * @return An aspect that wraps the original effect with telemetry
   */
  def instrumentAgentDiscovery[R, E, A](
    queryType: String
  ): ZIOAspect[R, E, A, R & MeshTelemetry & TelemetryProvider, E, A] =
    new ZIOAspect[R, E, A, R & MeshTelemetry & TelemetryProvider, E, A] {
      override def apply[R1 <: R, E1 >: E](zio: ZIO[R1, E1, A])(implicit trace: Trace): ZIO[R1 & MeshTelemetry & TelemetryProvider, E1, A] = {
        for {
          // Start timing
          startTime <- Clock.currentTime(TimeUnit.MILLISECONDS)
          
          // Create span
          span <- TelemetryAspect.createSpan(
            "mesh.discovery",
            SpanKind.INTERNAL,
            Map(
              "query.type" -> queryType
            )
          )
          
          // Execute the original effect with the span
          result <- TelemetryAspect.withSpan(span)(zio).catchAll { error =>
            span.setStatus(StatusCode.ERROR, error.toString)
            ZIO.fail(error)
          }
          
          // Get end time for latency calculation
          endTime <- Clock.currentTime(TimeUnit.MILLISECONDS)
          
          // Record metrics (if result is a collection, count the size)
          _ <- MeshTelemetry.recordAgentDiscovery(
            queryType,
            result match {
              case seq: Seq[_] => seq.size
              case list: List[_] => list.size
              case set: Set[_] => set.size
              case map: Map[_, _] => map.size
              case array: Array[_] => array.length
              case opt: Option[_] => if (opt.isDefined) 1 else 0
              case _ => 1
            },
            endTime - startTime
          )
        } yield result
      }
    }
    
  /**
   * Instrument a connection attempt between nodes
   *
   * @param sourceId Source node/agent ID
   * @param destinationId Destination node/agent ID
   * @return An aspect that wraps the original effect with telemetry
   */
  def instrumentConnectionAttempt[R, E, A](
    sourceId: String,
    destinationId: String
  ): ZIOAspect[R, E, A, R & MeshTelemetry & TelemetryProvider, E, A] =
    new ZIOAspect[R, E, A, R & MeshTelemetry & TelemetryProvider, E, A] {
      override def apply[R1 <: R, E1 >: E](zio: ZIO[R1, E1, A])(implicit trace: Trace): ZIO[R1 & MeshTelemetry & TelemetryProvider, E1, A] = {
        for {
          // Create span
          span <- TelemetryAspect.createSpan(
            "mesh.connection",
            SpanKind.CLIENT,
            Map(
              "source.id" -> sourceId,
              "destination.id" -> destinationId
            )
          )
          
          // Execute the original effect with the span
          result <- TelemetryAspect.withSpan(span)(zio).foldZIO(
            error => {
              span.setStatus(StatusCode.ERROR, error.toString)
              MeshTelemetry.recordConnectionAttempt(sourceId, destinationId, false) *>
              ZIO.fail(error)
            },
            success => {
              MeshTelemetry.recordConnectionAttempt(sourceId, destinationId, true) *>
              ZIO.succeed(success)
            }
          )
        } yield result
      }
    }
}