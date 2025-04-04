# Circuit Pattern Diagrams

This document contains visual representations of how Factorio circuit patterns are implemented in our Agentic AI framework.

## Memory Cell Comparison

```mermaid
flowchart TB
    subgraph Factorio["Factorio Memory Cell"]
        F_Input[Input Signal] --> F_DC[Decider Combinator<br>Output if condition is true]
        F_DC -->|Output| F_Output[Output Signal]
        F_DC -->|Feedback Loop| F_DC
    end
    
    subgraph AgenticAI["Agentic AI Memory Cell"]
        A_Input[Input Value] --> A_Cell[MemoryCell<br>Store & Retrieve Values]
        A_Cell --> A_Output[Output Value]
        A_Cell -->|Internal State| A_Cell
    end
    
    Factorio -.->|Inspired| AgenticAI
```

## Clock Pattern

```mermaid
flowchart TB
    subgraph Factorio["Factorio Clock"]
        F_CC[Constant Combinator] -->|"+1" every tick| F_MC[Memory Cell]
        F_MC --> F_DC[Decider Combinator<br>Reset when T > limit]
        F_DC -->|Reset Signal| F_MC
        F_MC -->|"T value"| F_Output[Clock Output]
    end
    
    subgraph AgenticAI["Agentic AI Clock"]
        A_Tick[Tick Method] -->|Increment Counter| A_Counter[Internal Counter]
        A_Counter --> A_Reset[Reset Logic<br>When counter > limit]
        A_Reset -->|Reset if needed| A_Counter
        A_Counter -->|Current Value| A_Output[Clock Output]
    end
    
    Factorio -.->|Inspired| AgenticAI
```

## Signal Processing Pipeline

```mermaid
flowchart LR
    subgraph Factorio["Factorio Signal Processing"]
        F_Input[Input Signals] --> F_Filter[Filter Combinators]
        F_Filter --> F_Transform[Transform Combinators]
        F_Transform --> F_Output[Output Signals]
    end
    
    subgraph AgenticAI["Agentic AI Pipeline"]
        A_Input[Input Data] --> A_Filter[Filter Agents]
        A_Filter --> A_Transform[Transform Agents]
        A_Transform --> A_Output[Output Data]
    end
    
    Factorio -.->|Inspired| AgenticAI
```

## Bit Packing

```mermaid
flowchart TB
    subgraph Factorio["Factorio Bit Packing"]
        F_Input["Multiple Input Signals<br>(Value 1, Value 2, Value 3)"] 
        F_Input --> F_Pack["Arithmetic Combinators<br>Shift and Combine"]
        F_Pack --> F_Packed["Single Packed Signal"]
        F_Packed --> F_Unpack["Arithmetic Combinators<br>Shift and Mask"]
        F_Unpack --> F_Output["Multiple Output Signals<br>(Value 1, Value 2, Value 3)"]
    end
    
    subgraph AgenticAI["Agentic AI Bit Packing"]
        A_Input["Multiple Values<br>(Value 1, Value 2, Value 3)"] 
        A_Input --> A_Pack["BitPacking.packInts()"]
        A_Pack --> A_Packed["Single Packed Long Value"]
        A_Packed --> A_Unpack["BitPacking.unpackInts()"]
        A_Unpack --> A_Output["Multiple Values<br>(Value 1, Value 2, Value 3)"]
    end
    
    Factorio -.->|Inspired| AgenticAI
```

## Text Processing Demo Architecture

```mermaid
flowchart TB
    Input["Input Text"] --> Tokenizer["Tokenizer Agent"]
    Tokenizer --> StopWordsFilter["Stop Words Filter Agent"]
    StopWordsFilter --> Counter["Word Counter Agent"]
    Counter --> FrequencyUpdater["Frequency Updater Agent"]
    FrequencyUpdater --> TopWordsFinder["Top Words Finder Agent"]
    TopWordsFinder --> Formatter["Formatter Agent"]
    Formatter --> Output["Formatted Results"]
    
    WordCountMemory["Word Count<br>Memory Cell"] -.->|Reads from| FrequencyUpdater
    FrequencyUpdater -->|Updates| WordCountMemory
    WordCountMemory -.->|Reads from| TopWordsFinder
    
    TopWordsMemory["Top Words<br>Memory Cell"] -.->|Reads from| TopWordsFinder
    TopWordsFinder -->|Updates| TopWordsMemory
    
    subgraph Pipeline["Agent Pipelines"]
        Pipeline1["processWords = pipeline(tokenizer, stopWordsFilter)"]
        Pipeline2["updateFrequency = pipeline(counter, wordFrequencyUpdater)"]
        Pipeline3["findTopWords = pipeline(topWordsFinder, formatter)"]
        Pipeline4["completePipeline = pipeline(pipeline1, findTopWords)"]
    end
```

## Latch Pattern

```mermaid
flowchart TB
    subgraph Factorio["Factorio Latch"]
        F_Set["Set Signal"] --> F_SR["SR Latch<br>(Set-Reset Latch)"]
        F_Reset["Reset Signal"] --> F_SR
        F_SR --> F_Output["Output Signal"]
    end
    
    subgraph AgenticAI["Agentic AI Latch"]
        A_Set["Set Value"] --> A_Cell["Memory Cell with<br>Conditional Logic"]
        A_Reset["Reset Condition"] --> A_Cell
        A_Cell --> A_Output["Output Value"]
    end
    
    Factorio -.->|Inspired| AgenticAI
```

## Ring Buffer / Shift Register

```mermaid
flowchart LR
    subgraph Factorio["Factorio Shift Register"]
        F_Input["Input Signal"] --> F_Cell1["Memory Cell 1"]
        F_Cell1 --> F_Cell2["Memory Cell 2"]
        F_Cell2 --> F_Cell3["Memory Cell 3"]
        F_Cell3 --> F_Output["Output Signal"]
        F_Clock["Clock Signal"] -->|Triggers Shift| F_Cell1
        F_Clock -->|Triggers Shift| F_Cell2
        F_Clock -->|Triggers Shift| F_Cell3
    end
    
    subgraph AgenticAI["Agentic AI Sequential Processing"]
        A_Input["Input Data"] --> A_Agent1["Agent 1<br>First Transform"]
        A_Agent1 --> A_Agent2["Agent 2<br>Second Transform"]
        A_Agent2 --> A_Agent3["Agent 3<br>Third Transform"]
        A_Agent3 --> A_Output["Output Data"]
    end
    
    Factorio -.->|Inspired| AgenticAI