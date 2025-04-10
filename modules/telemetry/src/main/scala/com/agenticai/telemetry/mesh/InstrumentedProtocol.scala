package com.agenticai.telemetry.mesh

import com.agenticai.core.agent.Agent
import com.agenticai.mesh.protocol.{AgentLocation, MessageEnvelope, Protocol, RemoteAgentRef, Serialization}
import com.agenticai.telemetry.core.TelemetryProvider
import zio.*

import java.util.UUID

/**
 * Decorates a Protocol implementation with telemetry instrumentation.
 * This enables tracking of all mesh communication while leaving the underlying 
 * protocol implementation untouched.
 */
class InstrumentedProtocol(
  underlying: Protocol
) extends Protocol {

  /**
   * Send an agent to a remote location with telemetry instrumentation
   */
  override def sendAgent[I, O](
    agent: Agent[I, O],
    destination: AgentLocation
  ): ZIO[MeshTelemetry & TelemetryProvider, Throwable, RemoteAgentRef[I, O]] = {
    val sourceId = "local-" + UUID.randomUUID().toString.take(8)
    val destId = destination.toString

    underlying.sendAgent(agent, destination)
      .pipe(MeshTelemetryAspect.instrumentMessageSend(
        sourceId, 
        destId, 
        "AGENT_DEPLOY"
      ))
      .tap(ref => 
        MeshTelemetry.recordNodeHealth(
          destId,
          1, // Single agent deployed
          "ACTIVE",
          0.0 // Assume initial load is low
        )
      )
  }

  /**
   * Call a remote agent with telemetry instrumentation
   */
  override def callRemoteAgent[I, O](
    ref: RemoteAgentRef[I, O],
    input: I
  ): ZIO[MeshTelemetry & TelemetryProvider, Throwable, O] = {
    val sourceId = "local-" + UUID.randomUUID().toString.take(8)
    val destId = ref.location.toString

    underlying.callRemoteAgent(ref, input)
      .pipe(MeshTelemetryAspect.instrumentMessageSend(
        sourceId,
        destId,
        "AGENT_CALL"
      ))
  }

  /**
   * Get a remote agent with telemetry instrumentation
   */
  override def getRemoteAgent[I, O](
    ref: RemoteAgentRef[I, O]
  ): ZIO[MeshTelemetry & TelemetryProvider, Throwable, Option[Agent[I, O]]] = {
    val sourceId = "local-" + UUID.randomUUID().toString.take(8)
    val destId = ref.location.toString

    underlying.getRemoteAgent(ref)
      .pipe(MeshTelemetryAspect.instrumentMessageSend(
        sourceId,
        destId,
        "AGENT_GET"
      ))
  }

  /**
   * Send and receive a message with telemetry instrumentation
   */
  override def sendAndReceive(
    location: AgentLocation,
    message: MessageEnvelope
  ): ZIO[MeshTelemetry & TelemetryProvider, Throwable, MessageEnvelope] = {
    val sourceId = "local-" + UUID.randomUUID().toString.take(8)
    val destId = location.toString
    
    for {
      // Inject trace context into message
      telemetry <- ZIO.service[TelemetryProvider]
      messageWithContext <- MeshTraceContext.injectTraceContext(message)
      
      // Measure message size
      messageSize = messageWithContext.payload.length.toLong
      
      // Execute call with telemetry
      startTime <- Clock.currentTime(TimeUnit.MILLISECONDS)
      response <- underlying.sendAndReceive(location, messageWithContext)
        .pipe(MeshTelemetryAspect.instrumentMessageSend(
          sourceId,
          destId,
          message.messageType
        ))
      endTime <- Clock.currentTime(TimeUnit.MILLISECONDS)
      
      // Record latency
      _ <- MeshTelemetry.recordCommunicationLatency(
        sourceId,
        destId,
        message.messageType,
        (endTime - startTime).toDouble
      )
    } yield response
  }

  /**
   * Send a message without response with telemetry instrumentation
   */
  override def send(
    location: AgentLocation,
    message: MessageEnvelope
  ): ZIO[MeshTelemetry & TelemetryProvider, Throwable, Unit] = {
    val sourceId = "local-" + UUID.randomUUID().toString.take(8)
    val destId = location.toString
    
    for {
      // Inject trace context into message
      telemetry <- ZIO.service[TelemetryProvider]
      messageWithContext <- MeshTraceContext.injectTraceContext(message)
      
      // Execute call with telemetry
      _ <- underlying.send(location, messageWithContext)
        .pipe(MeshTelemetryAspect.instrumentMessageSend(
          sourceId,
          destId,
          message.messageType
        ))
    } yield ()
  }
}

object InstrumentedProtocol {
  /**
   * Create an instrumented version of a protocol
   * 
   * @param protocol The underlying protocol to instrument
   * @return A new protocol instance with telemetry instrumentation
   */
  def apply(protocol: Protocol): InstrumentedProtocol =
    new InstrumentedProtocol(protocol)
    
  /**
   * Create an instrumented in-memory protocol suitable for testing
   * 
   * @return An instrumented protocol
   */
  def inMemory: InstrumentedProtocol = {
    val serialization = Serialization.test
    val base = Protocol.inMemory
    new InstrumentedProtocol(base)
  }
}