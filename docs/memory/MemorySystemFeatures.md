# Memory System Features

This document outlines the key features of the Agentic AI Framework's memory system, including automatic cleanup strategies and memory usage monitoring.

## Automatic Cleanup Strategies

The memory system now supports automatic cleanup of memory cells based on configurable strategies:

- **Time-based Cleanup**: Remove cells that haven't been accessed or modified for a certain period
- **Size-based Cleanup**: Remove cells that exceed a certain size threshold
- **Tag-based Cleanup**: Remove cells with specific tags
- **Combinatorial Strategies**: Combine multiple strategies using AND/OR logic

### Example Usage

```scala
// Create a memory system
val memorySystem = MemorySystem.make

// Register cleanup strategies
val timeStrategy = CleanupStrategy.timeBasedAccess(java.time.Duration.ofMinutes(10))
val sizeStrategy = CleanupStrategy.sizeBasedCleanup(1000) // 1KB
val tagStrategy = CleanupStrategy.tagBasedCleanup(Set("temporary"))

// Register strategies with the memory system
memorySystem.registerCleanupStrategy(timeStrategy)
memorySystem.registerCleanupStrategy(sizeStrategy)

// Enable automatic cleanup every 5 minutes
memorySystem.enableAutomaticCleanup(java.time.Duration.ofMinutes(5))

// For manual cleanup
memorySystem.runCleanup() // Run all registered strategies
memorySystem.runCleanup(tagStrategy) // Run a specific strategy
```

### Factory Methods

The memory system provides convenient factory methods for common cleanup configurations:

```scala
// Create a time-based cleanup memory system
val timeBasedSystem = MemorySystem.makeWithTimeBasedCleanup(
  baseDir = new File("./memory"),
  maxAge = java.time.Duration.ofDays(1),
  interval = java.time.Duration.ofHours(1)
)

// Create a size-based cleanup memory system
val sizeBasedSystem = MemorySystem.makeWithSizeBasedCleanup(
  baseDir = new File("./memory"),
  maxSize = 1024 * 1024, // 1MB
  interval = java.time.Duration.ofMinutes(30)
)
```

## Memory Usage Monitoring

The framework now includes a comprehensive memory monitoring system that tracks memory usage metrics over time:

- **Real-time Metrics**: Get current memory usage statistics
- **Historical Data**: Track memory metrics over time
- **Thresholds and Alerts**: Set thresholds for memory size and cell count
- **Statistical Analysis**: Calculate trends and aggregated statistics

### Example Usage

```scala
// Create a memory system
val memorySystem = MemorySystem.make

// Create a monitor service
val monitorService = MemoryMonitorService.makeWithSystem(memorySystem)

// Enable periodic collection every minute
monitorService.enablePeriodicCollection(java.time.Duration.ofMinutes(1))

// Set thresholds for alerts
monitorService.setSizeThreshold(10 * 1024 * 1024) // 10MB
monitorService.setCountThreshold(1000) // 1000 cells

// Get current metrics
val metrics = monitorService.getMetrics

// Get historical metrics
val from = java.time.Instant.now().minus(1, java.time.temporal.ChronoUnit.DAYS)
val to = java.time.Instant.now()
val historicalMetrics = monitorService.getHistoricalMetrics(from, to)

// Get statistical analysis
val statistics = monitorService.getStatistics
```

## Integration with ZIO

Both the memory system and monitor service are fully integrated with ZIO for:

- **Asynchronous Operations**: All operations are non-blocking
- **Resource Management**: Clean handling of resources
- **Error Handling**: Comprehensive error types and recovery mechanisms
- **Concurrency**: Thread-safe operations for concurrent access
- **Testing**: TestClock support for time-based testing

## Memory System Types

The framework provides two main types of memory systems:

1. **InMemorySystem**: For ephemeral, in-memory storage
2. **PersistentMemorySystem**: For durable, persistent storage

Both types support the same interfaces for cleanup and monitoring.

## Complete Example

See `modules/examples/src/main/scala/com/agenticai/examples/memory/MemoryManagementExample.scala` for a complete example demonstrating these features.

## Testing

Comprehensive test suites are available in:

- `modules/core/src/test/scala/com/agenticai/core/memory/MemoryMonitorServiceSpec.scala`
- `modules/core/src/test/scala/com/agenticai/core/memory/CleanupStrategySpec.scala`