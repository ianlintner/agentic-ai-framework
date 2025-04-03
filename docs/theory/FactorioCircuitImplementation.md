# Factorio Circuit Network Implementation

This document provides an overview of our implementation of concepts inspired by the Factorio circuit networks shown in the [YouTube video](https://www.youtube.com/watch?v=etxV4pqVRm8).

## Overview

Factorio's circuit network system provides a visual programming language using combinators that manipulate signals. We've implemented similar concepts in our agentic AI framework, allowing for powerful data flow control, state management, and signal processing within agent systems.

## Key Concepts Implemented

### 1. Update Ticks and One-Tick Delay

In Factorio, operations happen in discrete time steps with a one-tick delay for signals to traverse combinators. We've implemented this using:

- Agent transformation functions that mimic the delay
- Clock implementation that operates on discrete ticks
- Pipeline and feedback patterns that handle sequential operations

### 2. Memory Cells

We've implemented memory cells that can maintain state between operations:

```scala
// In CircuitMemory.scala
class MemoryCell[T](initialValue: T) {
  def get: T
  def set(newValue: T): Unit 
  def update(f: T => T): Unit
  def asAgent: Agent[T => T, T]
}
```

Our memory cells can:
- Store any value type
- Update values with transformation functions
- Be used as agents within a processing pipeline
- Maintain state across multiple operations

### 3. Arithmetic and Decider Combinators

Factorio's combinators have been implemented as transformation functions in our agent system:

```scala
// In AgentCombinators.scala
def transform[I, A, B](agent: Agent[I, A])(f: A => B): Agent[I, B]
def filter[I, O](agent: Agent[I, O])(predicate: O => Boolean): Agent[I, Option[O]]
```

These provide the same functionality as arithmetic and decider combinators in Factorio.

### 4. Shift Registers and Ring Buffers

We've implemented shift registers that can process data through a sequence of stages:

```scala
// In CircuitMemory.scala
class ShiftRegister[T](stages: Int)
class RingBuffer[T](capacity: Int)
```

And as agent combinators:

```scala
// In AgentCombinators.scala
def shiftRegister[I, O](stages: List[Agent[O, O]])(initial: Agent[I, O]): Agent[I, O]
```

### 5. Bit Packing

Just like Factorio's belt printer that encodes multiple pixel data in a single signal, we've implemented bit packing utilities:

```scala
// In BitPacking.scala
def packInts(values: List[Int], bitWidths: List[Int]): Either[String, Long]
def unpackInts(packed: Long, bitWidths: List[Int]): Either[String, List[Int]]
```

This allows compressing multiple values into a single value for efficient transmission.

### 6. Clock Circuits

We've implemented clock mechanisms for timing and synchronization:

```scala
// In CircuitMemory.scala
class Clock(interval: Int) {
  def tick(): Boolean
  def current: Int
  def reset(): Unit
}
```

## Example Usage: Text Processing Pipeline

The TextProcessingDemo class demonstrates how these concepts can be applied to build a text processing system:

```scala
// Process words through multiple stages
val processWords = pipeline(tokenizer, stopWordsFilter)
val updateFrequency = pipeline(counter, wordFrequencyUpdater)
val findTopWords = pipeline(topWordsFinder, formatter)

// Connect the entire pipeline
val pipeline1 = pipeline(processWords, updateFrequency)
val completePipeline = pipeline(pipeline1, findTopWords)
```

## Comparison to Factorio

| Factorio Circuit Element | Our Implementation |
|--------------------------|-------------------|
| Arithmetic Combinator | `transform` function |
| Decider Combinator | `filter` function |
| Memory Cell | `MemoryCell` class |
| Shift Register | `ShiftRegister` class |
| Bit Packing | `BitPacking` utility |
| Clock Circuit | `Clock` class |
| Belt Printer | Text processor example |

## Diagrams

See the [CircuitPatternDiagrams.md](CircuitPatternDiagrams.md) file for visual representations of these patterns using mermaid diagrams.

## Applications

These circuit patterns can be used for:

1. **Agent Pipelines** - Chain transformations through multiple agents
2. **State Management** - Maintain and update state across agent operations
3. **Complex Processing** - Implement feedback loops and multi-stage processing
4. **Data Encoding** - Compress multiple signals into efficient representations
5. **Timing Control** - Synchronize operations using clock mechanisms

## Future Work

Potential extensions to this implementation:

1. Implement a visual representation of these circuit networks
2. Create a DSL for easier circuit definition
3. Add more complex patterns from Factorio like latches and edge detectors
4. Implement distributed circuit networks across multiple systems

## References

- [Factorio Wiki: Circuit Network](https://wiki.factorio.com/Circuit_network)
- [YouTube: Factorio Circuits Tutorial](https://www.youtube.com/watch?v=etxV4pqVRm8)
- [CircuitPatterns.md](CircuitPatterns.md) - Detailed documentation of implemented patterns