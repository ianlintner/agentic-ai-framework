# Langchain4j Integration for Agentic AI Framework

This module provides ZIO-based integration with [Langchain4j](https://github.com/langchain4j/langchain4j), a Java library that simplifies working with large language models.

## Overview

The integration offers several key benefits:

1. **Mature LLM Abstraction Layer**:
   - Built-in support for multiple LLM providers (Claude, OpenAI, Vertex AI, etc.)
   - Streamlined handling of context, prompt formatting, and streaming
   - Ready-to-use memory implementations

2. **ZIO Integration**: 
   - All operations are wrapped in ZIO effects for functional composition
   - Streaming support via ZStream
   - Clean error handling through ZIO error channel

3. **Testing Advantages**:
   - Deterministic tests without TestClock complexity
   - Simplified mocking capabilities
   - No timing-related issues in tests

## Requirements

The following dependencies are required:

```scala
// Added automatically via build.sbt
"dev.langchain4j" % "langchain4j" % "1.0.0-beta2"
"dev.langchain4j" % "langchain4j-anthropic" % "1.0.0-beta2"
"dev.langchain4j" % "langchain4j-vertex-ai" % "1.0.0-beta2"
"dev.langchain4j" % "langchain4j-vertex-ai-gemini" % "1.0.0-beta2"
"dev.langchain4j" % "langchain4j-google-ai-gemini" % "1.0.0-beta2"
"dev.langchain4j" % "langchain4j-open-ai" % "1.0.0-beta2"
"dev.langchain4j" % "langchain4j-http-client" % "1.0.0-beta2"
"dev.langchain4j" % "langchain4j-http-client-jdk" % "1.0.0-beta2"
```

## Usage

### Creating a Chat Model

```scala
import com.agenticai.core.llm.langchain._
import zio._

// Claude model
val claudeModel: ZIO[Any, Throwable, ZIOChatLanguageModel] = ZIOChatModelFactory.makeClaudeModel(
  apiKey = "your-api-key",
  modelName = "claude-3-opus-20240229",
  temperature = Some(0.7)
)

// Vertex AI model
val vertexModel: ZIO[Any, Throwable, ZIOChatLanguageModel] = ZIOChatModelFactory.makeVertexAIModel(
  projectId = "your-project-id",
  location = "us-central1",
  modelName = "gemini-1.5-pro"
)

// OpenAI model
val openAIModel: ZIO[Any, Throwable, ZIOChatLanguageModel] = ZIOChatModelFactory.makeOpenAIModel(
  apiKey = "your-api-key",
  modelName = "gpt-4o"
)
```

### Creating a Memory System

```scala
import com.agenticai.core.llm.langchain._
import zio._

// Create a windowed memory system that stores the last 10 turns (20 messages)
val memory: UIO[ZIOChatMemory] = ZIOChatMemory.createWindow(20)
```

### Creating an Agent

```scala
import com.agenticai.core.llm.langchain._
import zio._

// Create an agent using the factory method
val agent: ZIO[Any, Throwable, Agent] = LangchainAgent.make(
  ZIOChatModelFactory.ModelType.Claude,
  ZIOChatModelFactory.ModelConfig(
    apiKey = Some("your-api-key"),
    modelName = Some("claude-3-sonnet-20240229")
  ),
  name = "claude-assistant",
  maxHistory = 10
)

// Or create an agent manually
for {
  model <- ZIOChatModelFactory.makeClaudeModel("your-api-key")
  memory <- ZIOChatMemory.createWindow(20)
} yield LangchainAgent(model, memory, "custom-agent")
```

### Interacting with an Agent

```scala
import com.agenticai.core.llm.langchain._
import zio._
import zio.Console._

// Synchronous interaction (returns complete response)
val program1 = for {
  agent <- // ... create agent
  response <- agent.processSync("Tell me about ZIO")
  _ <- printLine(s"Response: $response")
} yield ()

// Streaming interaction (returns chunks as they're generated)
val program2 = for {
  agent <- // ... create agent
  _ <- agent.process("Tell me about ZIO")
    .tap(chunk => printLine(chunk))
    .runDrain
} yield ()
```

## Examples

See example applications in the `examples` package:

- `SimpleClaudeExample.scala` - Interactive CLI chat with Claude
- `SimpleVertexAIExample.scala` - Interactive CLI chat with Vertex AI

You can run these examples using the SBT tasks:

```
sbt runLangchainClaudeExample
sbt runLangchainVertexAIExample
```

## Testing

The integration includes comprehensive testing utilities:

```scala
import com.agenticai.core.llm.langchain.test._
import zio._
import zio.test._

// Create a mock agent
val mockAgent: ZIO[Any, Nothing, Agent] = AgentTestUtils.createMockAgent(
  responses = Map(
    "Hello" -> "Hi there!",
    "What's your name?" -> "I'm Claude."
  )
)

// Create a simple test
val test1 = test("should respond to greeting") {
  for {
    agent <- mockAgent
    response <- agent.processSync("Hello")
  } yield assertTrue(response == "Hi there!")
}

// Or use the utility methods
val test2 = AgentTestUtils.testAgent(
  "should respond to greeting",
  "Hello",
  "Hi there!"
)

val test3 = AgentTestUtils.testConversation(
  "full conversation flow",
  List(
    "Hello" -> "Hi there!",
    "What's your name?" -> "I'm Claude."
  )
)
```

## Advantages Over Custom Implementation

This integration offers several advantages over custom LLM client implementations:

1. **Reduced Maintenance Burden**: The Langchain4j team maintains compatibility with LLM APIs, so we don't have to
2. **Simplified Testing**: No more TestClock issues or timing-related test failures
3. **Broader Model Support**: Easy integration with multiple LLM providers through a consistent interface
4. **Future-Proofing**: Langchain4j updates as LLM APIs evolve, reducing our maintenance work
5. **Focus on Core Value**: Our team can focus on agentic behaviors rather than LLM client implementation details

## Migration Strategy

We recommend a phased approach:

1. Use this integration for new features 
2. Gradually migrate existing code to use the Langchain4j integration
3. Run both implementations in parallel during the transition
4. Remove the legacy implementation once migration is complete
