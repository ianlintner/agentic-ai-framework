package com.agenticai.core.llm.langchain.test

import com.agenticai.core.llm.langchain._
import zio._

/**
 * Utility methods for testing agents.
 * This object provides convenience methods for creating mock agents
 * and running common test scenarios.
 */
object AgentTestUtils {
  /**
   * Creates a mock agent with predefined responses.
   *
   * @param responses A map of user messages to predefined responses
   * @param name A name for the agent
   * @param maxHistory The maximum number of conversation turns to store
   * @return A ZIO effect that resolves to an Agent
   */
  def createMockAgent(
    responses: Map[String, String] = Map.empty,
    name: String = "test-agent",
    maxHistory: Int = 10
  ): ZIO[Any, Nothing, Agent] = {
    for {
      mockModel <- MockChatLanguageModel.make(responses)
      memory <- ZIOChatMemory.createWindow(maxHistory * 2) // * 2 for user+assistant messages
    } yield LangchainAgent(mockModel, memory, name)
  }
  
  /**
   * Creates a single-turn test with a mock agent.
   * This is a convenience method for simple tests that just need to check
   * if an agent produces the expected output for a given input.
   *
   * @param testName The name of the test
   * @param input The user input to test
   * @param expectedOutput The expected output (or a substring)
   * @param responses A map of user messages to predefined responses
   * @return A ZIO Test spec
   */
  def testAgent(
    testName: String,
    input: String,
    expectedOutput: String,
    responses: Map[String, String] = Map.empty
  ): ZIO[Any, Throwable, Boolean] = {
    for {
      agent <- createMockAgent(responses)
      output <- agent.processSync(input)
    } yield output.contains(expectedOutput)
  }
  
  /**
   * Creates a multi-turn conversation test with a mock agent.
   * This is useful for testing context retention across multiple turns.
   *
   * @param testName The name of the test
   * @param interactions A list of (input, expectedOutput) pairs
   * @param responses A map of user messages to predefined responses
   * @return A ZIO Test spec
   */
  def testConversation(
    testName: String,
    interactions: List[(String, String)],
    responses: Map[String, String] = Map.empty
  ): ZIO[Any, Throwable, Boolean] = {
    for {
      agent <- createMockAgent(responses)
      results <- ZIO.foreach(interactions) { case (input, expectedOutput) =>
        agent.processSync(input).map(output => output.contains(expectedOutput))
      }
    } yield results.forall(identity)
  }
  
  /**
   * Tests memory retention in an agent.
   * This verifies that the agent correctly stores and retrieves conversation history.
   *
   * @param testName The name of the test
   * @param interactions A list of user inputs to process sequentially
   * @param expectedHistorySize The expected number of messages in history after all interactions
   * @return A ZIO Test spec
   */
  def testMemoryRetention(
    testName: String,
    interactions: List[String],
    expectedHistorySize: Int
  ): ZIO[Any, Throwable, Boolean] = {
    for {
      agent <- createMockAgent()
      _ <- ZIO.foreach(interactions)(agent.processSync)
      history <- agent.getHistory
    } yield history.size == expectedHistorySize
  }
  
  /**
   * Tests memory truncation in an agent.
   * This verifies that the agent correctly truncates conversation history when it exceeds maxHistory.
   *
   * @param testName The name of the test
   * @param inputs A large number of inputs to process sequentially
   * @param maxHistory The maximum number of turns to keep
   * @return A ZIO Test spec
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
