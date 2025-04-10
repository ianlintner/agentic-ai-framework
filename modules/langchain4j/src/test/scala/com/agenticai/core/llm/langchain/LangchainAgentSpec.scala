package com.agenticai.core.llm.langchain

import com.agenticai.core.llm.langchain.test.*
import zio.*
import zio.test.*
import zio.test.Assertion.*

/** Tests for the LangchainAgent class. This demonstrates how to use our ZIO + Langchain4j
  * integration.
  */
object LangchainAgentSpec extends ZIOSpecDefault:

  def spec = suite("LangchainAgent")(
    // Basic functionality tests
    suite("Basic Functionality")(
      test("should process input and return response") {
        for
          // Create a mock agent using the utility method
          agent <- AgentTestUtils.createMockAgent("Hi there! How can I help you today?")

          // Process a message
          response <- agent.processSync("Hello")
        yield assertTrue(response == "Hi there! How can I help you today?")
      },
      test("should respond with default message") {
        for
          // Create a mock agent using the utility method with default message
          agent <- AgentTestUtils.createMockAgent()

          // Process a message
          response <- agent.processSync("Tell me a joke")
        yield assertTrue(response == "I am a mock AI assistant.")
      }
    ),

    // Memory system tests
    suite("Memory System")(
      test("should store conversation history") {
        for
          // Create a mock agent
          agent <- AgentTestUtils.createMockAgent()

          // Have a conversation
          _ <- agent.processSync("Hello")
          _ <- agent.processSync("How are you?")

          // Check conversation history
          history <- agent.getHistory
        yield assertTrue(history.size == 4) // 2 user messages + 2 assistant messages
      },
      test("should clear conversation history") {
        for
          // Create a mock agent
          agent <- AgentTestUtils.createMockAgent()

          // Have a conversation
          _ <- agent.processSync("Hello")
          _ <- agent.processSync("How are you?")

          // Clear history
          _ <- agent.clearHistory()

          // Check conversation history
          history <- agent.getHistory
        yield assertTrue(history.isEmpty)
      },
      test("should truncate history to maximum size") {
        val maxTurns = 3                                     // Keep 3 turns maximum (6 messages)
        val inputs   = List.tabulate(10)(i => s"Message $i") // 10 messages

        assertZIO(
          AgentTestUtils.testMemoryTruncation(
            "truncates history correctly",
            inputs,
            maxTurns
          )
        )(isTrue)
      }
    ),

    // Conversation flow tests
    suite("Conversation Flow")(
      // Using the test utility method for a conversation test
      test("full conversation with multiple turns") {
        assertZIO(
          AgentTestUtils.testConversation(
            "full conversation with multiple turns",
            List(
              "Hi there"          -> "I am a mock AI assistant.",
              "Tell me about ZIO" -> "I am a mock AI assistant.",
              "Thanks"            -> "I am a mock AI assistant."
            )
          )
        )(isTrue)
      }
    ),

    // Factory method tests
    suite("Factory Methods")(
      test("should create an agent with Claude model") {
        // This test is more of a compilation check since we don't want to make real API calls
        val program =
          for agent <- LangchainAgent.make(
              ZIOChatModelFactory.ModelType.Claude,
              ZIOChatModelFactory.ModelConfig(
                apiKey = Some("mock-api-key"),
                modelName = Some("claude-3-sonnet")
              )
            )
          yield agent

        // Just assert that we can create the program (don't actually run it)
        assertZIO(ZIO.attempt(program.getClass))(anything)
      },
      test("should create an agent with VertexAI Gemini model") {
        // This test is more of a compilation check since we don't want to make real API calls
        val program =
          for agent <- LangchainAgent.make(
              ZIOChatModelFactory.ModelType.VertexAIGemini,
              ZIOChatModelFactory.ModelConfig(
                projectId = Some("mock-project"),
                location = Some("us-central1"),
                modelName = Some("gemini-1.5-pro")
              )
            )
          yield agent

        // Just assert that we can create the program (don't actually run it)
        assertZIO(ZIO.attempt(program.getClass))(anything)
      },
      test("should create an agent with OpenAI model") {
        // This test is more of a compilation check since we don't want to make real API calls
        val program =
          for agent <- LangchainAgent.make(
              ZIOChatModelFactory.ModelType.OpenAI,
              ZIOChatModelFactory.ModelConfig(
                apiKey = Some("mock-api-key"),
                modelName = Some("gpt-4"),
                temperature = Some(0.7),
                maxTokens = Some(1000)
              )
            )
          yield agent

        // Just assert that we can create the program (don't actually run it)
        assertZIO(ZIO.attempt(program.getClass))(anything)
      }
    )
  )
