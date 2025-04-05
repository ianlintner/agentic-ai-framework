package com.agenticai.core.llm.langchain

import com.agenticai.core.llm.langchain.test._
import zio._
import zio.test._
import zio.test.Assertion._

/**
 * Tests for the LangchainAgent class.
 * This demonstrates how to use our ZIO + Langchain4j integration.
 */
object LangchainAgentSpec extends ZIOSpecDefault {
  def spec = suite("LangchainAgent")(
    // Basic functionality tests
    suite("Basic Functionality")(
      test("should process input and return response") {
        for {
          // Create a mock model with a predefined response
          mockModel <- MockChatLanguageModel.make(Map(
            "Hello" -> "Hi there! How can I help you today?"
          ))
          
          // Create memory
          memory <- ZIOChatMemory.createWindow(10)
          
          // Create the agent
          agent = LangchainAgent(mockModel, memory, "test-agent")
          
          // Process a message
          response <- agent.processSync("Hello")
        } yield assertTrue(response == "Hi there! How can I help you today?")
      },
      
      test("should stream response") {
        for {
          // Create a mock model with a predefined response
          mockModel <- MockChatLanguageModel.make(Map(
            "Tell me a joke" -> "Why did the chicken cross the road? To get to the other side!"
          ))
          
          // Create memory
          memory <- ZIOChatMemory.createWindow(10)
          
          // Create the agent
          agent = LangchainAgent(mockModel, memory, "test-agent")
          
          // Process a message as a stream and collect all chunks
          chunks <- agent.process("Tell me a joke").runCollect
          fullResponse = chunks.mkString
        } yield assertTrue(fullResponse == "Why did the chicken cross the road? To get to the other side!")
      }
    ),
    
    // Memory system tests
    suite("Memory System")(
      test("should store conversation history") {
        for {
          // Create a mock agent
          agent <- AgentTestUtils.createMockAgent()
          
          // Have a conversation
          _ <- agent.processSync("Hello")
          _ <- agent.processSync("How are you?")
          
          // Check conversation history
          history <- agent.getHistory
        } yield assertTrue(history.size == 4) // 2 user messages + 2 assistant messages
      },
      
      test("should clear conversation history") {
        for {
          // Create a mock agent
          agent <- AgentTestUtils.createMockAgent()
          
          // Have a conversation
          _ <- agent.processSync("Hello")
          _ <- agent.processSync("How are you?")
          
          // Clear history
          _ <- agent.clearHistory
          
          // Check conversation history
          history <- agent.getHistory
        } yield assertTrue(history.isEmpty)
      },
      
      test("should truncate history to maximum size") {
        val maxTurns = 3 // Keep 3 turns maximum (6 messages)
        val inputs = List.tabulate(10)(i => s"Message $i") // 10 messages
        
        assertZIO(AgentTestUtils.testMemoryTruncation(
          "truncates history correctly",
          inputs,
          maxTurns
        ))(isTrue)
      }
    ),
    
    // Conversation flow tests
    suite("Conversation Flow")(
      test("should maintain context across multiple turns") {
        // Predefined responses that reference previous messages
        val responses = Map(
          "My name is John" -> "Nice to meet you, John!",
          "What's my name?" -> "Your name is John."
        )
        
        // Test a multi-turn conversation
        for {
          agent <- AgentTestUtils.createMockAgent(responses)
          resp1 <- agent.processSync("My name is John")
          resp2 <- agent.processSync("What's my name?")
        } yield assertTrue(
          resp1 == "Nice to meet you, John!" &&
          resp2 == "Your name is John."
        )
      },
      
      // Using the test utility method for a conversation test
      test("full conversation with multiple turns") {
        assertZIO(AgentTestUtils.testConversation(
          "full conversation with multiple turns",
          List(
            "Hi there" -> "I am a mock AI assistant.",
            "Tell me about ZIO" -> "I am a mock AI assistant.",
            "Thanks" -> "I am a mock AI assistant."
          )
        ))(isTrue)
      }
    ),
    
    // Factory method tests
    suite("Factory Methods")(
      test("should create an agent with Claude model") {
        // This test is more of a compilation check since we don't want to make real API calls
        val program = for {
          agent <- LangchainAgent.make(
            ZIOChatModelFactory.ModelType.Claude,
            ZIOChatModelFactory.ModelConfig(
              apiKey = Some("mock-api-key"),
              modelName = Some("claude-3-sonnet")
            )
          )
        } yield agent
        
        // Just assert that we can create the program (don't actually run it)
        assertZIO(ZIO.attempt(program.getClass))(anything)
      },
      
      test("should create an agent with VertexAI Gemini model") {
        // This test is more of a compilation check since we don't want to make real API calls
        val program = for {
          agent <- LangchainAgent.make(
            ZIOChatModelFactory.ModelType.VertexAIGemini,
            ZIOChatModelFactory.ModelConfig(
              projectId = Some("mock-project"),
              location = Some("us-central1"),
              modelName = Some("gemini-1.5-pro")
            )
          )
        } yield agent
        
        // Just assert that we can create the program (don't actually run it)
        assertZIO(ZIO.attempt(program.getClass))(anything)
      }
    )
  )
}
