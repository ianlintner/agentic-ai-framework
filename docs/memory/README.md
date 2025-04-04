# Memory System Documentation

## Overview

The memory system provides a robust, type-safe foundation for managing state and communication in agentic AI systems. It supports both in-memory and persistent storage, with advanced features for resilience and asynchronous communication.

## Core Concepts

### Memory Cells

Memory cells are the fundamental building blocks of the system. Each cell:
- Maintains a value of type `A`
- Tracks metadata (creation time, access time, size, tags)
- Provides atomic read/write operations
- Supports tagging for organization and retrieval

```scala
// Create a simple memory cell
val cell = MemoryCell.make("initial value")

// Create a tagged memory cell
val taggedCell = MemoryCell.makeWithTags("value", Set("important", "temporary"))

// Read and write operations
for {
  value <- cell.read
  _ <- cell.write("new value")
  _ <- cell.update(_ + " updated")
} yield ()
```

### Memory Systems

Memory systems manage collections of cells and provide:
- Cell creation and management
- Tag-based retrieval
- Bulk operations
- Persistence (optional)

```scala
// Create an in-memory system
val system = MemorySystem.make

// Create a persistent system
val persistentSystem = PersistentMemorySystem.make(new File("memory-data"))

// Create and manage cells
for {
  cell1 <- system.createCell("value1")
  cell2 <- system.createCellWithTags("value2", Set("tag1"))
  taggedCells <- system.getCellsByTag("tag1")
} yield ()
```

### Compressed Memory Cells

The system provides memory cells with compression capabilities to reduce memory usage:

```scala
// String serializer/deserializer for compression
val serializer: String => Array[Byte] = _.getBytes(StandardCharsets.UTF_8)
val deserializer: Array[Byte] => String = bytes => new String(bytes, StandardCharsets.UTF_8)

// Create a compressed memory cell with GZIP compression (default)
val compressedCell = CompressedMemoryCell.make(
  "Initial large string value...",
  serializer,
  deserializer
)

// Create a compressed cell with custom compression strategy
val customCompressedCell = CompressedMemoryCell.makeWithStrategy(
  "Initial large string value...",
  GzipCompressionStrategy,  // You can implement your own CompressionStrategy
  serializer,
  deserializer,
  2048  // Custom compression threshold (in bytes)
)

// Get compression statistics
for {
  stats <- compressedCell.getCompressionStats
  _ <- ZIO.foreach(stats) { s =>
    ZIO.succeed(println(s"Original: ${s.originalSize} bytes, Compressed: ${s.compressedSize} bytes, Ratio: ${s.compressionRatio}:1"))
  }
} yield ()

// Force compression even for small data
compressedCell.forceCompress
```

Compressed memory cells provide the same operations as normal memory cells, but internally manage compression of the data:

- Small values (below the threshold, default 1KB) are not compressed
- Compression statistics are tracked (original size, compressed size, ratio)
- Multiple compression strategies are supported (GZIP by default, extensible)

## Agent Integration

### Basic Agent Memory

```scala
class MemoryAwareAgent extends BaseAgent {
  private val memory = MemorySystem.make

  def process(input: String): ZIO[Any, AgentError, String] = {
    for {
      // Create or retrieve context cell
      context <- memory.createCellWithTags(input, Set("context"))
      
      // Process with memory
      result <- processWithMemory(context)
      
      // Store result
      _ <- memory.createCellWithTags(result, Set("result"))
    } yield result
  }
}
```

### Resilient Processing

The memory system supports resilient processing patterns:

```scala
def resilientProcess[A](input: A): ZIO[Any, AgentError, A] = {
  for {
    system <- MemorySystem.make
    // Create checkpoint cell
    checkpoint <- system.createCellWithTags(input, Set("checkpoint"))
    
    // Process with timeout and retry
    result <- processWithTimeout(input, 5.seconds)
      .retry(Schedule.exponential(1.second) && Schedule.recurs(3))
      .catchAll { error =>
        // On failure, restore from checkpoint
        checkpoint.read.map(_.asInstanceOf[A])
      }
    
    // Update checkpoint
    _ <- checkpoint.write(result)
  } yield result
}
```

### Asynchronous Communication

Agents can communicate through memory cells without direct coupling:

```scala
class CommunicatingAgent extends BaseAgent {
  private val system = MemorySystem.make
  
  def sendMessage(message: String): ZIO[Any, AgentError, Unit] = {
    for {
      // Create message cell with unique ID
      messageId = java.util.UUID.randomUUID().toString
      _ <- system.createCellWithTags(
        Message(messageId, message),
        Set("message", messageId)
      )
    } yield ()
  }
  
  def receiveMessages: ZIO[Any, AgentError, List[Message]] = {
    for {
      // Get all message cells
      cells <- system.getCellsByTag("message")
      // Read messages
      messages <- ZIO.foreach(cells.toList)(_.read.map(_.asInstanceOf[Message]))
    } yield messages
  }
}
```

## Advanced Patterns

### Memory Optimization with Compression

For memory-intensive applications, compression can significantly reduce memory usage:

```scala
class MemoryOptimizedAgent extends BaseAgent {
  // Custom serializers for domain objects
  private val serializer: DomainObject => Array[Byte] = obj => {
    // Use a library like Jackson, Circe, etc. for serialization
    // This is just a simplified example
    obj.toString.getBytes(StandardCharsets.UTF_8)
  }
  
  private val deserializer: Array[Byte] => DomainObject = bytes => {
    // Deserialize bytes back to domain object
    DomainObject.fromString(new String(bytes, StandardCharsets.UTF_8))
  }
  
  def processLargeData(data: List[DomainObject]): ZIO[Any, AgentError, Result] = {
    for {
      // Store large data with compression
      compressedCell <- CompressedMemoryCell.make(
        data,
        serializer,
        deserializer,
        512 // Lower compression threshold for more aggressive compression
      )
      
      // Process data in chunks to minimize memory usage
      result <- processDataInChunks(compressedCell)
      
      // Get compression stats for monitoring/logging
      stats <- compressedCell.getCompressionStats
      _ <- ZIO.foreach(stats) { s => 
        ZIO.succeed(println(s"Memory savings: ${s.originalSize - s.compressedSize} bytes (${s.compressionRatio}:1 ratio)"))
      }
    } yield result
  }
}
```

### Waiting for Updates

```scala
def waitForUpdate[A](cell: MemoryCell[A], timeout: Duration): ZIO[Any, AgentError, A] = {
  for {
    initial <- cell.read
    result <- cell.read
      .repeatUntil(_ != initial)
      .timeout(timeout)
      .someOrFail(AgentError.TimeoutError("Update timeout"))
  } yield result
}
```

### Conditional Processing

```scala
def processIfUpdated[A](cell: MemoryCell[A]): ZIO[Any, AgentError, Option[A]] = {
  for {
    current <- cell.read
    metadata <- cell.metadata
    result <- if (metadata.lastModified.isAfter(metadata.lastAccessed)) {
      // Process only if updated since last access
      processValue(current).map(Some(_))
    } else {
      ZIO.succeed(None)
    }
  } yield result
}
```

### Memory-Based Coordination

```scala
class CoordinatedAgents extends BaseAgent {
  private val system = MemorySystem.make
  
  def coordinate[A](task: A): ZIO[Any, AgentError, Unit] = {
    for {
      // Create coordination cell
      coord <- system.createCellWithTags(
        CoordinationState(task, Set.empty),
        Set("coordination")
      )
      
      // Wait for all agents to acknowledge
      _ <- waitForCoordination(coord)
      
      // Execute coordinated task
      _ <- executeTask(task)
    } yield ()
  }
  
  private def waitForCoordination(
    coord: MemoryCell[CoordinationState]
  ): ZIO[Any, AgentError, Unit] = {
    for {
      state <- coord.read
      _ <- if (state.acknowledgments.size < expectedAgents) {
        // Wait for more acknowledgments
        waitForUpdate(coord, 5.seconds) *> waitForCoordination(coord)
      } else {
        ZIO.unit
      }
    } yield ()
  }
}
```

## Best Practices

1. **Cell Lifecycle Management**
   - Create cells with meaningful tags
   - Clear cells when no longer needed
   - Use appropriate persistence strategies

2. **Resilience**
   - Implement checkpointing for long-running operations
   - Use timeouts and retries appropriately
   - Handle failures gracefully

3. **Communication**
   - Use unique IDs for message cells
   - Implement proper message acknowledgment
   - Clean up old messages

4. **Performance**
   - Use compressed cells for large data
   - Monitor compression ratios for optimization
   - Choose appropriate compression thresholds
   - Use memory cells appropriate for data size
   - Implement cleanup strategies

## Integration with Agent Framework

The memory system integrates seamlessly with the agent framework:

```scala
class MemoryAwareChatAgent extends ChatAgent {
  private val memory = MemorySystem.make
  
  override def process(input: String): ZIO[Any, AgentError, String] = {
    for {
      // Store conversation context
      context <- memory.createCellWithTags(
        ConversationContext(input),
        Set("conversation", "context")
      )
      
      // Process with memory
      result <- super.process(input)
      
      // Store result
      _ <- memory.createCellWithTags(
        result,
        Set("conversation", "result")
      )
    } yield result
  }
}
```

## Future Enhancements

1. **Advanced Memory Features**
   - Automatic cleanup
   - Memory usage monitoring

2. **Communication Patterns**
   - Pub/sub system
   - Message routing
   - Priority queues

3. **Resilience Features**
   - Automatic recovery
   - State replication
   - Conflict resolution

4. **Integration Features**
   - Database integration
   - Cache integration
   - Distributed storage