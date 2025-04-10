package com.agenticai.telemetry

import zio._

/**
 * Stand-in implementation to use until full telemetry module is properly compiled
 */
object ZIOTelemetryAspect {
  /**
   * Adds tracing to an operation
   */
  def traced[R, E, A](
    operationName: String, 
    attributes: Map[String, String] = Map.empty
  ): ZIOAspect[Nothing, R, Nothing, R, E, A] =
    new ZIOAspect[Nothing, R, Nothing, R, E, A] {
      override def apply[R1 <: R, E1 >: E, A1 >: A](zio: ZIO[R1, E1, A1])(implicit trace: Trace): ZIO[R1, E1, A1] = {
        // Just a passthrough for now until we fully implement telemetry
        zio
      }
    }
}

/**
 * Stand-in implementation for MetricsSupport
 */
trait MetricsSupport {
  def recordMetric(name: String, value: Double, tags: Map[String, String] = Map.empty): UIO[Unit]
}

object MetricsSupport {
  def recordMetric(name: String, value: Double, tags: Map[String, String] = Map.empty): URIO[MetricsSupport, Unit] =
    ZIO.serviceWithZIO[MetricsSupport](_.recordMetric(name, value, tags))
    
  val live: ULayer[MetricsSupport] = ZLayer.succeed(
    new MetricsSupport {
      def recordMetric(name: String, value: Double, tags: Map[String, String]): UIO[Unit] =
        ZIO.logInfo(s"METRIC: $name = $value ${tags.map { case (k, v) => s"$k=$v" }.mkString(", ")}")
    }
  )
}