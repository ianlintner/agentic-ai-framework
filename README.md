# Agentic AI Framework

A modern, type-safe framework for building AI agents using Scala 3 and ZIO.

## Overview

Agentic AI Framework provides a robust and scalable foundation for building AI agents that can interact with various services and maintain state. Built on top of ZIO, it leverages functional programming principles to ensure type safety, concurrency, and resilience.

## Features

- **Type-Safe Agent Definitions**: Define agents with strong type guarantees
- **ZIO Integration**: Built on ZIO for robust concurrent and asynchronous programming
- **Stream-Based Message Processing**: Handle message streams efficiently
- **Persistent Memory System**: Store and retrieve agent state across sessions
- **Extensible Architecture**: Modular design for easy integration with various services
- **LLM Integration**: Built-in integrations with Claude on Vertex AI
- **Web Dashboard**: Visualization and monitoring tools

## Project Structure

The framework is organized into the following modules:

- **core**: Essential base definitions and interfaces
- **memory**: Memory system implementation for agent state persistence
- **agents**: Agent implementations for various use cases
- **http**: Web API implementation
- **dashboard**: Web UI and visualizations
- **examples**: Example applications using the framework
- **demo**: Standalone demos with minimal dependencies

## Getting Started

### Prerequisites

- Java 11 or later
- Scala 3.3.1 or later
- SBT (Scala Build Tool)

### Building the Project

```bash
sbt compile
```

### Running Tests

```bash
sbt test
```

For integration tests:

```bash
sbt integrationTest
```

### Running Examples

The project includes several examples that demonstrate how to use the framework:

```bash
# Run the simple Claude agent example
sbt runClaudeExample

# Test the connection to Vertex AI
sbt testVertexConnection
```

## Usage

### Creating a New Agent

1. Extend the `BaseAgent` class with your message and action types:

```scala
class MyAgent extends BaseAgent[InputType, OutputType] {
  override protected def processMessage(message: InputType): ZStream[Any, Throwable, OutputType] = {
    // Implement your agent's logic here
    ZStream.fromIterable(/* your processing logic */)
  }
}
```

2. Use the agent in your application:

```scala
val agent = new MyAgent()
agent.process(inputMessage)
  .foreach(action => /* handle the action */)
```

### Using the Memory System

The framework provides a flexible memory system for storing agent state:

```scala
val memorySystem = new PersistentMemorySystem("./memory")
val cell = memorySystem.createCell("myValue", "initialValue")

// Read value
val value = cell.read()

// Update value
cell.write("newValue")

// Update with function
cell.update(currentValue => s"$currentValue with addition")
```

## Architecture

The framework is built around the following core concepts:

- `Agent`: The base trait defining the interface for all agents
- `BaseAgent`: A base implementation providing common functionality
- `MemoryCell`: Type-safe state container for agents
- `MemorySystem`: Interface for persistent storage of agent state
- `PersistentMemorySystem`: Implementation of the memory system

## Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

## License

MIT License - see the [LICENSE](LICENSE) file for details