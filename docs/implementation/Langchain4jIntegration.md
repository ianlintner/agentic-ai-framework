# Langchain4j + ZIO Integration Design Document

## Executive Summary

This document outlines a strategy to integrate Langchain4j with our existing ZIO-based agentic AI framework. By leveraging Langchain4j for LLM interactions, we can focus our development efforts on the agentic/task components of our system while delegating LLM management to a mature library. This approach will solve several existing issues, including the TestClock challenges we're experiencing, while providing a more robust foundation for future development.

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
      memory <- ZIOChatMemory.createWindow(maxHistory * 2) // User + Assistant messages
    } yield LangchainAgent(model, memory, name)
  }
}

// Supporting types
sealed trait ModelType
object ModelType {
  case object Claude extends ModelType
  case object VertexAI extends ModelType
  // Other model types
}

case class ModelConfig(
  apiKey: Option[String] = None,
  projectId: Option[String] = None,
  location: Option[String] = None,
  modelName: Option[String] = None
)
```

### Phase 3: Testing Framework

#### 3.1 Test Mocks Implementation

```scala
package com.agenticai.core.llm.langchain.test

import com.agenticai.core.llm.langchain._
import dev.langchain4j.model.chat._
import zio._
import zio.stream._

class MockChatLanguageModel(responses: Map[String, String] = Map.empty) extends ZIOChatLanguageModel {
  private val defaultResponse = "I am a mock AI assistant."
  
  override def generate(request: ChatLanguageModel.Request): ZIO[Any, Throwable, ChatLanguageModel.Response] = {
    // Extract the last user message
    val lastUserMessage = extractLastUserMessage(request)
    
    // Look up the response or use default
    val responseText = responses.getOrElse(lastUserMessage, defaultResponse)
    
    // Create a response
    ZIO.succeed(createMockResponse(responseText))
  }
  
  override def generateStream(request: ChatLanguageModel.Request): ZStream[Any, Throwable, String] = {
    // Extract the last user message
    val lastUserMessage = extractLastUserMessage(request)
    
    // Look up the response or use default
    val responseText = responses.getOrElse(lastUserMessage, defaultResponse)
    
    // Create a stream of the response
    ZStream.succeed(responseText)
  }
  
  // Helper methods
  private def extractLastUserMessage(request: ChatLanguageModel.Request): String = {
    // Implementation details
    ""
  }
  
  private def createMockResponse(text: String): ChatLanguageModel.Response = {
    // Implementation details
    null
  }
}

object MockChatLanguageModel {
  def make(responses: Map[String, String] = Map.empty): UIO[ZIOChatLanguageModel] =
    ZIO.succeed(new MockChatLanguageModel(responses))
    
  def layer(responses: Map[String, String] = Map.empty): ZLayer[Any, Nothing, ZIOChatLanguageModel] =
    ZLayer.succeed(new MockChatLanguageModel(responses))
}
```

#### 3.2 Test Utilities

```scala
package com.agenticai.core.llm.langchain.test

import com.agenticai.core.agents.Agent
import com.agenticai.core.llm.langchain._
import zio._
import zio.test._

object AgentTestUtils {
  def createMockAgent(
    responses: Map[String, String] = Map.empty,
    name: String = "test-agent",
    maxHistory: Int = 10
  ): ZIO[Any, Nothing, Agent] = {
    for {
      mockModel <- MockChatLanguageModel.make(responses)
      memory <- ZIOChatMemory.createWindow(maxHistory * 2)
    } yield LangchainAgent(mockModel, memory, name)
  }
  
  def testAgent(
    testName: String,
    input: String,
    expectedOutput: String,
    responses: Map[String, String] = Map.empty
  ): Spec[Any, Throwable] = {
    test(testName) {
      for {
        agent <- createMockAgent(responses)
        output <- agent.processSync(input)
      } yield assertTrue(output.contains(expectedOutput))
    }
  }
}
```

## Instructions for AI Agents to Build Tests

### Unit Test Generation Instructions

AI agents should follow these guidelines when generating unit tests for the Langchain4j integration:

1. **Test each component in isolation**
   - Focus on one ZIO wrapper class at a time
   - Use mock implementations for dependencies
   - Test both success and error paths

2. **For ZIOChatLanguageModel tests**:
   ```scala
   test("should generate responses successfully") {
     for {
       mockModel <- MockChatLanguageModel.make(Map("Hello" -> "Hi there!"))
       request = createTestRequest("Hello")
       response <- mockModel.generate(request)
     } yield assertTrue(response.content.text == "Hi there!")
   }
   ```

3. **For ZIOChatMemory tests**:
   ```scala
   test("should store and retrieve messages") {
     for {
       memory <- ZIOChatMemory.createWindow(10)
       _ <- memory.addUserMessage("Hello")
       _ <- memory.addAssistantMessage("Hi there!")
       messages <- memory.messages
     } yield assertTrue(
       messages.size == 2 &&
       messages.head.isInstanceOf[UserMessage] &&
       messages(1).isInstanceOf[AiMessage]
     )
   }
   ```

4. **Use ZIO Test assertions extensively**:
   - `assertTrue`, `assertZIO`, `assertCompletes`
   - Test both success and failure cases
   - Test edge conditions (empty inputs, etc.)

### Functional Test Generation Instructions

AI agents should follow these guidelines when generating functional tests:

1. **Test end-to-end agent behavior**
   - Simulate multiple turns of conversation
   - Verify memory retention
   - Test with realistic inputs

2. **Example conversation test**:
   ```scala
   test("should maintain context across multiple turns") {
     for {
       agent <- createMockAgent(Map(
         "My name is John" -> "Nice to meet you, John!",
         "What's my name?" -> "Your name is John."
       ))
       _ <- agent.processSync("My name is John")
       response <- agent.processSync("What's my name?")
     } yield assertTrue(response.contains("John"))
   }
   ```

3. **Test with realistic scenarios**:
   - Information extraction
   - Task planning
   - Decision making

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

## Phased Migration Strategy

### Phase 1: Parallel Implementation (Weeks 1-2)
- Implement core ZIO wrappers
- Create basic test infrastructure
- Deploy alongside existing implementation

### Phase 2: Feature Parity (Weeks 3-4)
- Implement full agent functionality
- Migrate memory system
- Achieve functional parity with current system

### Phase 3: Transition (Weeks 5-6)
- Switch new code to use Langchain4j agents
- Run parallel tests to verify equivalence
- Deprecate old implementation

### Phase 4: Cleanup (Weeks 7-8)
- Remove deprecated code
- Consolidate interfaces
- Complete documentation

## Benefits

1. **Focus on Core Value**: By delegating LLM integration to Langchain4j, we can focus on developing agentic behaviors and task execution.

2. **Reduced Testing Complexity**: Simplified testing approach eliminates TestClock issues and provides more deterministic tests.

3. **Future-Proofing**: Langchain4j maintains compatibility with evolving LLM APIs, reducing our maintenance burden.

4. **Improved Developer Experience**: Clearer separation of concerns and simplified abstractions improve code readability and maintainability.

## Conclusion

Integrating Langchain4j with our ZIO-based framework allows us to leverage the strengths of both technologies: the functional programming paradigm and effect system of ZIO, combined with the mature LLM abstraction layer of Langchain4j. This strategic move will solve our existing testing issues while allowing us to focus our development resources on the agentic aspects of our framework.
