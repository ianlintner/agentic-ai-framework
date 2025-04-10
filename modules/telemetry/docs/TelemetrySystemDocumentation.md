# Agentic AI Framework Telemetry System

## Architecture Overview

The Agentic AI Framework's telemetry system provides a comprehensive observability solution designed around OpenTelemetry, enabling detailed monitoring, tracing, and metrics collection across distributed agent operations.

### System Layers

![Telemetry Architecture](images/telemetry_architecture.png)

The telemetry system is structured in four primary layers:

1. **Core Telemetry Layer**
   - Foundation for all telemetry functionality
   - Provides basic tracing and metrics capabilities
   - Manages exporters and configuration
   - Maintains referential transparency with ZIO integrations

2. **Agent Telemetry Layer**
   - Tracks agent-specific operations
   - Monitors LLM usage, token counts, and costs
   - Records agent execution times and success rates
   - Provides specialized aspects for agent instrumentation

3. **Mesh Telemetry Layer**
   - Monitors distributed communication
   - Tracks message passing between agents
   - Propagates trace context across agent boundaries
   - Measures network performance and reliability

4. **Exporters Layer**
   - Connects telemetry data to monitoring systems
   - Supports multiple export formats (Prometheus, Jaeger, OTLP, Console)
   - Configurable based on environment (dev, test, production)

### Key Components

- **TelemetryProvider**: Core service that provides access to OpenTelemetry tracers and meters
- **TelemetryAspect**: ZIO aspects that add tracing to any effect
- **AgentTelemetry**: Specialized telemetry for agent operations
- **MeshTelemetry**: Monitoring for distributed agent communication
- **Exporters**: Components that send telemetry data to monitoring systems

### Dataflow

1. Instrumented code generates telemetry data via aspects
2. Data is collected by the TelemetryProvider
3. Exporters send data to monitoring systems
4. Dashboards and UIs visualize the data

## Setup and Configuration

### Prerequisites

- Docker and Docker Compose (for running the monitoring stack)
- Scala and SBT (for running the applications)
- JDK 11+ (for OpenTelemetry compatibility)

### Basic Setup

1. **Add dependencies**:

   ```scala
   libraryDependencies ++= Seq(
     "com.agenticai" %% "telemetry" % "<version>"
   )
   ```

2. **Configure telemetry** in `application.conf`:

   ```hocon
   telemetry {
     service-name = "your-service-name"
     sampling-ratio = 1.0  # 0.0-1.0
     
     exporters {
       prometheus.enabled = true
       prometheus.port = 9464
       
       jaeger.enabled = true
       jaeger.endpoint = "http://localhost:14250"
       
       otlp.enabled = true
       otlp.endpoint = "http://localhost:4317"
       
       console.enabled = true
     }
   }
   ```

3. **Add TelemetryProvider layers** to your application:

   ```scala
   val program = appLogic.provide(
     TelemetryExporterConfig.fromConfig(config),
     TelemetryExporterProvider.localDocker,
     AgentTelemetry.live
   )
   ```

### Running the Monitoring Stack

The module includes a Docker Compose setup for running Prometheus, Jaeger, Grafana, and OpenTelemetry Collector:

```bash
cd modules/telemetry
./run-monitoring.sh
```

This starts:
- Prometheus at http://localhost:9090
- Jaeger at http://localhost:16686
- Grafana at http://localhost:3000 (admin/admin)
- OpenTelemetry Collector at http://localhost:4317 (gRPC) and http://localhost:4318 (HTTP)

### Configuration Options

#### Sampling

For high-volume environments, configure sampling to reduce telemetry data volume:

```hocon
telemetry {
  sampling-ratio = 0.1  # Sample 10% of traces
}
```

#### Environment-Specific Configurations

**Development**:
```scala
val program = appLogic.provide(
  TelemetryExporterConfig.local,
  TelemetryExporterProvider.development
)
```

**Production**:
```scala
val program = appLogic.provide(
  TelemetryExporterConfig.fromConfig(config),
  TelemetryExporterProvider.production
)
```

## Usage Patterns and Best Practices

### Instrumenting Agents

Agents should use the `AgentTelemetryAspect` to instrument their operations:

```scala
import com.agenticai.telemetry.agent.AgentTelemetryAspect

class MyAgent extends Agent[Input, Output]:
  def process(input: Input): ZIO[Any, Throwable, Output] =
    baseLogic(input)
      .inject(AgentTelemetryAspect.instrumentAgent("my-agent", "process"))
```

### Manual Telemetry Recording

For fine-grained control, use the `AgentTelemetry` service directly:

```scala
import com.agenticai.telemetry.agent.AgentTelemetry

def runOperation(input: String): ZIO[AgentTelemetry, Nothing, String] =
  for {
    _ <- AgentTelemetry.recordAgentStart("TextProcessor")
    result = processInput(input)
    _ <- AgentTelemetry.recordAgentOperation("TextProcessor", "process", success = true)
    _ <- AgentTelemetry.recordAgentCompletion("TextProcessor")
  } yield result
```

### Tracing Workflows

Track workflow execution with dedicated aspects:

```scala
import com.agenticai.telemetry.agent.AgentTelemetryAspect

def executeWorkflow(workflow: Workflow, input: String): ZIO[Any, Throwable, String] =
  workflowEngine.execute(workflow, input)
    .inject(AgentTelemetryAspect.instrumentWorkflowStep(workflow.id, "execution"))
```

### Distributed Context Propagation

When passing data between services, ensure trace context is propagated:

```scala
import com.agenticai.telemetry.mesh.MeshTraceContext

// Extract context
val context = MeshTraceContext.extract(message.headers)

// Process with context
processMessage(message.body)
  .inject(TelemetryAspect.withDistributedContext(context))

// Inject context into outgoing message
val newHeaders = MeshTraceContext.inject(message.headers)
sendMessage(message.body, newHeaders)
```

### Best Practices

1. **Instrument at boundaries**: Focus on service entry/exit points
2. **Use aspects for non-invasive instrumentation**: Avoid polluting business logic
3. **Be mindful of high-cardinality attributes**: Limit the number of unique values
4. **Name operations clearly and consistently**: Use hierarchical naming conventions
5. **Track errors and success rates**: Essential for understanding system health
6. **Add business-relevant attributes**: Connect technical metrics to business outcomes

## Performance Considerations

### Telemetry Overhead

The telemetry system adds some overhead to application performance:

- **Memory usage**: ~20-30MB baseline increase
- **CPU overhead**: ~1-3% in typical workloads
- **Latency**: Microseconds per operation (negligible for most use cases)

### Minimizing Impact

1. **Configure appropriate sampling**: Use `sampling-ratio` to reduce data volume
2. **Be selective with instrumentation**: Focus on critical paths and boundaries
3. **Use batch export**: Configure exporters with appropriate batch sizes
4. **Monitor telemetry itself**: Track telemetry system performance

### Export Frequency

Configure export batch size and frequency based on your needs:

```hocon
telemetry {
  exporters {
    prometheus {
      batch-size = 512
      export-interval-ms = 15000
    }
  }
}
```

### Local Development

For local development, use console exporter to reduce resource usage:

```scala
val program = appLogic.provide(
  TelemetryExporterConfig.local,
  TelemetryExporterProvider.development
)
```

## Troubleshooting Guide

### Common Issues

#### No Data in Prometheus/Grafana

**Possible causes**:
- Prometheus not running or not scraping the correct endpoint
- Exporter not configured correctly
- Application not providing the TelemetryProvider layer

**Solutions**:
1. Check Prometheus is running: `curl http://localhost:9090/-/healthy`
2. Verify Prometheus scrape configuration in `prometheus.yml`
3. Ensure application has the correct telemetry layers
4. Check Prometheus targets page for errors: http://localhost:9090/targets

#### Missing Traces in Jaeger

**Possible causes**:
- Jaeger not running or configured correctly
- Sampling ratio set too low
- Trace context not propagated correctly

**Solutions**:
1. Check Jaeger is running: `curl http://localhost:16686/`
2. Increase sampling ratio in configuration
3. Verify trace context propagation in distributed calls
4. Check for errors in Jaeger UI: http://localhost:16686

#### High Cardinality Issues

**Symptoms**:
- Prometheus/Grafana performance degradation
- High memory usage in monitoring stack
- "Too many time series" errors

**Solutions**:
1. Reduce number of unique tag combinations
2. Use fewer dimensions in metrics
3. Aggregate similar metrics
4. Increase memory limits for Prometheus

### Debugging Techniques

#### Enable Debug Logging

```hocon
telemetry {
  log-level = "DEBUG"
}
```

#### Console Exporter

Enable console exporter to see telemetry data in logs:

```hocon
telemetry {
  exporters {
    console.enabled = true
  }
}
```

#### Trace Context Debugging

Add explicit trace context logging:

```scala
for {
  telemetry <- ZIO.service[TelemetryProvider]
  context <- telemetry.currentContext
  _ <- Console.printLine(s"Current trace context: $context")
} yield ()
```

#### Health Endpoints

Add health endpoints to verify telemetry configuration:

```scala
val telemetryHealth = Handler.fromZIO {
  ZIO.service[TelemetryProvider].map { telemetry =>
    Response.text(s"Telemetry configured: $telemetry")
  }
}
```

### Getting Help

1. Check telemetry logs for errors
2. Review OpenTelemetry documentation for your specific exporter
3. Use monitoring system's troubleshooting tools (e.g., Prometheus debug endpoints)
4. Consult project documentation for known issues