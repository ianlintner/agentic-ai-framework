# Core Module

## Implementation Status

This module includes implementation status markers to clearly indicate the current state of each component:

- ✅ **Implemented**: Features that are fully implemented and tested
- 🚧 **In Progress**: Features that are partially implemented
- 🔮 **Planned**: Features planned for future development

## Overview

The Core module provides the essential interfaces, abstractions, and base types for the Agentic AI Framework. It contains foundational components upon which other modules are built, including agent interfaces, memory system primitives, and category theory foundations.

## Current Status

Overall status: ✅ **Implemented**

### Features

- ✅ **Agent Interfaces**: Base agent definitions and interfaces for building agents
- ✅ **Memory System**: Core memory cell and memory system interfaces with basic implementations
- ✅ **Category Theory Foundations**: Functional programming abstractions like Monad, Applicative, and Natural Transformations
- ✅ **LLM Integration**: Clients for Claude and other LLMs via Vertex AI
- 🚧 **Circuit Patterns**: Factorio-inspired circuit patterns for agent composition
- 🚧 **Agent Combinators**: Functional composition of agents

## Dependencies

This module has no external dependencies within the framework as it is the foundation upon which other modules are built.

External dependencies:
- ZIO for effect handling and concurrency
- Google Cloud libraries for Vertex AI integration

## Usage Examples

```scala
// Basic agent implementation
import com.agenticai.core.BaseAgent
import zio.*

class MyAgent extends BaseAgent[String, String] {
  override protected def processMessage(message: String): ZStream[Any, Throwable, String] = {
    ZStream.succeed(s"Processed: $message")
  }
}

// Using a memory cell
import com.agenticai.core.memory.MemorySystem

val program = for {
  memorySystem <- ZIO.service[MemorySystem]
  cell <- memorySystem.createCell[String]("greeting")
  _ <- cell.write("Hello, World!")
  value <- cell.read
} yield value
```

### Common Patterns

```scala
// LLM Integration with Claude
import com.agenticai.core.llm.VertexAIClient
import com.agenticai.core.llm.VertexAIConfig

val config = VertexAIConfig.claudeDefault
val client = VertexAIClient(config)

val program = for {
  response <- client.complete("Tell me a joke about AI")
  stream <- client.streamCompletion("What are the best practices for prompt engineering?")
  _ <- stream.foreach(chunk => ZIO.succeed(print(chunk)))
} yield ()
```

## Architecture

The Core module is organized into several key packages:

- `com.agenticai.core`: Base agent interfaces and implementations
- `com.agenticai.core.memory`: Memory system interfaces and implementations
- `com.agenticai.core.category`: Category theory abstractions
- `com.agenticai.core.llm`: LLM client implementations

The module follows functional programming principles with pure functions, immutable data, and effect management through ZIO.

## Known Limitations

- ✅ Basic implementations focus on functionality over performance optimizations
- 🚧 Circuit patterns are still being refined and may change in future versions
- 🚧 Memory system implementations have basic persistence but lack advanced features like automatic cleanup

## Future Development

Planned enhancements:

- 🔮 Enhanced circuit patterns with more complex combinators
- 🔮 Optimized memory implementations for large-scale agent systems
- 🔮 Advanced category theory abstractions for complex agent compositions
- 🔮 Additional LLM integrations beyond Claude

## Testing

The Core module has comprehensive test coverage, including:

- Unit tests for all major components
- Property-based tests for category theory abstractions
- Mock implementations for testability without external dependencies

Run tests for this module with:
```bash
sbt "core/test"
```

For test coverage:
```bash
./scripts/run-tests-with-reports.sh --modules=core
```

## See Also

- [Memory System Features](../../docs/memory/MemorySystemFeatures.md)
- [LLM Implementation Details](../../docs/implementation/LLMImplementationDetails.md)
- [Category Theory Foundations](../../docs/theory/CategoryTheoryFoundations.md)