# Circuit Pattern Diagrams

This document provides visual representations of the circuit patterns implemented in our framework, inspired by Factorio's circuit networks.

## Memory Cell

```mermaid
flowchart LR
    Input --> |"set(value)"| MemoryCell["Memory Cell"]
    MemoryCell --> |"get()"| Output
    MemoryCell --> |"feedback"| MemoryCell
```

The Memory Cell maintains state between operations, similar to how Factorio's memory cells hold signals across multiple ticks.

## Transform (Arithmetic Combinator)

```mermaid
flowchart LR
    Input --> Agent["Agent (f)"] --> |"result"| Transform["Transform (g)"] --> Output
```

Transforms the output of an agent, analogous to an Arithmetic Combinator that takes signals and outputs a modified version.

## Filter (Decider Combinator)

```mermaid
flowchart LR
    Input --> Agent["Agent (process)"] --> |"result"| Filter["Filter (predicate)"]
    Filter --> |"Some(result)"| Output
    Filter --> |"None"| NoOutput["No Output"]
```

Conditionally passes output based on a predicate, similar to a Decider Combinator that only lets signals through if they meet a condition.

## Pipeline

```mermaid
flowchart LR
    Input --> FirstAgent["First Agent"] --> |"intermediate result"| SecondAgent["Second Agent"] --> Output
```

Connects agents in sequence, like a production line where the output of one stage becomes the input to the next.

## Parallel Processing

```mermaid
flowchart LR
    Input --> AgentA["Agent A"] --> |"result A"| Combine
    Input --> AgentB["Agent B"] --> |"result B"| Combine["Combine (A, B)"]
    Combine --> Output
```

Processes input through multiple paths simultaneously and combines the results, similar to splitting a belt in Factorio and recombining the processed items.

## Shift Register

```mermaid
flowchart LR
    Input --> Initial["Initial Agent"] --> |"stage 0"| Stage1["Stage 1"] --> |"stage 1"| Stage2["Stage 2"] --> |"stage 2"| StageN["Stage N"] --> Output
```

Applies a sequence of transformations to data, similar to Factorio's shift register that pushes signals through a sequence of memory cells.

## Clock

```mermaid
flowchart LR
    Tick["tick()"] --> |"increment"| Counter["Current Tick"] 
    Counter --> |"currentTick % interval == 0"| PulseOutput["Pulse Output"]
    Counter --> |"current tick"| TickOutput["Tick Value"]
```

Generates regular pulses, like Factorio's clock circuits that drive timing operations.

## Feedback Loop

```mermaid
flowchart LR
    Input --> Agent["Agent"] --> |"result"| Output
    Agent --> |"iterations"| Agent
```

Applies an operation repeatedly to its own output, like a recursive circuit in Factorio.

## Ring Buffer

```mermaid
flowchart LR
    Input --> |"write(value)"| Buffer["Ring Buffer"] 
    Buffer --> |"read()"| Output
    Buffer --> |"circular storage"| Buffer
```

Creates a continuous loop of memory cells, similar to how Factorio's belt printers loop data around a circuit.

## Bit Packing

```mermaid
flowchart LR
    Values["Multiple Values"] --> |"pack"| PackedValue["Single Packed Value"]
    PackedValue --> |"unpack"| UnpackedValues["Multiple Values"]
```

Compresses multiple values into a single value, like how Factorio's belt printers encode multiple pixel values into a single signal.

## Complete Processing System

```mermaid
flowchart TD
    Input --> InputStage["Input Stage"]
    InputStage --> |"transform"| Processing["Processing Stage"]
    Processing --> |"state"| MemoryCell["Memory Cell"]
    MemoryCell --> |"feedback"| Processing
    Processing --> |"filter"| Condition{"Condition Met?"}
    Condition --> |"Yes"| OutputStage["Output Stage"]
    Condition --> |"No"| Processing
    OutputStage --> Output
```

A full processing system combining multiple circuit patterns, similar to complex Factorio circuit networks that combine multiple components.

These diagrams illustrate how our circuit patterns translate the concepts from Factorio's visual programming system into functional programming constructs in our framework.