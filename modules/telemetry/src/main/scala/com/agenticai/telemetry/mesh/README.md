# Mesh Telemetry

This module provides telemetry instrumentation for the distributed agent mesh, enabling observability across a network of collaborating agents. The implementation builds on the core telemetry and agent telemetry modules to provide mesh-specific metrics, tracing, and monitoring capabilities.

## Features

### Distributed Tracing

- **Trace Context Propagation**: Automatically propagate trace context across agent boundaries using `MeshTraceContext`, enabling end-to-end tracing for operations that span multiple agents.
- **Trace Context Injection/Extraction**: Utilities for injecting and extracting trace context from message envelopes.
- **Parent-Child Span Relationships**: Create child spans with proper parent context to track the flow of requests through the mesh.

### Mesh Metrics

The module provides comprehensive metrics for mesh operations:

- **Message Throughput**: Track the number of messages sent and received by agents.
- **Communication Latency**: Measure latency between agents in the mesh.
- **Connection Success/Failure Rates**: Monitor connectivity issues between mesh nodes.
- **Message Size**: Track the size of messages flowing through the mesh.
- **Mesh Node Health**: Monitor the health and load of nodes in the mesh.

### Non-Invasive Instrumentation

- **Decorator Pattern**: Uses decorators to wrap existing components like `Protocol`, `AgentDirectory`, and `HttpServer` without modifying their implementation.
- **ZIO Aspects**: Provides ZIO aspects through `MeshTelemetryAspect` for instrumenting code without changing the underlying implementation.

## Components

### MeshTelemetry

The core service that provides telemetry capabilities for mesh operations:

```scala
trait MeshTelemetry {
  def recordMessageSent(sourceNodeId: String, destinationNodeId: String, messageType: String, messageSizeBytes: Long): UIO[Unit]
  def recordMessageReceived(sourceNodeId: String, destinationNodeId: String, messageType: String, messageSizeBytes: Long): UIO[Unit]
  def recordCommunicationLatency(sourceNodeId: String, destinationNodeId: String, messageType: String, latencyMs: Double): UIO[Unit]
  def recordConnectionAttempt(sourceNodeId: String, destinationNodeId: String, success: Boolean): UIO[Unit]
  def recordAgentDiscovery(queryType: String, resultCount: Int, durationMs: Long): UIO[Unit]
  def recordNodeHealth(nodeId: String, agentCount: Int, status: String, loadFactor: Double): UIO[Unit]
}
```

### MeshTelemetryAspect

Provides ZIO aspects for non-invasive instrumentation:

```scala
object MeshTelemetryAspect {
  def instrumentMessageSend[R, E, A](sourceId: String, destinationId: String, messageType: String): ZIOAspect[...] = ...
  def instrumentMessageReceive[R, E, A](sourceId: String, destinationId: String, messageType: String): ZIOAspect[...] = ...
  def instrumentAgentDiscovery[R, E, A](queryType: String): ZIOAspect[...] = ...
  def instrumentConnectionAttempt[R, E, A](sourceId: String, destinationId: String): ZIOAspect[...] = ...
}
```

### MeshTraceContext

Utilities for propagating distributed trace context:

```scala
object MeshTraceContext {
  def extractTraceContext(envelope: MessageEnvelope): ZIO[TelemetryProvider, Nothing, Context] = ...
  def injectTraceContext(envelope: MessageEnvelope): ZIO[TelemetryProvider, Nothing, MessageEnvelope] = ...
  def createChildSpan(envelope: MessageEnvelope, spanName: String, kind: SpanKind, attributes: Map[String, String]): ZIO[...] = ...
  def withMessageContext[R, E, A](envelope: MessageEnvelope)(zio: ZIO[R, E, A]): ZIO[R & TelemetryProvider, E, A] = ...
}
```

### Instrumented Components

- `InstrumentedProtocol`: Decorates a `Protocol` with telemetry instrumentation.
- `InstrumentedAgentDirectory`: Decorates an `AgentDirectory` with telemetry instrumentation.
- `InstrumentedHttpServer`: Decorates an `HttpServer` with telemetry instrumentation.

## Usage Example

```scala
// Create instrumented components
val protocol = InstrumentedProtocol(Protocol.inMemory)
val directory = InstrumentedAgentDirectory(new InMemoryAgentDirectory())
val server = InstrumentedHttpServer(HttpServer(8080, serialization), "node-1")

// The instrumented components are drop-in replacements for the original ones
val program = for {
  ref <- protocol.sendAgent(agent, server.location)
  _ <- directory.registerAgent(ref, metadata)
  result <- protocol.callRemoteAgent(ref, input)
} yield result

// Provide the telemetry layers
program.provide(
  TelemetryProvider.live,
  ZLayer.succeed(telemetryConfig),
  MeshTelemetry.live
)
```

## Integration with Other Systems

The mesh telemetry module can be integrated with:

- **Prometheus**: For metrics collection and alerting
- **Jaeger/Zipkin**: For distributed tracing visualization
- **Grafana**: For dashboards and monitoring

## Implementation Notes

- Spans are created with the appropriate SpanKind (CLIENT/SERVER) to model the correct relationships.
- Trace context is propagated using OpenTelemetry's TextMapPropagator.
- Telemetry information is added to the message metadata without affecting the actual message content.