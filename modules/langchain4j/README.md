# Langchain4j ZIO Integration

This module provides ZIO wrappers for the [Langchain4j](https://github.com/langchain4j/langchain4j) library, enabling seamless integration with the ZIO ecosystem.

## Overview

The Langchain4j ZIO Integration module offers:

1. ZIO-based wrappers for Langchain4j's chat language models
2. Typed error handling with a comprehensive error hierarchy
3. ZIO Layers for dependency injection
4. Support for streaming responses
5. Memory management for conversation history

## Components

### ZIOChatLanguageModel

A ZIO wrapper for Langchain4j's `ChatLanguageModel` that provides:

- Synchronous message generation with `generate`
- Streaming message generation with `generateStream`
- Proper error handling with typed errors

```scala
trait ZIOChatLanguageModel:
  def generate(messages: List[ChatMessage]): ZIO[Any, LangchainError, AiMessage]
  def generateStream(messages: List[ChatMessage]): ZStream[Any, LangchainError, String]
```

### ZIOChatModelFactory

Factory methods for creating various LLM providers:

- OpenAI models
- Claude/Anthropic models
- VertexAI/Gemini models

```scala
// Example: Creating a Claude model
val model = ZIOChatModelFactory.makeClaudeModel(
  apiKey = "your-api-key",
  modelName = "claude-3-opus-20240229",
  temperature = Some(0.7),
  maxTokens = Some(1000)
)
```

### ZIOChatMemory

Memory management for conversation history:

- Add user and assistant messages
- Retrieve conversation history
- Clear conversation history

```scala
// Example: Creating in-memory chat memory
val memory = ZIOChatMemory.createInMemory(maxMessages = 10)
```

### Agent

A high-level abstraction for building conversational agents:

- Process user input with streaming or synchronous responses
- Manage conversation history
- Named agents for multi-agent systems

```scala
// Example: Creating an agent
val agent = for {
  model <- ZIOChatModelFactory.makeClaudeModel("your-api-key")
  memory <- ZIOChatMemory.createInMemory(10)
} yield LangchainAgent(model, memory, "assistant")

// Using the agent
val response = agent.flatMap(_.processSync("Hello, how can you help me today?"))
```

### Error Handling

Comprehensive error handling with a typed error hierarchy:

- `LangchainError` - Base error type
- `ModelError` - Errors from the underlying model
- `RateLimitError` - Rate limiting errors with retry information
- `AuthenticationError` - Authentication failures
- `ContextLengthError` - Token limit exceeded errors
- `ServiceUnavailableError` - Service unavailability with retry information
- `InvalidRequestError` - Invalid request parameters

## Usage Examples

### Basic Chat Interaction

```scala
import com.agenticai.core.llm.langchain.*
import dev.langchain4j.data.message.UserMessage
import zio.*

val program = for {
  model <- ZIOChatModelFactory.makeClaudeModel("your-api-key")
  messages = List(UserMessage.from("What is the capital of France?"))
  response <- model.generate(messages)
} yield response.text()

// Run the program
Unsafe.run(program)
```

### Streaming Responses

```scala
import com.agenticai.core.llm.langchain.*
import dev.langchain4j.data.message.UserMessage
import zio.*
import zio.stream.*

val program = for {
  model <- ZIOChatModelFactory.makeClaudeModel("your-api-key")
  messages = List(UserMessage.from("Write a short poem about programming"))
  stream = model.generateStream(messages)
  _ <- stream.foreach(chunk => Console.printLine(chunk))
} yield ()

// Run the program
Unsafe.run(program)
```

### Creating an Agent with Memory

```scala
import com.agenticai.core.llm.langchain.*
import zio.*

val program = for {
  agent <- LangchainAgent.make(
    modelType = ZIOChatModelFactory.ModelType.Claude,
    config = ZIOChatModelFactory.ModelConfig(
      apiKey = Some("your-api-key"),
      temperature = Some(0.7)
    ),
    name = "assistant",
    maxHistory = 10
  )
  
  // First interaction
  response1 <- agent.processSync("My name is Alice.")
  _ <- Console.printLine(s"Agent: $response1")
  
  // Second interaction (agent remembers the name)
  response2 <- agent.processSync("What's my name?")
  _ <- Console.printLine(s"Agent: $response2")
} yield ()

// Run the program
Unsafe.run(program)
```

## Testing

The module includes comprehensive tests for all components:

- Unit tests for individual components
- Integration tests for end-to-end functionality
- Mock implementations for testing without API calls

## Dependencies

This module depends on:

- ZIO 2.x
- Langchain4j 1.x
- Various model provider libraries (OpenAI, Anthropic, VertexAI)
