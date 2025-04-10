package com.agenticai.telemetry.mesh

import com.agenticai.telemetry.core.TelemetryProvider
import io.opentelemetry.api.metrics.{LongCounter, DoubleHistogram}
import io.opentelemetry.api.trace.SpanKind
import zio.*

import java.util.UUID

/**
 * Mesh-specific telemetry implementation for tracking distributed agent communication.
 * Provides metrics and tracing for mesh operations, inter-agent communication, and node health.
 */
trait MeshTelemetry {
  /**
   * Record a message sent between agents in the mesh
   *
   * @param sourceNodeId ID of the source node
   * @param destinationNodeId ID of the destination node
   * @param messageType Type of message being sent
   * @param messageSizeBytes Size of the message in bytes
   * @return Effect representing the recording operation
   */
  def recordMessageSent(
    sourceNodeId: String,
    destinationNodeId: String,
    messageType: String,
    messageSizeBytes: Long
  ): UIO[Unit]

  /**
   * Record a message received by an agent in the mesh
   *
   * @param sourceNodeId ID of the source node
   * @param destinationNodeId ID of the destination node
   * @param messageType Type of message being received
   * @param messageSizeBytes Size of the message in bytes
   * @return Effect representing the recording operation
   */
  def recordMessageReceived(
    sourceNodeId: String,
    destinationNodeId: String,
    messageType: String,
    messageSizeBytes: Long
  ): UIO[Unit]

  /**
   * Record the latency of communication between agents
   *
   * @param sourceNodeId ID of the source node
   * @param destinationNodeId ID of the destination node
   * @param messageType Type of message
   * @param latencyMs Latency in milliseconds
   * @return Effect representing the recording operation
   */
  def recordCommunicationLatency(
    sourceNodeId: String,
    destinationNodeId: String,
    messageType: String,
    latencyMs: Double
  ): UIO[Unit]

  /**
   * Record a connection attempt between nodes
   *
   * @param sourceNodeId ID of the source node
   * @param destinationNodeId ID of the destination node
   * @param success Whether the connection was successful
   * @return Effect representing the recording operation
   */
  def recordConnectionAttempt(
    sourceNodeId: String,
    destinationNodeId: String,
    success: Boolean
  ): UIO[Unit]

  /**
   * Record agent discovery operation
   *
   * @param queryType Type of query (capability, id, etc)
   * @param resultCount Number of agents found
   * @param durationMs Duration of the query in milliseconds
   * @return Effect representing the recording operation
   */
  def recordAgentDiscovery(
    queryType: String,
    resultCount: Int,
    durationMs: Long
  ): UIO[Unit]

  /**
   * Record node health metrics
   *
   * @param nodeId ID of the node
   * @param agentCount Number of agents hosted on the node
   * @param status Status of the node (ACTIVE, DEGRADED, etc)
   * @param loadFactor Current load factor (0.0-1.0)
   * @return Effect representing the recording operation
   */
  def recordNodeHealth(
    nodeId: String,
    agentCount: Int,
    status: String,
    loadFactor: Double
  ): UIO[Unit]
}

object MeshTelemetry {
  def live: ZLayer[TelemetryProvider, Nothing, MeshTelemetry] = ZLayer.scoped {
    for {
      telemetry <- ZIO.service[TelemetryProvider]
      tracer <- telemetry.tracer("agentic-ai.mesh")
      meter = tracer.meter

      // Message throughput metrics
      messageThroughput <- ZIO.succeed(meter.counterBuilder("mesh.messages")
        .setDescription("Number of messages sent/received through the mesh")
        .build())

      // Message size metrics
      messageSize <- ZIO.succeed(meter.histogramBuilder("mesh.message_size")
        .setDescription("Size of messages in bytes")
        .setUnit("bytes")
        .build())

      // Communication latency metrics
      latency <- ZIO.succeed(meter.histogramBuilder("mesh.latency")
        .setDescription("Latency of inter-agent communication in milliseconds")
        .setUnit("ms")
        .build())

      // Connection success/failure metrics
      connections <- ZIO.succeed(meter.counterBuilder("mesh.connections")
        .setDescription("Number of connection attempts between nodes")
        .build())

      // Agent discovery metrics
      discoveryOperations <- ZIO.succeed(meter.counterBuilder("mesh.discovery.operations")
        .setDescription("Number of agent discovery operations")
        .build())

      discoveryLatency <- ZIO.succeed(meter.histogramBuilder("mesh.discovery.latency")
        .setDescription("Latency of agent discovery operations in milliseconds")
        .setUnit("ms")
        .build())

      // Node health metrics
      nodeHealth <- ZIO.succeed(meter.gaugeBuilder("mesh.node.health")
        .setDescription("Health metrics for mesh nodes")
        .ofDoubles()
        .build())

    } yield new MeshTelemetry {
      override def recordMessageSent(
        sourceNodeId: String,
        destinationNodeId: String,
        messageType: String,
        messageSizeBytes: Long
      ): UIO[Unit] = ZIO.succeed {
        messageThroughput.add(
          1,
          "source_node", sourceNodeId,
          "destination_node", destinationNodeId,
          "message_type", messageType,
          "direction", "outbound"
        )
        
        messageSize.record(
          messageSizeBytes,
          "source_node", sourceNodeId,
          "destination_node", destinationNodeId,
          "message_type", messageType,
          "direction", "outbound"
        )
      }

      override def recordMessageReceived(
        sourceNodeId: String,
        destinationNodeId: String,
        messageType: String,
        messageSizeBytes: Long
      ): UIO[Unit] = ZIO.succeed {
        messageThroughput.add(
          1,
          "source_node", sourceNodeId,
          "destination_node", destinationNodeId,
          "message_type", messageType,
          "direction", "inbound"
        )
        
        messageSize.record(
          messageSizeBytes,
          "source_node", sourceNodeId,
          "destination_node", destinationNodeId,
          "message_type", messageType,
          "direction", "inbound"
        )
      }

      override def recordCommunicationLatency(
        sourceNodeId: String,
        destinationNodeId: String,
        messageType: String,
        latencyMs: Double
      ): UIO[Unit] = ZIO.succeed {
        latency.record(
          latencyMs,
          "source_node", sourceNodeId,
          "destination_node", destinationNodeId,
          "message_type", messageType
        )
      }

      override def recordConnectionAttempt(
        sourceNodeId: String,
        destinationNodeId: String,
        success: Boolean
      ): UIO[Unit] = ZIO.succeed {
        connections.add(
          1,
          "source_node", sourceNodeId,
          "destination_node", destinationNodeId,
          "success", success.toString
        )
      }

      override def recordAgentDiscovery(
        queryType: String,
        resultCount: Int,
        durationMs: Long
      ): UIO[Unit] = ZIO.succeed {
        discoveryOperations.add(
          1,
          "query_type", queryType,
          "result_count", resultCount.toString
        )
        
        discoveryLatency.record(
          durationMs,
          "query_type", queryType
        )
      }

      override def recordNodeHealth(
        nodeId: String,
        agentCount: Int,
        status: String,
        loadFactor: Double
      ): UIO[Unit] = ZIO.succeed {
        nodeHealth.record(
          loadFactor,
          "node_id", nodeId,
          "agent_count", agentCount.toString,
          "status", status
        )
      }
    }
  }

  // Accessor methods
  def recordMessageSent(
    sourceNodeId: String,
    destinationNodeId: String,
    messageType: String,
    messageSizeBytes: Long
  ): URIO[MeshTelemetry, Unit] =
    ZIO.serviceWithZIO(_.recordMessageSent(sourceNodeId, destinationNodeId, messageType, messageSizeBytes))

  def recordMessageReceived(
    sourceNodeId: String,
    destinationNodeId: String,
    messageType: String,
    messageSizeBytes: Long
  ): URIO[MeshTelemetry, Unit] =
    ZIO.serviceWithZIO(_.recordMessageReceived(sourceNodeId, destinationNodeId, messageType, messageSizeBytes))

  def recordCommunicationLatency(
    sourceNodeId: String,
    destinationNodeId: String,
    messageType: String,
    latencyMs: Double
  ): URIO[MeshTelemetry, Unit] =
    ZIO.serviceWithZIO(_.recordCommunicationLatency(sourceNodeId, destinationNodeId, messageType, latencyMs))

  def recordConnectionAttempt(
    sourceNodeId: String,
    destinationNodeId: String,
    success: Boolean
  ): URIO[MeshTelemetry, Unit] =
    ZIO.serviceWithZIO(_.recordConnectionAttempt(sourceNodeId, destinationNodeId, success))

  def recordAgentDiscovery(
    queryType: String,
    resultCount: Int,
    durationMs: Long
  ): URIO[MeshTelemetry, Unit] =
    ZIO.serviceWithZIO(_.recordAgentDiscovery(queryType, resultCount, durationMs))

  def recordNodeHealth(
    nodeId: String,
    agentCount: Int,
    status: String,
    loadFactor: Double
  ): URIO[MeshTelemetry, Unit] =
    ZIO.serviceWithZIO(_.recordNodeHealth(nodeId, agentCount, status, loadFactor))
}