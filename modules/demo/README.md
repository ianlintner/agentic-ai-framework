# Agentic AI Framework - Scala 3 Claude Integration

This module demonstrates an integration between Claude 3.7 and our Agentic AI Framework using **Scala 3 exclusively**.

## Features

- **Pure Scala 3 Implementation**: Uses Scala 3.3.1 with features that don't exist in Scala 2
- **ZIO Integration**: Leverages ZIO for functional effect handling
- **Vertex AI Client**: Connects to Claude 3.7 via Google Cloud's Vertex AI
- **No External Languages**: No Python, JavaScript or other non-Scala 3 code

## Scala 3 Features Demonstrated

The codebase showcases Scala 3-specific features including:

- Top-level functions and definitions
- Native enums with methods
- Union and intersection types
- Extension methods
- Opaque type aliases
- Context functions
- Significant indentation syntax (no curly braces)
- Export clauses
- Type lambdas

## Running the Demo

Use the provided shell scripts to execute the demos:

```bash
# To demonstrate pure Scala 3 features
./run-scala3-demo.sh

# To run the Claude 3.7 integration
./run-standalone-demo.sh
```

## Implementation Details

The Claude 3.7 integration is implemented in:
- `src/main/scala/com/agenticai/demo/VertexAIClaudeDemo.scala`

Scala 3 language features are showcased in:
- `src/main/scala/com/agenticai/demo/Scala3OnlyFeatures.scala`

## Building

The project is built using SBT with Scala 3.3.1:

```bash
sbt clean compile
```

## Compliance with Project Goals

This implementation fully complies with the project goals:
1. **Scala 3 Only**: All code is written in Scala 3.3.1
2. **Functional Programming**: Uses ZIO for pure functional effect handling
3. **No External Languages**: Removed all Python and non-Scala code
4. **Always Buildable**: Clean compilation with sbt
5. **Type Safety**: Leverages Scala 3's enhanced type system

This demonstrates our commitment to using Scala 3 and ZIO as specified in the project goals.