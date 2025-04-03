# Circuit Patterns

This document describes circuit patterns inspired by Factorio's circuit networks, as implemented in our framework. These patterns allow for powerful data flow control, state management, and signal processing.

## Key Concepts from Factorio's Circuit Networks

Factorio's circuit networks provide a visual programming framework using combinators that manipulate signals. The key insights from these systems include:

1. **Update Ticks** - Operations happen in discrete time steps
2. **One-Tick Delay** - Signals take one tick to process through combinators
3. **Memory Cells** - Store and maintain state between operations
4. **Bit-Packing** - Compress multiple values into a single signal
5. **Timing Control** - Clocks and synchronization mechanisms

## Core Components

### Arithmetic Combinator
Transforms an input signal to produce an output signal. In our implementation, this is represented by the `transform` function in `AgentCombinators`.

```scala
def transform[I, A, B](agent: Agent[I, A])(f: A => B): Agent[I, B]
```

### Decider Combinator
Filters signals based on a condition. Implemented as the `filter` function.

```scala
def filter[I, O](agent: Agent[I, O])(predicate: O => Boolean): Agent[I, Option[O]]
```

### Memory Cell
Maintains state between operations, capturing and storing signals. Implemented in `CircuitMemory.MemoryCell`.

```scala
class MemoryCell[T](initialValue: T) {
  def get: T
  def set(newValue: T): Unit
  def update(f: T => T): Unit
  def asAgent: Agent[T => T, T]
}
```

## Advanced Patterns

### Pipeline
Connects components in sequence, like a production line. Implemented as `pipeline` in `AgentCombinators`.

```scala
def pipeline[I, A, B](first: Agent[I, A], second: Agent[A, B]): Agent[I, B]
```

### Parallel Processing
Processes signals through multiple paths simultaneously. Implemented as `parallel` in `AgentCombinators`.

```scala
def parallel[I, A, B, C](agentA: Agent[I, A], agentB: Agent[I, B])(combine: (A, B) => C): Agent[I, C]
```

### Shift Register
Applies transformations in sequence, creating a pipeline of operations with sequential state transitions.

```scala
def shiftRegister[I, O](stages: List[Agent[O, O]])(initial: Agent[I, O]): Agent[I, O]
```

### Clock
Controls timing and synchronization:

```scala
class Clock(interval: Int) {
  def tick(): Boolean
  def current: Int
  def reset(): Unit
}
```

### Feedback Loop
Applies an operation repeatedly to its own output. Implemented as `feedback` in `AgentCombinators`.

```scala
def feedback[O](agent: Agent[O, O])(iterations: Int): Agent[O, O]
```

### Ring Buffer
Creates a continuous loop of memory cells:

```scala
class RingBuffer[T](capacity: Int) {
  def write(value: T): Unit
  def read(): Option[T]
  def isFull: Boolean
  def isEmpty: Boolean
  def currentSize: Int
}
```

## Bit Packing

Bit packing allows for compressing multiple signals into a single value, similar to how Factorio circuits use limited signal types efficiently:

```scala
object BitPacking {
  def packInts(values: List[Int], bitWidths: List[Int]): Either[String, Long]
  def unpackInts(packed: Long, bitWidths: List[Int]): Either[String, List[Int]]
  def packBooleans(flags: List[Boolean]): Int
  def unpackBooleans(packed: Int, count: Int): List[Boolean]
}
```

## Practical Applications

These circuit patterns can be used for:

1. **Data Flow Control** - Route and transform information through an agent system
2. **State Management** - Maintain and update state over time
3. **Complex Processing Pipelines** - Chain operations with feedback loops and state transitions
4. **Signal Processing** - Filter, transform, and combine signals
5. **Resource Management** - Track and control resources with memory cells

## Example: Text Processing Pipeline

```scala
// Simple text processing agents
val tokenizer = Agent[String, List[String]] { text =>
  text.split("\\s+").toList
}

val counter = Agent[List[String], Int] { words =>
  words.length
}

val formatter = Agent[Int, String] { count =>
  s"Word count: $count"
}

// Example of using combinators
val wordCounter = pipeline(tokenizer, counter)
val completeProcessor = pipeline(wordCounter, formatter)

// Process a string and get formatted output
val result = completeProcessor.process("This is example text")
// result: "Word count: 4"
```

## Example: Memory Cell Counter

```scala
val counterCell = new MemoryCell[Int](0)
val incrementer = Agent[String, Int] { _ =>
  counterCell.update(_ + 1)
  counterCell.get
}

// Outputs: 1, 2, 3 as the memory cell maintains state
incrementer.process("First")    // 1
incrementer.process("Second")   // 2  
incrementer.process("Third")    // 3
```

## Comparison to Factorio Circuits

| Factorio Circuit | Framework Implementation |
|------------------|--------------------------|
| Arithmetic Combinator | `transform` function |
| Decider Combinator | `filter` function |
| Memory Cell | `MemoryCell` class |
| Clock Circuit | `Clock` class |
| Shift Register | `ShiftRegister` class & `shiftRegister` function |
| Belt Printer | Complex composition of components |
| Signal Routing | Combinators and pipelines |
| Bit Packing | `BitPacking` utility |

These patterns provide a foundation for implementing complex, stateful agent systems with precise control over data flow and processing, inspired by the elegant design of Factorio's circuit networks.