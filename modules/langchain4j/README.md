# Langchain4j Integration Module

## Implementation Status

This module includes implementation status markers to clearly indicate the current state of each component:

- ✅ **Implemented**: Features that are fully implemented and tested
- 🚧 **In Progress**: Features that are partially implemented
- 🔮 **Planned**: Features planned for future development

## Overview

The Langchain4j module provides integration with the Langchain4j library, enabling seamless access to various LLM providers through a standardized interface. It wraps Langchain4j components in ZIO APIs for consistent integration with the rest of the Agentic AI Framework.

## Current Status

Overall status: ✅ **Implemented**

### Features

- ✅ **ZIO Chat Model Factory**: Factory for creating ZIO-wrapped Langchain4j chat models
- ✅ **Vertex AI Model Support**: Integration with Claude and other models on Google Vertex AI
- ✅ **Streaming Support**: Real-time token streaming for chat completions
- ✅ **Error Handling**: ZIO-based error handling for all LLM operations
- 🔮 **Tool Support**: Integration with Langchain4j's tool support

## Dependencies

This module depends on:

- `core`: Required - Uses ZIO integration and core interfaces
- External dependency on Langchain4j library

## Usage Examples

```scala
import com.agenticai.core.llm.langchain.ZIOChatModelFactory
import com.agenticai.core.llm.langchain.VertexAIModelSupport
import zio.*
import dev.langchain4j.model.chat.*

// Create a Claude model using Vertex AI
val program = for {
  // Get a Claude model on Vertex AI
  model <- ZIOChatModelFactory.claudeOnVertexAI(
    projectId = "my-project",
    location = "us-central1"
  )
  
  // Create a user message
  userMessage = UserMessage.from("Explain quantum computing in simple terms")
  
  // Get a chat response
  response <- model.chat(userMessage)
  
  // Print the response
  _ <- Console.printLine(response.content)
} yield ()
```

### Streaming Example

```scala
import com.agenticai.core.llm.langchain.ZIOChatModelFactory
import zio.stream.*

// Create a streaming chat model
val streamingProgram = for {
  // Get a streaming model
  model <- ZIOChatModelFactory.vertexAIStreamingChatModel(
    projectId = "my-project",
    location = "us-central1",
    modelName = "claude-3-sonnet@20240229"
  )
  
  // Create a user message
  userMessage = UserMessage.from("Write a short poem about AI")
  
  // Get a streaming response
  responseStream <- model.chatStream(userMessage)
  
  // Process the stream
  _ <- responseStream.foreach { responsePart =>
    ZIO.succeed(print(responsePart.content))
  }
} yield ()
```

## Architecture

The module is organized around:

- **ZIOChatModelFactory**: Central factory for creating ZIO-wrapped Langchain4j models
- **VertexAIModelSupport**: Specific support for Vertex AI models
- **ZIO Wrappers**: Conversion layer between Langchain4j types and ZIO-based APIs

The implementation leverages Langchain4j's provider interfaces while adding ZIO's effect management, error handling, and concurrency benefits.

## Known Limitations

- ✅ Limited to the models and providers supported by Langchain4j
- 🚧 Tool support is not yet implemented
- 🚧 Some advanced Langchain4j features may not have ZIO wrappers yet

## Future Development

Planned enhancements:

- 🔮 Support for additional Langchain4j model providers
- 🔮 Integration with Langchain4j's retrieval augmented generation (RAG) capabilities
- 🔮 Tool/function calling support via Langchain4j
- 🔮 Agent types from Langchain4j integrated with the framework's agent system

## Testing

The module includes tests for:

- ZIO wrapper functionality
- Vertex AI model integration
- Stream handling

Run tests for this module with:
```bash
sbt "langchain4j/test"
```

For test coverage:
```bash
./scripts/run-tests-with-reports.sh --modules=langchain4j
```

## See Also

- [Langchain4j Integration Documentation](../../docs/implementation/Langchain4jIntegration.md)
- [LLM Implementation Details](../../docs/implementation/LLMImplementationDetails.md)
- [Official Langchain4j Documentation](https://docs.langchain4j.dev/)
