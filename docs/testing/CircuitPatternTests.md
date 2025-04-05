# Circuit Pattern Testing Guide

This document explains how circuit patterns and agent combinators are tested in the Agentic AI Framework.

## Overview

The circuit pattern is a core architectural pattern in the Agentic AI Framework, inspired by digital circuits and functional programming. These patterns allow for:

- Composable agent behaviors
- Efficient state management
- Parallelization and sequencing of operations
- Complex information flow between agents

## Test Structure

Circuit pattern tests are located in `modules/core/src/test/scala/com/agenticai/core/memory/circuits/` and include:

1. `AgentCombinatorsSpec.scala`: Tests functional combinators for agent composition
2. `BitPackingSpec.scala`: Tests bit-level data operations
3. `CircuitMemorySpec.scala`: Tests circuit-based memory implementations

## Agent Combinators

The `AgentCombinatorsSpec` tests verify that agents can be combined in various ways to create more complex behaviors.

### Transform

Tests that the `transform` combinator correctly applies a function to an agent's output:

```scala
test("transform should apply a function to an agent's output") {
  val agent = Agent[Int, Int](_ * 2)
  val transformed = transform(agent)(x => x + 1)
  
  assertTrue(transformed.process(5) == 11) // (5 * 2) + 1 = 11
}
```

### Filter

Tests that the `filter` combinator conditionally passes outputs based on a predicate:

```scala
test("filter should conditionally pass output based on a predicate") {
  val agent = Agent[Int, Int](x => x * x)
  val filtered = filter(agent)(_ > 10)
  
  assertTrue(filtered.process(4) == Some(16)) && // 16 > 10, so Some(16)
  assertTrue(filtered.process(3) == None) // 9 < 10, so None
}
```

### Pipeline

Tests that the `pipeline` combinator connects agents in sequence, where each agent processes the output of the previous agent:

```scala
test("pipeline should connect agents in sequence") {
  val double = Agent[Int, Int](_ * 2)
  val addOne = Agent[Int, Int](_ + 1)
  val pipeline1 = pipeline(double, addOne)
  
  assertTrue(pipeline1.process(5) == 11) // (5 * 2) + 1 = 11
}
```

### Parallel

Tests that the `parallel` combinator processes inputs through multiple agents in parallel and combines their results:

```scala
test("parallel should process through multiple agents and combine results") {
  val square = Agent[Int, Int](x => x * x)
  val double = Agent[Int, Int](_ * 2)
  val combiner = parallel(square, double)((a, b) => a + b)
  
  assertTrue(combiner.process(5) == 35) // (5 * 5) + (5 * 2) = 25 + 10 = 35
}
```

### Shift Register

Tests that the `shiftRegister` combinator applies multiple transformations in sequence:

```scala
test("shiftRegister should apply multiple transformations in sequence") {
  val stages = List(
    Agent[Int, Int](_ + 1),
    Agent[Int, Int](_ * 2),
    Agent[Int, Int](_ - 3)
  )
  val initial = Agent[String, Int](_.toInt)
  val register = shiftRegister(stages)(initial)
  
  assertTrue(register.process("5") == 9) // ((5 + 1) * 2) - 3 = 12 - 3 = 9
}
```

### Feedback

Tests that the `feedback` combinator applies an agent repeatedly:

```scala
test("feedback should apply an agent repeatedly") {
  val doubler = Agent[Int, Int](_ * 2)
  val repeated = feedback(doubler)(3)
  
  assertTrue(repeated.process(2) == 16) // 2 * 2 * 2 * 2 = 16
}
```

### Memory Cell

Tests that `MemoryCell` maintains state between operations:

```scala
test("MemoryCell should maintain state between operations") {
  // Tests various operations on memory cells
  val initialCell = new MemoryCell[Int](0)
  val initialValue = initialCell.get
  
  val setCell = new MemoryCell[Int](0)
  setCell.set(5)
  val setValue = setCell.get
  
  // ... more operations ...
  
  assertTrue(initialValue == 0) &&
  assertTrue(setValue == 5) &&
  // ... more assertions ...
}
```

## Bit Packing

The `CircuitMemorySpec` tests ensure that the bit-level operations for efficient memory representation work correctly.

### Integer Packing

Tests that integers can be packed and unpacked within bit constraints:

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

### Variable Bit Widths

Tests that different bit widths can be handled correctly:

```scala
test("should handle different bit widths") {
  val values = List(1, 100, 3)
  val bitWidths = List(2, 8, 4)  // 2 bits, 8 bits, 4 bits
  
  for {
    packed <- ZIO.fromEither(BitPacking.packInts(values, bitWidths))
    unpacked <- ZIO.fromEither(BitPacking.unpackInts(packed, bitWidths))
  } yield assertTrue(unpacked == values)
}
```

### Boolean Packing

Tests that boolean values can be efficiently packed and unpacked:

```scala
test("should pack and unpack booleans") {
  val flags = List(true, false, true, false, true)
  val packed = BitPacking.packBooleans(flags)
  val unpacked = BitPacking.unpackBooleans(packed, flags.length)
  
  assertTrue(unpacked == flags)
}
```

### Error Handling

Tests that appropriate errors are returned when operations exceed constraints:

```scala
test("should fail when value doesn't fit in bit width") {
  val values = List(5, 20, 3)  // 20 won't fit in 4 bits
  val bitWidths = List(4, 4, 4)
  
  val result = BitPacking.packInts(values, bitWidths)
  assertTrue(result.isLeft)
}
```

## Real-World Applications

The circuit pattern tests validate not just individual combinators, but also how they can be composed to solve real-world problems. Here are some examples of what the tests ensure:

### Data Transformation Pipelines

Tests ensure that data can flow through a series of transformations correctly:

```scala
// Example: A pipeline that processes text
val tokenizer = Agent[String, List[String]](_.split(" ").toList)
val filter = Agent[List[String], List[String]](_.filter(_.length > 3))
val counter = Agent[List[String], Int](_.size)
val pipeline = pipeline(pipeline(tokenizer, filter), counter)

// This should count words longer than 3 characters
assertTrue(pipeline.process("this is a test sentence") == 2) // "test" and "sentence"
```

### State Management

Tests verify that state can be maintained across operations:

```scala
// A memory cell that keeps track of values
val cell = new MemoryCell[List[Int]](List())
val addToList = cell.asAgent
val result1 = addToList.process(list => 1 :: list)
val result2 = addToList.process(list => 2 :: list)
val finalState = cell.get

assertTrue(finalState == List(2, 1))
```

### Error Handling Circuits

Tests confirm that error handling circuits behave correctly:

```scala
// An agent that might fail
val division = Agent[Int, Either[String, Int]](x => 
  if (x == 0) Left("Division by zero") else Right(100 / x)
)

// Filter out errors
val onlySuccess = filter(division)(_.isRight)

assertTrue(onlySuccess.process(10) == Some(Right(10)))
assertTrue(onlySuccess.process(0) == None)
```

## Running Circuit Tests

Execute the circuit pattern tests using SBT:

```bash
# Run all circuit tests
sbt "testOnly com.agenticai.core.memory.circuits.*"

# Run a specific circuit test suite
sbt "testOnly com.agenticai.core.memory.circuits.AgentCombinatorsSpec"
```

## Test Design Philosophy

The circuit pattern tests follow these principles:

1. **Composability**: Tests verify that combinators work correctly both individually and when composed
2. **Type Safety**: Tests ensure that combinators properly handle type transformations
3. **Edge Cases**: Tests cover boundary conditions and error states
4. **Simplicity**: Tests use simple functions to make behavior and expectations clear

## Extending Circuit Tests

When adding new circuit combinators or patterns, follow these guidelines:

1. Test the combinator's basic functionality in isolation
2. Test composition with other combinators
3. Test edge cases and error handling
4. Test realistic usage scenarios to validate practical utility
5. Consider performance implications for data-intensive operations

## Integration With Framework

The circuit pattern tests also verify integration with other framework components:

1. **Memory System**: Tests ensure circuits can store and retrieve state in the memory system
2. **Agent Framework**: Tests confirm that agents based on circuit patterns behave correctly
3. **Complex Agent Behaviors**: Tests verify that complex agent behaviors can be composed from simple combinators

## Circuit-Based Testing Patterns

The use of circuit patterns has also influenced how tests themselves are written:

1. **Test Composition**: Complex test scenarios are broken down into smaller, composable parts
2. **State Management**: Test state is managed explicitly through the same patterns used in the system
3. **Parallel Testing**: Independent tests can be run in parallel using the same patterns