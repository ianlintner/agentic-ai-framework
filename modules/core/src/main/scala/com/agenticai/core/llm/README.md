# Vertex AI Client for LLM Integration

This package provides integration with Google Cloud's Vertex AI service for accessing large language models like Claude. It is designed to work seamlessly with the ZIO ecosystem.

## Components

### VertexAIClient

The main client for interacting with Google's Vertex AI API to access Claude and other models:
- Supports both streaming and non-streaming completions
- Configurable with various model parameters
- Type-safe ZIO-based API

### VertexAIConfig

Configuration for the Vertex AI client:
- Project ID, location, and model selection
- Model parameters (temperature, top-p, top-k)
- Predefined configurations for different use cases (Claude, high throughput, low latency)

### VertexAIClientMock

A mock implementation for testing or demo purposes:
- No actual API calls or credentials required
- Simulates responses for both streaming and non-streaming calls
- Useful for unit tests and examples

### ClaudeAgent

A higher-level agent built on top of VertexAIClient:
- Memory-based context management for conversations
- Conversation history tracking
- Stream-based response handling

## Usage Examples

### Basic Completion

```scala
import com.agenticai.core.llm.*
import zio.*

val program = for
  client <- VertexAIClient.create(VertexAIConfig.claudeDefault.copy(
    projectId = "your-project-id"
  ))
  response <- client.complete("What is the capital of France?")
  _ <- Console.printLine(s"Response: $response")
yield ()
```

### Streaming Completion

```scala
import com.agenticai.core.llm.*
import zio.*

val program = for
  client <- VertexAIClient.create(VertexAIConfig.claudeDefault.copy(
    projectId = "your-project-id"
  ))
  _ <- client.streamCompletion("Tell me a short story.")
       .foreach(token => Console.print(token))
yield ()
```

### Using Mock for Testing

```scala
import com.agenticai.core.llm.*
import zio.*

val testProgram = for
  client = VertexAIClientMock.make()
  response <- client.complete("What is the capital of France?")
  _ <- Console.printLine(s"Mock Response: $response")
yield ()
```

## Important Notes

1. You need valid Google Cloud credentials with Vertex AI API access
2. The environment variable `GOOGLE_APPLICATION_CREDENTIALS` should point to your credentials file
3. For streaming responses, be aware of token consumption and rate limits