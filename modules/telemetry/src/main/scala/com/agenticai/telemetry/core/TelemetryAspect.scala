package com.agenticai.telemetry.core

import io.opentelemetry.api.trace.{Span, SpanKind, StatusCode}
import io.opentelemetry.context.Context
import zio.*

/**
 * Provides ZIO aspects for adding telemetry instrumentation to effects.
 * Maintains referential transparency while adding tracing capabilities.
 */
object TelemetryAspect {
  
  /**
   * Creates a traced version of an effect with the given operation name.
   * Automatically manages span lifecycle and error propagation.
   *
   * @param operationName Name of the operation for the trace
   * @param attributes Optional set of attributes to add to the span
   * @return An aspect that can be applied to any ZIO effect
   */
  def traced(
    operationName: String,
    attributes: Map[String, String] = Map.empty
  ): ZIOAspect[Nothing, TelemetryProvider, Nothing, Any, Nothing, Any] =
    new ZIOAspect[Nothing, TelemetryProvider, Nothing, Any, Nothing, Any] {
      override def apply[R <: TelemetryProvider, E, A](
        zio: ZIO[R, E, A]
      )(implicit trace: Trace): ZIO[R, E, A] = {
        for {
          telemetry <- ZIO.service[TelemetryProvider]
          tracer <- telemetry.tracer("agentic-ai")
          parentContext <- telemetry.currentContext
          result <- ZIO.suspendSucceed {
            val spanBuilder = tracer.spanBuilder(operationName)
              .setParent(parentContext)
              .setSpanKind(SpanKind.INTERNAL)
            
            attributes.foreach { case (k, v) => 
              spanBuilder.setAttribute(k, v)
            }
            
            val span = spanBuilder.startSpan()
            val scope = span.makeCurrent()
            
            zio.foldCauseZIO(
              cause => {
                val error = cause.failureOrCause match {
                  case Left(e) => e
                  case Right(c) => c.squash
                }
                ZIO.succeed {
                  span.setStatus(StatusCode.ERROR, error.toString)
                  span.recordException(error match {
                    case t: Throwable => t
                    case e => new Exception(e.toString)
                  })
                } *> ZIO.fail(error)
              },
              success => ZIO.succeed(success)
            ).ensuring(
              ZIO.succeed {
                scope.close()
                span.end()
              }
            )
          }
        } yield result
      }
    }

  /**
   * Adds distributed tracing context propagation to an effect.
   * Useful for passing trace context across service boundaries.
   */
  def withDistributedContext[R, E, A](
    context: Context
  ): ZIOAspect[Nothing, TelemetryProvider, Nothing, R, E, A] =
    new ZIOAspect[Nothing, TelemetryProvider, Nothing, R, E, A] {
      override def apply[R1 <: TelemetryProvider, E1 >: E, A1 >: A](
        zio: ZIO[R1, E1, A1]
      )(implicit trace: Trace): ZIO[R1, E1, A1] = {
        ZIO.service[TelemetryProvider].flatMap(_.withContext(context)(zio))
      }
    }
}