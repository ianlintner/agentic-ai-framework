package com.agenticai.telemetry

import zio._
import java.util.concurrent.TimeUnit

/**
 * A simplified telemetry layer for demonstration purposes.
 * In a real implementation, this would connect to OpenTelemetry.
 */
object SimpleTelemetry {
  
  /**
   * Records metrics for tracing and monitoring.
   * 
   * @param metricName The full metric name (e.g., "workflow.execution.time")
   * @param value The metric value
   * @param tags Additional context tags
   * @return An effect that records the metric
   */
  def recordMetric(
    metricName: String,
    value: Double,
    tags: Map[String, String] = Map.empty
  ): UIO[Unit] = {
    ZIO.logInfo(
      s"METRIC: $metricName = $value ${tags.map { case (k, v) => s"$k=$v" }.mkString(", ")}"
    )
  }
  
  /**
   * Records the start of an operation.
   * 
   * @param operationName The name of the operation
   * @param tags Additional context tags
   * @return An effect that records the start
   */
  def recordStart(
    operationName: String,
    tags: Map[String, String] = Map.empty
  ): UIO[Unit] = {
    ZIO.logInfo(s"START: $operationName ${tags.map { case (k, v) => s"$k=$v" }.mkString(", ")}")
  }
  
  /**
   * Records the end of an operation.
   * 
   * @param operationName The name of the operation
   * @param durationMs The duration in milliseconds
   * @param tags Additional context tags
   * @return An effect that records the end
   */
  def recordEnd(
    operationName: String,
    durationMs: Long,
    tags: Map[String, String] = Map.empty
  ): UIO[Unit] = {
    ZIO.logInfo(
      s"END: $operationName duration=${durationMs}ms ${tags.map { case (k, v) => s"$k=$v" }.mkString(", ")}"
    )
  }
  
  /**
   * Records an error.
   * 
   * @param operationName The name of the operation
   * @param errorType The type of error
   * @param message The error message
   * @param tags Additional context tags
   * @return An effect that records the error
   */
  def recordError(
    operationName: String,
    errorType: String,
    message: String,
    tags: Map[String, String] = Map.empty
  ): UIO[Unit] = {
    ZIO.logError(
      s"ERROR: $operationName type=$errorType message=$message ${tags.map { case (k, v) => s"$k=$v" }.mkString(", ")}"
    )
  }
  
  /**
   * Wraps an effect with telemetry recording.
   * 
   * @param operationName The name of the operation
   * @param tags Additional context tags
   * @param zio The effect to trace
   * @return The traced effect
   */
  def traceEffect[R, E, A](
    operationName: String,
    tags: Map[String, String] = Map.empty
  )(zio: ZIO[R, E, A]): ZIO[R, E, A] = {
    for {
      startTime <- Clock.currentTime(TimeUnit.MILLISECONDS)
      _ <- recordStart(operationName, tags)
      
      result <- zio.catchAll { error =>
        val errorMessage = Option(error).map(_.toString).getOrElse("Unknown error")
        val errorType = Option(error).map(_.getClass.getSimpleName).getOrElse("Unknown")
        recordError(operationName, errorType, errorMessage, tags).as(ZIO.fail(error)).flatten
      }
      
      endTime <- Clock.currentTime(TimeUnit.MILLISECONDS)
      duration = endTime - startTime
      _ <- recordEnd(operationName, duration, tags)
    } yield result
  }
}