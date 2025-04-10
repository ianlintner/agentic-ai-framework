# Telemetry Integration Guide

This guide provides practical steps and examples for integrating telemetry into ROO-based applications. It covers common integration patterns, configuration options, and troubleshooting tips.

## Quick Start

### 1. Basic Integration

To add telemetry to an existing agent or workflow:

```scala
import com.agenticai.telemetry.SimpleTelemetry

// For a standalone operation
val result = SimpleTelemetry.traceEffect("my.operation", Map("context" -> "value")) {
  myOperation()
}

// For an agent
val myAgent = new MyCustomAgent()
val result = myAgent.processWithTelemetry("agent-id", request)
```

### 2. Running with Telemetry

Use the provided script to run your application with the telemetry monitoring stack:

```bash
# Make the script executable
chmod +x modules/workflow-demo/run-workflow-demo-with-telemetry.sh

# Run the workflow demo with telemetry
./modules/workflow-demo/run-workflow-demo-with-telemetry.sh
```

This will:
- Start Prometheus, Grafana, and Jaeger
- Run the application with telemetry enabled
- Provide URLs to access dashboards

## Detailed Integration Guide

### Instrumenting Agents

The base `Agent` trait already includes telemetry support. When implementing an agent:

```scala
class MyDataProcessingAgent extends Agent[RawData, ProcessedData]:
  def process(input: RawData): ZIO[Any, Throwable, ProcessedData] =
    // Your implementation logic here
    ZIO.succeed(transform(input))
```

To use the agent with telemetry:

```scala
val agent = new MyDataProcessingAgent()

// This will automatically add tracing, timing, and error reporting
val result = agent.processWithTelemetry("data-processor-1", rawData)
```

### Instrumenting Workflows

The `WorkflowEngine` is already instrumented. You can use it directly:

```scala
val engine = new WorkflowEngine(textTransformer, textSplitter, summarizer, buildAgent)
val result = engine.executeWorkflow(myWorkflow, inputText)
```

### Adding Custom Metrics

For application-specific metrics:

```scala
// Count something important
def trackDocumentProcessed(docType: String, sizeBytes: Long): UIO[Unit] =
  SimpleTelemetry.recordMetric(
    "documents.processed", 
    1.0, 
    Map("doc_type" -> docType, "size_range" -> sizeCategory(sizeBytes))
  )

// Measure important durations
def recordProcessingTime(operation: String, durationMs: Long): UIO[Unit] =
  SimpleTelemetry.recordMetric(
    "processing.duration_ms", 
    durationMs.toDouble,
    Map("operation" -> operation)
  )
```

### Instrumenting Mesh Communication

For distributed mesh communication:

```scala
// When sending a message
def sendMessage(target: AgentId, message: Message): ZIO[Any, Throwable, Unit] =
  SimpleTelemetry.traceEffect("mesh.send", Map("target" -> target.toString)) {
    // Add trace context to message metadata
    val messageWithContext = addTraceContext(message)
    meshService.send(target, messageWithContext)
  }

// When receiving a message
def handleMessage(message: Message): ZIO[Any, Throwable, Response] =
  // Extract trace context from message metadata
  val traceContext = extractTraceContext(message)
  
  SimpleTelemetry.traceEffect("mesh.receive", Map("source" -> message.source.toString)) {
    // Process the message
    processMessage(message)
  }
```

## Configuration Options

### Environment Variables

Configure telemetry behavior with environment variables:

- `ENABLE_TELEMETRY`: Set to "true" to enable telemetry (default: false)
- `OTEL_SERVICE_NAME`: The service name used in telemetry (default: "roo-service")
- `OTEL_EXPORTER_OTLP_ENDPOINT`: The OpenTelemetry collector endpoint (default: "http://localhost:4317")
- `OTEL_TRACE_SAMPLING_RATIO`: Sampling ratio from 0.0 to 1.0 (default: 1.0)
- `OTEL_LOG_LEVEL`: Logging level for telemetry (default: "info")

Example:

```bash
export ENABLE_TELEMETRY=true
export OTEL_SERVICE_NAME="document-processor"
export OTEL_TRACE_SAMPLING_RATIO=0.1
sbt run
```

### Programmatic Configuration

For more control, configure programmatically:

```scala
import com.agenticai.telemetry.config.TelemetryConfig

val config = TelemetryConfig(
  enabled = true,
  serviceName = "document-processor",
  sampling = SamplingConfig(
    traceRatio = 0.1,
    errorTraceRatio = 1.0
  ),
  exporters = Seq(
    OtlpExporter("http://collector:4317")
  )
)

// Initialize with config
TelemetrySystem.initialize(config)
```

## Observing Telemetry Data

### Logs

The simplest form of telemetry observation is logs. When telemetry is enabled, you'll see log entries like:

```
INFO  c.a.t.SimpleTelemetry - START: workflow.execute workflow_id=wf-123, workflow_name=text-processing, node_count=4
INFO  c.a.t.SimpleTelemetry - METRIC: workflow.plan.steps = 4.0 workflow_id=wf-123, workflow_name=text-processing, node_count=4
INFO  c.a.t.SimpleTelemetry - END: workflow.execute duration=1253ms workflow_id=wf-123, workflow_name=text-processing, node_count=4
```

### Grafana Dashboards

Access Grafana at http://localhost:3000 to view:

1. **ROO Overview Dashboard**: High-level metrics about all services
2. **Workflow Performance Dashboard**: Details about workflow execution
3. **Agent Performance Dashboard**: Metrics about agent processing
4. **Mesh Communication Dashboard**: Inter-agent communication patterns

### Jaeger Traces

Access Jaeger at http://localhost:16686 to view:

1. Select your service from the dropdown
2. View traces with unusual durations or errors
3. Drill down into spans to see detailed timing

## Common Issues and Solutions

### Missing Telemetry Data

**Issue**: Telemetry is enabled but no data appears in dashboards.

**Solutions**:
1. Check that `ENABLE_TELEMETRY=true` is set
2. Verify the OTLP endpoint is accessible
3. Confirm the monitoring stack is running (`docker ps`)
4. Check for errors in the application logs

### High Cardinality

**Issue**: "High cardinality" warnings in Prometheus.

**Solutions**:
1. Limit tag values to enumerated types
2. Use buckets for continuous values
3. Avoid using unique identifiers as tags

### Performance Impact

**Issue**: Application slows down with telemetry enabled.

**Solutions**:
1. Reduce sampling rate (e.g., `OTEL_TRACE_SAMPLING_RATIO=0.1`)
2. Limit the number of metrics recorded
3. Use async exporters (default)
4. Profile the application to identify bottlenecks

## Advanced Usage

### Custom Spans for Complex Operations

For operations with multiple sub-steps:

```scala
def complexOperation(): ZIO[Any, Throwable, Result] =
  SimpleTelemetry.traceEffect("complex.operation", Map.empty) {
    for {
      // First sub-step
      _ <- SimpleTelemetry.recordStart("substep.1", Map.empty)
      startTime1 <- Clock.currentTime(TimeUnit.MILLISECONDS)
      result1 <- subOperation1()
      endTime1 <- Clock.currentTime(TimeUnit.MILLISECONDS)
      _ <- SimpleTelemetry.recordEnd("substep.1", endTime1 - startTime1, Map.empty)
      
      // Second sub-step
      _ <- SimpleTelemetry.recordStart("substep.2", Map.empty)
      startTime2 <- Clock.currentTime(TimeUnit.MILLISECONDS)
      result2 <- subOperation2(result1)
      endTime2 <- Clock.currentTime(TimeUnit.MILLISECONDS)
      _ <- SimpleTelemetry.recordEnd("substep.2", endTime2 - startTime2, Map.empty)
      
      // Final result
      finalResult = combineResults(result1, result2)
    } yield finalResult
  }
```

### Custom Tags for Better Filtering

Carefully chosen tags make filtering and analysis easier:

```scala
// Good tagging practice
val tags = Map(
  "user_type" -> userType,           // Use enumerated values
  "document_size" -> sizeCategory,   // Use bucketed categories
  "priority" -> priority.toString,   // Use enumerated values
  "region" -> region                 // Use limited set of values
)

// Avoid high cardinality tags
// BAD: "user_id" -> userId          // Too many unique values
// BAD: "timestamp" -> timestamp     // Use metrics for this
// BAD: "exact_size" -> size.toString // Use buckets instead
```

## Conclusion

By following this integration guide, you can add comprehensive telemetry to your ROO-based application with minimal invasiveness while maintaining functional purity. The resulting observability will help you diagnose issues, optimize performance, and understand application behavior in production.