package com.agenticai.core.llm.langchain.test

import com.agenticai.core.llm.langchain._
import zio._
import dev.langchain4j.data.message.ChatMessage

/**
 * Utility methods for testing agents.
 */
object AgentTestUtils {
  /**
   * Creates a mock agent for testing.
   *
   * @param defaultResponse The default response for the mock agent
   * @param name A name for the agent
   * @param maxHistory The maximum number of messages to keep in history
   * @return A ZIO effect that resolves to an Agent
   */
  def createMockAgent(
    defaultResponse: String = "I am a mock AI assistant.",
    name: String = "test-agent",
    maxHistory: Int = 10
  ): ZIO[Any, Nothing, Agent] = {
    for {
      mockModel <- MockChatLanguageModel.make(Map.empty, defaultResponse)
      memory <- ZIOChatMemory.createInMemory(maxHistory * 2) // * 2 for user+assistant messages
    } yield LangchainAgent.create(mockModel, memory, name)
  }
  
  /**
   * Utility method for testing an agent with a single interaction.
   *
   * @param input The input to send to the agent
   * @param defaultResponse The default response for the mock agent
   * @return A ZIO effect that completes with the agent's response
   */
  def testSingleInteraction(
    input: String, 
    defaultResponse: String = "I am a mock AI assistant."
  ): ZIO[Any, Throwable, String] = {
    for {
      agent <- createMockAgent(defaultResponse)
      output <- agent.processSync(input)
    } yield output
  }
  
  /**
   * Creates a multi-turn conversation test with a mock agent.
   * This is useful for testing context retention across multiple turns.
   *
   * @param testName The name of the test
   * @param interactions A list of (input, expectedOutput) pairs
   * @return A ZIO effect that resolves to a boolean indicating success or failure
   */
  def testConversation(
    testName: String,
    interactions: List[(String, String)]
  ): ZIO[Any, Throwable, Boolean] = {
    for {
      agent <- createMockAgent(defaultResponse = "I am a mock AI assistant.")
      results <- ZIO.foreach(interactions) { case (input, expectedOutput) =>
        agent.processSync(input).map(output => output.contains(expectedOutput))
      }
    } yield results.forall(identity)
  }
  
  /**
   * Tests memory truncation in an agent.
   * This verifies that the agent correctly truncates conversation history when it exceeds maxHistory.
   *
   * @param testName The name of the test
   * @param inputs A large number of inputs to process sequentially
   * @param maxHistory The maximum number of turns to keep
   * @return A ZIO effect that resolves to a boolean indicating success or failure
   */
  def testMemoryTruncation(
    testName: String,
    inputs: List[String],
    maxHistory: Int
  ): ZIO[Any, Throwable, Boolean] = {
    for {
      agent <- createMockAgent(maxHistory = maxHistory)
      _ <- ZIO.foreach(inputs)(agent.processSync)
      history <- agent.getHistory
    } yield history.size <= maxHistory * 2 // * 2 for user+assistant messages
  }
}
