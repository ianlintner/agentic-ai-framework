# ZIO Agentic AI Framework

A modern, type-safe framework for building AI agents using Scala 3 and ZIO.

## Overview

ZIO Agentic AI Framework provides a robust and scalable foundation for building AI agents that can interact with various services and maintain state. Built on top of ZIO, it leverages functional programming principles to ensure type safety, concurrency, and resilience.

## Features

- ✅ **Type-Safe Agent Definitions**: Define agents with strong type guarantees
- ✅ **ZIO Integration**: Built on ZIO for robust concurrent and asynchronous programming
- ✅ **Stream-Based Message Processing**: Handle message streams efficiently
- ✅ **Persistent Memory System**: Store and retrieve agent state across sessions
- 🚧 **Circuit Patterns**: Factorio-inspired circuit patterns for agent composition and state management
- 🚧 **Agent Combinators**: Compose agents using functional programming patterns
- 🔮 **Bit Packing**: Efficient data transmission between agents
- ✅ **Extensible Architecture**: Modular design for easy integration with various services
- ✅ **LLM Integration**: Built-in integrations with Claude on Vertex AI
- 🚧 **Web Dashboard**: Visualization and monitoring tools

## Project Structure

The framework is organized into the following modules:
- ✅ **core**: Essential base definitions and interfaces
- ✅ **memory**: Memory system implementation for agent state persistence
- 🚧 **agents**: Agent implementations for various use cases
- 🚧 **http**: Web API implementation
- 🚧 **dashboard**: Web UI and visualizations
- ✅ **workflow-demo**: Visual UI builder for agent workflow composition
- ✅ **examples**: Example applications using the framework
- ✅ **langchain4j**: Langchain4j integration for LLM access
- 🚧 **mesh**: Distributed mesh network for agent communication
- 🚧 **integration-tests**: Integration tests for the framework

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
# Run all tests
sbt test

# Run tests for a specific module
sbt "mesh/test"
```

For integration tests:

```bash
# Run integration tests for Langchain4j
sbt "it/test"
```

### Generating Test Reports

The project includes advanced test reporting tools:

```bash
# Generate test reports with coverage for all modules
./scripts/run-tests-with-reports.sh --all

# Generate test reports for specific modules
./scripts/run-tests-with-reports.sh --modules=core,mesh

# Skip coverage reports for faster execution
./scripts/run-tests-with-reports.sh --modules=core --skip-coverage
```

This generates HTML test reports, coverage reports, and summary information.

There's also a GitHub Actions workflow that can be run locally:

```bash
# Using GitHub CLI
gh workflow run scala-test-reports.yml
```

### Running Examples

The project includes several examples that demonstrate how to use the framework:

```bash
# Run the simple Claude agent example
sbt runClaudeExample

# Test the connection to Vertex AI
sbt testVertexConnection

# Run the Workflow UI Builder Demo
sbt runWorkflowDemo

# Run the Factorio-inspired Circuit Patterns Demo
sbt "core/runMain com.agenticai.core.memory.circuits.examples.TextProcessingDemo"
```

## Circuit Patterns

The framework includes a powerful circuit-based architecture inspired by Factorio's circuit network system. This approach enables sophisticated agent communication, state management, and composition patterns.

### Key Components

- ✅ **Memory Cells**: Stateful components that store and update values with precise timing control
- 🚧 **Agent Combinators**: Compose agents like Factorio's combinators to create complex processing pipelines
- 🚧 **Signals**: Typed data transmission between agents and memory cells
- 🚧 **Clock**: Timing mechanism to synchronize operations across the system
- 🔮 **Bit Packing**: Efficient data encoding for optimized signal transmission

### Circuit Patterns Demo

The framework includes an interactive terminal demo that visualizes these circuit patterns:

```bash
# Run the TextProcessingDemo in the core module
sbt "core/runMain com.agenticai.core.memory.circuits.examples.TextProcessingDemo"
```

This demo showcases:
- Memory cells storing state
- Agents processing data like Factorio's arithmetic and decider combinators
- Signal transmission between components
- Clock mechanisms controlling timing
- Bit packing for data compression

See the [Factorio Circuit Implementation](docs/theory/FactorioCircuitImplementation.md) for theoretical background.

### Using Circuit Patterns

```scala
import com.agenticai.core.memory.circuits.AgentCombinators._
import com.agenticai.core.memory.circuits.CircuitMemory

// Create a circuit memory cell that holds a value
val counterCell = CircuitMemory.cell[Int](0)

// Create a clock that increments every second
val clock = CircuitMemory.clock(1.second)

// Create an agent that increments the counter
val incrementer = clock.wireTo(counterCell.update(_ + 1))

// Create an agent that processes the counter value
val processor = counterCell.wireTo { value =>
  // Process the value here
  println(s"Current count: $value")
}

// Connect the agents to form a circuit
val circuit = incrementer.andThen(processor)

// Run the circuit
circuit.run()
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

### Using Circuit Memory

For more advanced state management with timing control:

```scala
import com.agenticai.core.memory.circuits._

// Create a circuit memory cell
val cell = CircuitMemory.cell[String]("initial value")

// Create an agent that processes the cell value
val processor = cell.map(value => s"Processed: $value")

// Create a clock-driven updater
val updater = CircuitMemory.clock(500.millis).wireTo {
  cell.update(current => s"$current - tick")
}

// Run the circuit
(processor combine updater).run()
```

## Architecture

The framework is built around the following core concepts:

- ✅ `Agent`: The base trait defining the interface for all agents
- ✅ `BaseAgent`: A base implementation providing common functionality
- ✅ `MemoryCell`: Type-safe state container for agents
- ✅ `MemorySystem`: Interface for persistent storage of agent state
- ✅ `PersistentMemorySystem`: Implementation of the memory system
- 🚧 `CircuitMemory`: State management with timing control
- 🚧 `AgentCombinators`: Functional composition of agents

## Implementation Status Legend

- ✅ **Implemented**: Features that are fully implemented and tested
- 🚧 **In Progress**: Features that are partially implemented
- 🔮 **Planned**: Features planned for future development

## Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

## License

MIT License - see the [LICENSE](LICENSE) file for details
