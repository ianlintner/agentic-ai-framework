# ROO Telemetry Module

The telemetry module provides observability capabilities for the ROO framework, allowing agents, workflows, and mesh communication to be monitored and analyzed.

## Features

- **Minimal API**: Simple, functional API for recording metrics, traces, and errors
- **Agent Integration**: Automatic telemetry for agent processing
- **Workflow Integration**: Detailed workflow execution monitoring
- **Distributed Tracing**: Trace context propagation across agent boundaries
- **Performance Metrics**: Track operation durations and resource usage
- **Error Monitoring**: Structured error recording with context
- **Functional Design**: Pure implementation that respects ZIO effects

## Quick Start

### Instrumenting an Agent

Agents automatically support telemetry through the base `Agent` trait:

```scala
val agent = new MyCustomAgent()
val result = agent.processWithTelemetry("agent-id", input)
```

### Instrumenting Custom Operations

For custom operations:

```scala
import com.agenticai.telemetry.SimpleTelemetry

val result = SimpleTelemetry.traceEffect("my.operation", Map("context" -> "value")) {
  myOperation()
}
```

### Recording Metrics

For important measurements:

```scala
// Record a metric
SimpleTelemetry.recordMetric(
  "processing.time", 
  durationMs.toDouble, 
  Map("operation" -> "document-processing")
)
```

## Running the Monitoring Stack

A Docker Compose configuration is provided to run a complete monitoring stack:

```bash
# Navigate to the telemetry module
cd modules/telemetry

# Start the monitoring stack
docker-compose up -d
```

This starts:
- **Prometheus**: Metrics collection (port 9090)
- **Jaeger**: Distributed tracing (port 16686)
- **Grafana**: Dashboards (port 3000)
- **OpenTelemetry Collector**: Telemetry processing (ports 4317, 4318, 8888)

## Running the Workflow Demo with Telemetry

A script is provided to run the workflow demo with telemetry enabled:

```bash
# Make the script executable
chmod +x modules/workflow-demo/run-workflow-demo-with-telemetry.sh

# Run the demo with telemetry
./modules/workflow-demo/run-workflow-demo-with-telemetry.sh
```

This will:
1. Start the monitoring stack
2. Run the workflow demo with telemetry enabled
3. Provide URLs to access the dashboards

## Documentation

Detailed documentation is available in the `docs` directory:

- [Telemetry System Overview](./docs/TelemetrySystemOverview.md): Architecture, components, and concepts
- [Telemetry Integration Guide](./docs/TelemetryIntegrationGuide.md): Practical integration steps and examples

## Configuration

Configure telemetry through environment variables:

- `ENABLE_TELEMETRY`: Set to "true" to enable telemetry (default: false)
- `OTEL_SERVICE_NAME`: Service name for telemetry (default: "roo-service")
- `OTEL_EXPORTER_OTLP_ENDPOINT`: Collector endpoint (default: "http://localhost:4317")
- `OTEL_TRACE_SAMPLING_RATIO`: Sampling ratio from 0.0 to 1.0 (default: 1.0)

## Implementation Details

The telemetry module is built on OpenTelemetry standards and follows functional programming principles:

- Pure functional implementation with ZIO for effectful operations
- Minimal invasiveness to application code
- Composable design that respects referential transparency
- Support for distributed trace context propagation
- Extensible export pipeline for different backends

## License

Same as the ROO framework license.