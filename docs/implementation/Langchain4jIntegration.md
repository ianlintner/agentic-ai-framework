# Langchain4j + ZIO Agentic AI Framework Integration Design Document

## Executive Summary

This document outlines a strategy to integrate Langchain4j with our ZIO Agentic AI Framework. By leveraging Langchain4j for LLM interactions, we can focus our development efforts on the agentic/task components of our system while delegating LLM management to a mature library. This approach will solve several existing issues, including the TestClock challenges we're experiencing, while providing a more robust foundation for future development.

## Objectives

1. Eliminate custom LLM client management (Claude, VertexAI, etc.)
2. Resolve TestClock testing issues by using a more testable abstraction
3. Focus development resources on agentic behaviors and task execution
4. Improve test reliability and maintenance
5. Maintain ZIO functional programming principles

## Architectural Overview

The integration will follow a layered approach:

```
┌─────────────────────────────────────┐
│          Agentic Framework          │
│   (Focus area - Task Execution)     │
└───────────────┬─────────────────────┘
                │
┌───────────────▼─────────────────────┐
│     ZIO Langchain4j Adapters        │
│ (Our new layer - thin ZIO wrappers) │
└───────────────┬─────────────────────┘
                │
┌───────────────▼─────────────────────┐
│          Langchain4j                │
│  (LLM abstraction, prompting, etc.) │
└─────────────────────────────────────┘
```

### Key Components

1. **ZIO Langchain4j Service Layer**
   - Thin adapters exposing Langchain4j functionality as ZIO effects
   - ZLayer-based dependency injection
   - Error handling translated to ZIO error channel

2. **LLM Interaction Module**
   - Chat models (Claude, OpenAI, etc.)
   - Embedding models
   - Streaming support

3. **Memory System Adapters**
   - Conversation history with ZIO persistence
   - Vector store integration
   - Context management

4. **Testing Infrastructure**
   - Mock LLM implementations
   - Deterministic test environments
   - Record/replay capabilities

## Implementation Plan

### Phase 1: Core ZIO Adapters

#### 1.1 Basic Dependencies Setup

```scala
// build.sbt
libraryDependencies ++= Seq(
  "dev.langchain4j" % "langchain4j" % "0.26.1",
  "dev.langchain4j" % "langchain4j-anthropic" % "0.26.1",
  "dev.langchain4j" % "langchain4j-vertex-ai" % "0.26.1"
)
```

#### 1.2 Chat Model ZIO Wrapper

```scala
package com.agenticai.core.llm.langchain

import dev.langchain4j.model.chat._
import zio._
import zio.stream._

trait ZIOChatLanguageModel {
  def generate(request: ChatLanguageModel.Request): ZIO[Any, Throwable, ChatLanguageModel.Response]
  def generateStream(request: ChatLanguageModel.Request): ZStream[Any, Throwable, String]
}

case class ZIOChatLanguageModelLive(model: ChatLanguageModel) extends ZIOChatLanguageModel {
  override def generate(request: ChatLanguageModel.Request): ZIO[Any, Throwable, ChatLanguageModel.Response] =
    ZIO.attemptBlocking(model.generate(request))
  
  override def generateStream(request: ChatLanguageModel.Request): ZStream[Any, Throwable, String] =
    // Implementation depends on streaming support in chosen model
    // This is a simplified version
    ZStream.fromZIO(generate(request)).map(_.content.text)
}

object ZIOChatLanguageModel {
  def apply(model: ChatLanguageModel): ZIOChatLanguageModel = 
    ZIOChatLanguageModelLive(model)
    
  // Layer for ZIO DI
  def layer(model: ChatLanguageModel): ZLayer[Any, Nothing, ZIOChatLanguageModel] =
    ZLayer.succeed(ZIOChatLanguageModelLive(model))
}
```

#### 1.3 Factory Methods for Various Models

```scala
package com.agenticai.core.llm.langchain

import dev.langchain4j.model.anthropic.AnthropicChatModel
import dev.langchain4j.model.openai.OpenAiChatModel
import dev.langchain4j.model.vertexai.VertexAiChatModel
import zio._

object ZIOChatModelFactory {
  def makeClaudeModel(apiKey: String, modelName: String = "claude-3-opus-20240229"): ZIO[Any, Throwable, ZIOChatLanguageModel] =
    ZIO.attempt {
      val model = AnthropicChatModel.builder()
        .apiKey(apiKey)
        .modelName(modelName)
        .build()
      
      ZIOChatLanguageModel(model)
    }
  
  def makeVertexAIModel(projectId: String, location: String, modelName: String): ZIO[Any, Throwable, ZIOChatLanguageModel] =
    ZIO.attempt {
      val model = VertexAiChatModel.builder()
        .project(projectId)
        .location(location)
        .modelName(modelName)
        .build()
      
      ZIOChatLanguageModel(model)
    }
    
  // More factory methods for other providers
}
```

#### 1.4 Memory System Adapter

```scala
package com.agenticai.core.llm.langchain

import dev.langchain4j.memory.ChatMemory
import dev.langchain4j.memory.chat.MessageWindowChatMemory
import zio._

trait ZIOChatMemory {
  def addUserMessage(message: String): ZIO[Any, Throwable, Unit]
  def addAssistantMessage(message: String): ZIO[Any, Throwable, Unit]
  def messages: ZIO[Any, Throwable, List[dev.langchain4j.data.message.ChatMessage]]
  def clear(): ZIO[Any, Throwable, Unit]
}

case class ZIOChatMemoryLive(memory: ChatMemory) extends ZIOChatMemory {
  override def addUserMessage(message: String): ZIO[Any, Throwable, Unit] =
    ZIO.attemptBlocking(memory.add(dev.langchain4j.data.message.UserMessage.from(message)))
  
  override def addAssistantMessage(message: String): ZIO[Any, Throwable, Unit] =
    ZIO.attemptBlocking(memory.add(dev.langchain4j.data.message.AiMessage.from(message)))
  
  override def messages: ZIO[Any, Throwable, List[dev.langchain4j.data.message.ChatMessage]] =
    ZIO.attemptBlocking(memory.messages().toArray.toList.asInstanceOf[List[dev.langchain4j.data.message.ChatMessage]])
  
  override def clear(): ZIO[Any, Throwable, Unit] =
    ZIO.attemptBlocking(memory.clear())
}

object ZIOChatMemory {
  def createWindow(maxMessages: Int): UIO[ZIOChatMemory] =
    ZIO.succeed(ZIOChatMemoryLive(MessageWindowChatMemory.withMaxMessages(maxMessages)))
}
```

### Phase 2: Agent Implementation

#### 2.1 Basic Agent Interface

```scala
package com.agenticai.core.agents

import com.agenticai.core.llm.langchain._
import zio._
import zio.stream._

trait Agent {
  def process(input: String): ZStream[Any, Throwable, String]
  def processSync(input: String): ZIO[Any, Throwable, String]
}

case class LangchainAgent(
  chatModel: ZIOChatLanguageModel,
  memory: ZIOChatMemory,
  name: String
) extends Agent {
  override def process(input: String): ZStream[Any, Throwable, String] = {
    for {
      _ <- ZStream.fromZIO(memory.addUserMessage(input))
      messages <- ZStream.fromZIO(memory.messages)
      
      // Convert messages to Langchain4j request format
      request = chatModelRequestFromMessages(messages)
      
      // Stream the response
      responseChunk <- chatModel.generateStream(request)
      
      // After receiving all chunks, save the full response
      _ <- ZStream.fromZIO(memory.addAssistantMessage(responseChunk).when(isLastChunk))
    } yield responseChunk
  }
  
  override def processSync(input: String): ZIO[Any, Throwable, String] = {
    for {
      _ <- memory.addUserMessage(input)
      messages <- memory.messages
      
      // Convert messages to Langchain4j request format  
      request = chatModelRequestFromMessages(messages)
      
      // Get the response
      response <- chatModel.generate(request)
      
      // Save to memory
      _ <- memory.addAssistantMessage(response.content.text)
    } yield response.content.text
  }
  
  // Helper methods
  private def chatModelRequestFromMessages(messages: List[dev.langchain4j.data.message.ChatMessage]) = {
    // Implementation details
  }
}

object LangchainAgent {
  def make(
    modelType: ModelType,
    config: ModelConfig,
    name: String = "agent",
    maxHistory: Int = 10
  ): ZIO[Any, Throwable, Agent] = {
    for {
      model <- modelType match {
        case ModelType.Claude => 
          ZIOChatModelFactory.makeClaudeModel(config.apiKey.get, config.modelName.getOrElse("claude-3-opus-20240229"))
        case ModelType.VertexAI => 
          ZIOChatModelFactory.makeVertexAIModel(
            config.projectId.get, 
            config.location.getOrElse("us-central1"), 
            config.modelName.getOrElse("gemini-1.5-pro")
          )
        // Other model types
      }
      memory <- ZIOChatMemory.createWindow(maxHistory)
    } yield LangchainAgent(model, memory, name)
  }
}
```

### Phase 3: Testing Infrastructure

#### 3.1 Test Mocks Implementation

```scala
package com.agenticai.core.llm.langchain.testing

import com.agenticai.core.llm.langchain._
import dev.langchain4j.model.chat._
import zio._
import zio.stream._

// Mock implementation for testing
class MockChatLanguageModel(responses: Map[String, String]) extends ZIOChatLanguageModel {
  override def generate(request: ChatLanguageModel.Request): ZIO[Any, Throwable, ChatLanguageModel.Response] = {
    // Extract the last user message
    val lastUserMessage = request.messages.asScala.last.text
    
    // Look up the response or provide a default
    val responseText = responses.getOrElse(lastUserMessage, "I don't know how to respond to that.")
    
    // Create a response
    ZIO.succeed(
      ChatLanguageModel.Response.from(
        dev.langchain4j.data.message.AiMessage.from(responseText)
      )
    )
  }
  
  override def generateStream(request: ChatLanguageModel.Request): ZStream[Any, Throwable, String] = {
    // For simplicity, we'll just split the response into words for streaming
    ZStream.fromZIO(generate(request))
      .flatMap { response =>
        val words = response.content.text.split(" ")
        ZStream.fromIterable(words.map(_ + " "))
      }
  }
}

object MockChatLanguageModel {
  def make(responses: Map[String, String]): ZIOChatLanguageModel =
    new MockChatLanguageModel(responses)
    
  def layer(responses: Map[String, String]): ZLayer[Any, Nothing, ZIOChatLanguageModel] =
    ZLayer.succeed(make(responses))
}
```

#### 3.2 Test Utilities

```scala
package com.agenticai.core.llm.langchain.testing

import com.agenticai.core.llm.langchain._
import zio._
import zio.test._

object LangchainTestUtils {
  // Create a test environment with mocked responses
  def testEnvironment(responses: Map[String, String]): ZLayer[Any, Nothing, ZIOChatLanguageModel] =
    MockChatLanguageModel.layer(responses)
  
  // Create a test agent with mocked responses
  def testAgent(
    responses: Map[String, String],
    name: String = "test-agent",
    maxHistory: Int = 10
  ): ZIO[Any, Nothing, Agent] = {
    for {
      model <- ZIO.succeed(MockChatLanguageModel.make(responses))
      memory <- ZIOChatMemory.createWindow(maxHistory)
    } yield LangchainAgent(model, memory, name)
  }
  
  // Test spec for verifying agent behavior
  def agentSpec(
    responses: Map[String, String],
    testCases: List[(String, String)]
  ): Spec[Any, Throwable] = {
    suite("Agent Tests")(
      testM("should respond correctly to prompts") {
        for {
          agent <- testAgent(responses)
          results <- ZIO.foreach(testCases) { case (input, expectedOutput) =>
            agent.processSync(input).map(output => (output, expectedOutput))
          }
        } yield {
          results.foreach { case (actual, expected) =>
            assertTrue(actual == expected)
          }
        }
      }
    )
  }
}
```

## Test Generation Guidelines

### Unit Test Generation Instructions

AI agents should follow these guidelines when generating unit tests:

1. **Test ZIO wrappers**:
   - Test that ZIO effects wrap Langchain4j operations correctly
   - Verify error handling and resource management
   - Use mocks for Langchain4j dependencies

2. **Test factory methods**:
   - Verify factory methods create correct model types
   - Test configuration parameter handling
   - Test error cases (missing credentials, etc.)

3. **Example unit test**:
   ```scala
   test("ZIOChatLanguageModel should wrap Langchain4j model") {
     val mockResponse = ChatLanguageModel.Response.from(AiMessage.from("Test response"))
     val mockModel = new ChatLanguageModel {
       override def generate(request: ChatLanguageModel.Request): ChatLanguageModel.Response = mockResponse
     }
     
     val zioModel = ZIOChatLanguageModel(mockModel)
     
     for {
       response <- zioModel.generate(ChatLanguageModel.Request.from(UserMessage.from("Test")))
     } yield assertTrue(response == mockResponse)
   }
   ```

### Functional Test Generation Instructions

AI agents should follow these guidelines when generating functional tests:

1. **Test agent behavior**:
   - Test conversation flow with multiple turns
   - Verify memory retention and context handling
   - Test error recovery and graceful degradation

2. **Use mock models**:
   - Create deterministic test scenarios with predefined responses
   - Test edge cases and error conditions

3. **Example functional test**:
   ```scala
   test("Agent should maintain conversation context") {
     val responses = Map(
       "Hello" -> "Hi there!",
       "What did I just say?" -> "You said 'Hello'",
       "And what did you reply?" -> "I replied with 'Hi there!'"
     )
     
     for {
       agent <- LangchainTestUtils.testAgent(responses)
       response1 <- agent.processSync("Hello")
       response2 <- agent.processSync("What did I just say?")
       response3 <- agent.processSync("And what did you reply?")
     } yield {
       assertTrue(
         response1 == "Hi there!",
         response2 == "You said 'Hello'",
         response3 == "I replied with 'Hi there!'"
       )
     }
   }
   ```

### Integration Test Generation Instructions

AI agents should follow these guidelines when generating integration tests:

1. **Test actual LLM integration**:
   - Use test API keys for real models
   - Keep tests focused on integration points
   - Ensure proper error handling

2. **Test environment configuration**:
   - Use smaller, faster models for testing
   - Cache responses when possible
   - Implement recording/replay for deterministic tests

3. **Example integration test**:
   ```scala
   test("should connect to actual Claude API") {
     for {
       model <- ZIOChatModelFactory.makeClaudeModel(testApiKey, "claude-3-haiku-20240307")
       request = createSimpleTestRequest("Hello")
       response <- model.generate(request).timeoutFail(new Exception("API timeout"))(30.seconds)
     } yield assertTrue(response.content.text.nonEmpty)
   }
   ```

## Implementation Timeline

### Phase 1: Parallel Implementation (Weeks 1-2)
- Implement ZIO wrappers for Langchain4j models
- Create factory methods for different LLM providers
- Develop test infrastructure with mocks

### Phase 2: Feature Parity (Weeks 3-4)
- Implement memory adapters
- Create agent implementations
- Develop streaming support

### Phase 3: Transition (Weeks 5-6)
- Migrate existing agents to new implementation
- Update examples to use new implementation
- Deprecate old implementation

### Phase 4: Cleanup (Weeks 7-8)
- Remove deprecated code
- Finalize documentation
- Complete test coverage

## Current Implementation Status

As of April 2025, the implementation status is:

- ✅ **Implemented**: ZIO wrappers for Langchain4j models
- ✅ **Implemented**: Factory methods for Claude and Vertex AI models
- ✅ **Implemented**: Basic memory adapters
- ✅ **Implemented**: Streaming support for chat models
- 🚧 **In Progress**: Tool support integration
- 🚧 **In Progress**: Advanced Langchain4j features
- 🔮 **Planned**: RAG (Retrieval Augmented Generation) capabilities
- 🔮 **Planned**: Agent types from Langchain4j

## Benefits

The Langchain4j integration provides several benefits:

1. **Reduced Maintenance**: Leverage a mature library for LLM interactions
2. **Improved Testing**: Better test infrastructure with mocks and deterministic tests
3. **Broader Model Support**: Access to more LLM providers through Langchain4j
4. **Future-Proofing**: Easier to adopt new LLM features as they become available
5. **Focus on Core Value**: More time to focus on agent behaviors and task execution
6. **Community Alignment**: Leverage the broader Langchain4j community

## Conclusion

The Langchain4j integration represents a strategic shift in our approach to LLM interactions. By leveraging an established library, we can focus our development efforts on the unique value of our agentic framework while benefiting from the broader ecosystem of LLM tools and capabilities.
