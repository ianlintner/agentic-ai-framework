# Telemetry System Overview

## Introduction

The telemetry system in the ROO framework provides observability into the execution of agents, workflows, and distributed mesh communications. It follows functional programming principles while introducing minimal invasiveness to application code.

This document provides an overview of the telemetry architecture, setup instructions, usage patterns, performance considerations, and troubleshooting guidance.

## Architecture Overview

### Design Philosophy

The telemetry system is designed with the following principles:

1. **Minimal Invasiveness** - Telemetry should be additive and not intrude on core business logic
2. **Functional Purity** - Telemetry operations are implemented as pure functions and effects
3. **Composability** - Telemetry components can be combined and layered
4. **Extensibility** - The system allows for different exporters and collection backends

### Core Components

The telemetry system consists of the following key components:

#### 1. `SimpleTelemetry`

The core utility class that provides functions for recording metrics, traces, and errors. It includes:

- **Metrics Recording** - Capturing numerical measurements with tags
- **Operation Tracing** - Recording the start and end of operations with duration
- **Error Reporting** - Structured error recording with context
- **Effect Tracing** - Wrapping ZIO effects with automatic timing and error reporting

#### 2. Agent Integration

Agents support telemetry recording via the `processWithTelemetry` method that records:

- Start and end of processing
- Processing duration
- Errors that occur during processing

#### 3. Workflow Engine Integration

The workflow engine records:

- Workflow execution metrics
- Step execution timing
- Plan building and execution details
- Error propagation with context

#### 4. Export Pipeline

Telemetry data can be exported to various backends:

- Console logging (default implementation)
- Prometheus metrics
- Jaeger/Zipkin distributed tracing
- Custom exporters

## Setup and Configuration

### Dependencies

Add the following dependencies to your project:

```scala
libraryDependencies ++= Seq(
  "io.opentelemetry" % "opentelemetry-api" % "1.24.0",
  "io.opentelemetry" % "opentelemetry-sdk" % "1.24.0",
  "io.opentelemetry" % "opentelemetry-exporter-otlp" % "1.24.0"
)
```

### Basic Configuration

To set up telemetry in your application:

1. Import the telemetry module:

```scala
import com.agenticai.telemetry.SimpleTelemetry
```

2. Use the telemetry functions directly in your code:

```scala
// Record a metric
SimpleTelemetry.recordMetric("request.count", 1.0, Map("endpoint" -> "/api/data"))

// Trace an operation
SimpleTelemetry.traceEffect("important.operation", Map("user_id" -> "123")) {
  yourOperation()
}
```

### Advanced Configuration

For production use, you'll want to configure exporters:

```scala
// Example configuration for production telemetry
val telemetryConfig = TelemetryConfig(
  serviceName = "my-agent-service",
  exporters = Seq(
    PrometheusExporter("localhost", 9090),
    JaegerExporter("jaeger-collector", 14250)
  )
)

// Initialize telemetry system with configuration
TelemetrySystem.initialize(telemetryConfig)
```

## Usage Patterns and Best Practices

### Instrumenting Agents

When creating agents, extend the base `Agent` trait which provides telemetry integration:

```scala
class MyCustomAgent extends Agent[Request, Response]:
  def process(input: Request): ZIO[Any, Throwable, Response] = 
    // Your processing logic here
    ZIO.succeed(processRequest(input))
    
  // processWithTelemetry is automatically provided
```

To use the agent with telemetry:

```scala
val agent = new MyCustomAgent()
val result = agent.processWithTelemetry("custom-agent-123", request)
```

### Instrumenting Custom Operations

For custom operations, wrap them with telemetry:

```scala
def importantBusinessFunction(data: Data): ZIO[Any, Throwable, Result] =
  SimpleTelemetry.traceEffect("business.function", Map("data_size" -> data.size.toString)) {
    // Your function implementation
    processData(data)
  }
```

### Recording Custom Metrics

For important measurements:

```scala
// Record counters
def incrementRequestCount(endpoint: String): UIO[Unit] =
  SimpleTelemetry.recordMetric("requests.count", 1.0, Map("endpoint" -> endpoint))

// Record timings
def recordProcessingTime(operation: String, durationMs: Long): UIO[Unit] =
  SimpleTelemetry.recordMetric("processing.time", durationMs, Map("operation" -> operation))
```

### Distributed Trace Context Propagation

When communicating between services:

1. Extract the current trace context
2. Send it with your request
3. Restore the context on the receiving service

```scala
// On the sender side
val traceContext = getCurrentTraceContext()
sendRequest(Request(data, traceContext))

// On the receiver side
def handleRequest(request: Request): ZIO[Any, Throwable, Response] =
  withTraceContext(request.traceContext) {
    // Process with the same trace context
    processRequest(request.data)
  }
```

## Performance Considerations

### Overhead

The telemetry system is designed to have minimal overhead:

- Metrics recording: < 1μs per call in most cases
- Tracing: < 10μs overhead per trace
- Context propagation: < 5μs per operation

These numbers are negligible for most operations but can add up in high-throughput scenarios.

### Sampling

For high-volume systems, configure sampling to reduce overhead:

```scala
val samplingConfig = SamplingConfig(
  traceRatio = 0.1,  // Only trace 10% of operations
  errorTraceRatio = 1.0  // Always trace errors
)

TelemetrySystem.configure(samplingConfig)
```

### Batching

Metrics and traces are batched before export to reduce network overhead. Default batch settings:

- Batch size: 512 items
- Flush interval: 5 seconds

These can be configured based on your specific needs.

## Troubleshooting Guide

### Common Issues

#### 1. Missing Telemetry Data

**Symptoms**: Metrics or traces not appearing in dashboards or logs.

**Possible Causes**:
- Exporters not properly configured
- Sampling rate set too low
- Network connectivity issues with collectors

**Resolution**:
- Check exporter configuration
- Temporarily set sampling to 100%
- Verify network connectivity with collectors

#### 2. High Cardinality Warnings

**Symptoms**: Prometheus warnings about high cardinality metrics.

**Possible Causes**:
- Too many unique tag combinations
- User IDs or request IDs used as tags

**Resolution**:
- Limit tag values to enumerated types
- Use bucketing for continuous values
- Avoid using unique identifiers as tags

#### 3. Performance Impact

**Symptoms**: Application performance degradation after enabling telemetry.

**Possible Causes**:
- Too much telemetry data being collected
- Synchronous exporters blocking operations
- Memory pressure from large batches

**Resolution**:
- Increase sampling rate (collect less data)
- Ensure exporters are configured for asynchronous operation
- Reduce batch sizes to lower memory pressure

### Diagnostics

To diagnose telemetry issues, enable debug logging:

```scala
TelemetrySystem.setLogLevel(LogLevel.Debug)
```

This will output detailed information about telemetry operations, including:
- Metric recording
- Trace creation and completion
- Export operations
- Sampling decisions

## Integration with Monitoring Stack

The telemetry system integrates with standard monitoring tools:

- **Prometheus**: For metrics collection and alerting
- **Grafana**: For metrics visualization and dashboards
- **Jaeger/Zipkin**: For distributed tracing visualization
- **OpenTelemetry Collector**: For data processing and forwarding

See the accompanying `run-monitoring.sh` script for setting up a complete monitoring stack.

## Conclusion

The ROO telemetry system provides comprehensive observability with minimal code changes. By following the patterns and practices outlined in this document, you can gain deep insights into your agent and workflow operations while maintaining the functional programming principles that make ROO powerful and maintainable.