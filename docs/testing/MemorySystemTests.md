# Memory System Testing Guide

This document provides details on how the Memory System components are tested in the Agentic AI Framework.

## Overview

The memory subsystem is a critical component of the Agentic AI Framework, responsible for:

- Storing and retrieving data for agents
- Managing memory lifecycle
- Cleaning up unused or obsolete memory
- Monitoring memory usage and performance

The test suite ensures these components work correctly both individually and when integrated.

## Test Structure

Memory system tests are located in `modules/core/src/test/scala/com/agenticai/core/memory/` and are organized into the following test suites:

1. `MemorySystemSpec.scala`: Tests the core memory system functionality
2. `AutomaticCleanupSpec.scala`: Tests automatic memory cleanup mechanisms
3. `CleanupStrategySpec.scala`: Tests various cleanup strategies
4. `MemoryMonitorSpec.scala`: Tests memory monitoring and metrics
5. `CompressedMemoryCellSpec.scala`: Tests compressed memory storage
6. `MemoryCellSpec.scala`: Tests basic memory cell operations
7. `PersistentMemorySystemSpec.scala`: Tests persistence capabilities

Additionally, circuit-related memory tests are in the `circuits` subdirectory:
- `AgentCombinatorsSpec.scala`: Tests agent circuit combinators
- `BitPackingSpec.scala`: Tests low-level bit operations
- `CircuitMemorySpec.scala`: Tests circuit-based memory

## MemorySystem Tests

### Core Functionality

The `MemorySystemSpec` tests basic memory operations:

```scala
test("createCell creates a new memory cell") {
  for {
    system <- MemorySystem.make
    cell <- system.createCell("test")
    value <- cell.read
  } yield assertTrue(value.contains("test"))
}
```

### Cell Tags and Retrieval

The memory system supports tagging cells and retrieving them by tag:

```scala
test("getCellsByTag returns cells with matching tag") {
  for {
    system <- MemorySystem.make
    cell1 <- system.createCellWithTags("test1", Set("tag1"))
    cell2 <- system.createCellWithTags("test2", Set("tag1", "tag2"))
    cells <- system.getCellsByTag("tag1")
  } yield assertTrue(cells.size == 2 && cells.contains(cell1) && cells.contains(cell2))
}
```

### Memory Cleanup

The test suite verifies different cleanup strategies:

```scala
test("runCleanup with timeBasedAccess cleans up old cells") {
  for {
    system <- MemorySystem.make
    cell <- system.createCell("test")
    strategy = CleanupStrategy.timeBasedAccess(JavaDuration.ofSeconds(1))
    _ <- TestClock.adjust(Duration.fromMillis(2 * 1000))
    count <- system.runCleanup(strategy)
    value <- cell.read
  } yield assertTrue(count == 1 && value.isEmpty)
}
```

## Cleanup Strategies

### Time-Based Cleanup

The `CleanupStrategySpec` tests time-based cleanup strategies:

```scala
test("timeBasedAccess should mark old cells for cleanup") {
  for {
    cell <- MemoryCell.make("test value")
    strategy <- ZIO.succeed(CleanupStrategy.timeBasedAccess(JavaDuration.ofMinutes(1)))
    _ <- TestClock.adjust(Duration.fromMillis(2 * 60 * 1000))
    shouldCleanup <- strategy.shouldCleanup(cell)
  } yield assertTrue(shouldCleanup)
}
```

### Size-Based Cleanup

Tests for size-based cleanup ensure large cells are properly identified:

```scala
test("sizeBasedCleanup should mark large cells for cleanup") {
  for {
    cell <- MemoryCell.make("initial")
    largeValue = "a" * 10000 // 10KB string
    _ <- cell.write(largeValue)
    strategy <- ZIO.succeed(CleanupStrategy.sizeBasedCleanup(5000))
    shouldCleanup <- strategy.shouldCleanup(cell)
  } yield assertTrue(shouldCleanup)
}
```

### Tag-Based Cleanup

Tag-based cleanup is verified to clean up cells with specific tags:

```scala
test("tagBasedCleanup should mark cells with specific tags for cleanup") {
  for {
    cell <- MemoryCell.makeWithTags("test value", Set("temp", "cache"))
    strategy <- ZIO.succeed(CleanupStrategy.tagBasedCleanup(Set("temp")))
    shouldCleanup <- strategy.shouldCleanup(cell)
  } yield assertTrue(shouldCleanup)
}
```

### Strategy Combinators

The test suite verifies that strategies can be combined using logical operators:

```scala
test("any combinator should mark cells for cleanup if any strategy matches") {
  for {
    cell <- MemoryCell.makeWithTags("small", Set("permanent", "important"))
    sizeStrategy <- ZIO.succeed(CleanupStrategy.sizeBasedCleanup(5000))
    tagStrategy <- ZIO.succeed(CleanupStrategy.tagBasedCleanup(Set("important")))
    combinedStrategy <- ZIO.succeed(CleanupStrategy.any(sizeStrategy, tagStrategy))
    shouldCleanup <- combinedStrategy.shouldCleanup(cell)
  } yield assertTrue(shouldCleanup)
}
```

## Memory Monitoring

The `MemoryMonitorSpec` ensures the monitoring system correctly tracks memory metrics:

```scala
test("getMetrics should return correct metrics for system with cells") {
  for {
    system <- MemorySystem.make
    cell1 <- system.createCell("small value")
    cell2 <- system.createCell("a" * 1000) // 1KB string
    cell3 <- system.createCellWithTags("tagged value", Set("test", "important"))
    monitor <- MemoryMonitor.make
    _ <- monitor.registerMemorySystem(system)
    metrics <- monitor.getMetrics
  } yield assertTrue(
    metrics.totalCells == 3 &&
    metrics.totalSize > 1000 &&
    metrics.cellsByTag.contains("test")
  )
}
```

## Circuit Memory Tests

### Bit Packing

The `CircuitMemorySpec` tests bit-level operations to ensure efficient memory usage:

```scala
test("should pack and unpack integers") {
  val values = List(3, 5, 7, 9)
  val bitWidths = List(4, 4, 4, 4)
  
  for {
    packed <- ZIO.fromEither(BitPacking.packInts(values, bitWidths))
    unpacked <- ZIO.fromEither(BitPacking.unpackInts(packed, bitWidths))
  } yield assertTrue(unpacked == values)
}
```

### Agent Combinators

The `AgentCombinatorsSpec` tests functional combinators used to build agent circuits:

```scala
test("pipeline should connect agents in sequence") {
  val double = Agent[Int, Int](_ * 2)
  val addOne = Agent[Int, Int](_ + 1)
  val pipeline1 = pipeline(double, addOne)
  
  assertTrue(pipeline1.process(5) == 11) // (5 * 2) + 1 = 11
}
```

## Running The Tests

Execute the memory system tests using SBT:

```bash
# Run all memory tests
sbt "testOnly com.agenticai.core.memory.*"

# Run a specific test suite
sbt "testOnly com.agenticai.core.memory.MemorySystemSpec"

# Run memory circuit tests
sbt "testOnly com.agenticai.core.memory.circuits.*"
```

## Test Design Philosophy

The memory system tests follow these principles:

1. **Isolation**: Each test focuses on a specific aspect of memory functionality
2. **Comprehensiveness**: Tests cover normal operation, edge cases, and error conditions
3. **Time Independence**: Using `TestClock` for time-dependent tests
4. **Resource Safety**: All resources are properly acquired and released
5. **ZIO Integration**: Tests leverage ZIO's testing capabilities for async operations

## Common Test Patterns

### Testing Time-Dependent Features

Time-dependent tests use ZIO's `TestClock` to advance time without waiting:

```scala
// Create a time-based strategy
strategy = CleanupStrategy.timeBasedAccess(JavaDuration.ofMinutes(1))

// Advance the test clock by 2 minutes to make the cell "old"
_ <- TestClock.adjust(Duration.fromMillis(2 * 60 * 1000))
```

### Testing Resource Cleanup

Resource cleanup tests verify that memory is properly released:

```scala
test("clearAll clears all cells") {
  for {
    system <- MemorySystem.make
    cell1 <- system.createCell("test1")
    cell2 <- system.createCell("test2")
    _ <- system.clearAll
    cells <- system.getAllCells
  } yield assertTrue(cells.isEmpty)
}
```

## Extending The Tests

When adding new memory system features, follow these guidelines for testing:

1. Add tests for normal operation, edge cases, and error scenarios
2. For time-dependent features, use `TestClock`
3. For resource management, verify both acquisition and release
4. For performance-critical code, consider adding benchmarks
5. Maintain test independence to allow parallel execution

## Integration With Other Components

The memory system tests also verify integration with other framework components:

1. **LLM Integration**: Tests verify that memory can store and retrieve LLM responses
2. **Agent Framework**: Tests confirm agents can use memory for state persistence
3. **Transport System**: Tests ensure memory can be used across transport boundaries