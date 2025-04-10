package com.agenticai.telemetry.mesh

import com.agenticai.mesh.protocol.{AgentLocation, Serialization}
import com.agenticai.mesh.server.HttpServer
import com.agenticai.telemetry.core.TelemetryProvider
import zio.*

/**
 * Decorates an HttpServer implementation with telemetry instrumentation.
 * This provides visibility into server operations without modifying
 * the underlying implementation.
 */
class InstrumentedHttpServer(
  underlying: HttpServer,
  nodeId: String
) extends HttpServer {

  /**
   * Start the server with telemetry instrumentation
   */
  override def start: ZIO[MeshTelemetry & TelemetryProvider, Throwable, Fiber[Throwable, Nothing]] = {
    for {
      // Create a startup span
      telemetry <- ZIO.service[TelemetryProvider]
      tracer <- telemetry.tracer("agentic-ai.mesh.server")
      span <- ZIO.succeed(tracer.spanBuilder("mesh.server.start")
                .setAttribute("node.id", nodeId)
                .setAttribute("port", location.port.toString)
                .startSpan())
      
      // Start the server with the span
      fiber <- ZIO.succeedWith { span.makeCurrent() }
        .bracket(_ => ZIO.succeed(span.end()))(
          _ => underlying.start
        )
      
      // Record node health as active
      _ <- MeshTelemetry.recordNodeHealth(
        nodeId,
        0, // No agents initially
        "ACTIVE",
        0.0 // Initial load is 0
      )
    } yield fiber
  }

  /**
   * Get the server's local location
   */
  override def location: AgentLocation = 
    underlying.location
}

object InstrumentedHttpServer {
  /**
   * Create an instrumented version of an HTTP server
   * 
   * @param server The underlying server to instrument
   * @param nodeId The ID to use for this node in telemetry
   * @return A new HTTP server with telemetry instrumentation
   */
  def apply(server: HttpServer, nodeId: String): InstrumentedHttpServer =
    new InstrumentedHttpServer(server, nodeId)
    
  /**
   * Create a new instrumented HTTP server
   * 
   * @param port The port to listen on
   * @param serialization The serialization to use
   * @param nodeId The ID to use for this node in telemetry
   * @return A new HTTP server with telemetry instrumentation
   */
  def apply(port: Int, serialization: Serialization, nodeId: String = s"node-$port"): InstrumentedHttpServer = {
    val baseServer = HttpServer(port, serialization)
    new InstrumentedHttpServer(baseServer, nodeId)
  }
}