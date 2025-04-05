# Factorio Circuit Patterns Demo

This demo showcases how patterns from Factorio's circuit networks have been implemented in the Agentic AI framework. These patterns provide powerful ways to compose agents, manage state, and process information efficiently.

## Overview

Factorio is a popular game about building automated factories, where players can create complex automation systems using components called "combinators" that form circuit networks. These circuits can perform operations like:

- Processing and manipulating signals
- Storing and retrieving values in memory
- Timing operations with clocks
- Packing multiple values into single signals
- Implementing various logical operations

We've adapted these patterns to create a flexible and powerful framework for composing AI agents and managing state in our system.

## Available Demos

The demo package includes several examples that demonstrate different aspects of the circuit pattern implementation:

1. **Memory Cell Demo**: Shows how to use memory cells to store and retrieve values persistently between operations.
2. **Clock Demo**: Demonstrates timing-based operations using the Clock component.
3. **Bit Packing Demo**: Shows how to efficiently pack multiple values into a single value for efficient data transfer.
4. **Text Processing Demo**: A more complex demo that chains multiple components together to create a complete text processing pipeline.

## Running the Demos

To run the demos, use the provided `run-circuit-demo.sh` script:

```bash
# Make the script executable (only needed once)
chmod +x run-circuit-demo.sh

# Run all demos in sequence
./run-circuit-demo.sh

# Run a specific demo
./run-circuit-demo.sh memory
./run-circuit-demo.sh clock
./run-circuit-demo.sh bit-packing
./run-circuit-demo.sh text

# Show help information
./run-circuit-demo.sh help
```

## Circuit Components

### Memory Cell

Memory cells allow agents to store state that persists between operations.

```scala
// Create a memory cell to store an integer
val counterMemory = new CircuitMemory.MemoryCell[Int](0)

// Read the current value
val current = counterMemory.get

// Update the value
counterMemory.set(current + 1)
```

### Clock

Clocks enable timing-based operations and synchronization between components.

```scala
// Create a clock that ticks every 5 cycles
val clock = new CircuitMemory.Clock(5)

// Check if the clock has ticked this cycle
if (clock.tick()) {
  // Perform an action on the clock pulse
  println("Clock triggered!")
}
```

### Bit Packing

Bit packing allows efficient storage and transfer of multiple values in a single signal.

```scala
// Pack multiple values with different bit widths
val values = List(3, 5, 9, 15)
val bitWidths = List(2, 3, 4, 5)
val packedResult = BitPacking.packInts(values, bitWidths)

// Unpack values
val unpackedResult = BitPacking.unpackInts(packed, bitWidths)
```

### Agent Combinators

Agent combinators allow for composing simple agents into more complex processing pipelines.

```scala
// Define simple agents
val tokenizer = Agent[String, List[String]] { text =>
  text.toLowerCase.split("\\s+").toList
}

val stopWordsFilter = Agent[List[String], List[String]] { words =>
  words.filterNot(stopWords.contains)
}

// Compose them into a pipeline
val processWords = pipeline(tokenizer, stopWordsFilter)

// Use the combined pipeline
val result = processWords.process("The quick brown fox")
```

## Further Reading

For more information about the implementation and concepts behind these circuit patterns, refer to the following documentation:

- [Factorio Circuit Patterns](../docs/theory/FactorioCircuitPatterns.md)
- [Circuit Pattern Diagrams](../docs/theory/CircuitPatternDiagrams.md)

## Implementation Notes

- The circuit components are implemented in `modules/core/src/main/scala/com/agenticai/core/memory/circuits/`.
- The BitPacking utility provides efficient data encoding and decoding.
- CircuitMemory contains Memory Cell and Clock implementations.
- AgentCombinators provides tools to compose agents into pipelines.