package com.agenticai.core.llm.langchain

import com.agenticai.core.llm.langchain.util.IntegrationTestConfig
import zio._
import zio.test._
import zio.test.Assertion._
import zio.test.TestAspect._

/**
 * Integration tests for the Claude model integration.
 * These tests verify that the Langchain4j Claude integration works correctly
 * with actual API calls.
 *
 * To run these tests, you need to set the CLAUDE_API_KEY environment variable.
 * Tests will be skipped if the API key is not available.
 */
object ClaudeIntegrationSpec extends ZIOSpecDefault {
  // Skip all tests if API key not available
  private val shouldRunTests = IntegrationTestConfig.shouldRunClaudeTests

  def spec = suite("Claude Integration Tests")(
    test("should connect to Claude API and get a response") {
      for {
        apiKey <- ZIO.fromOption(IntegrationTestConfig.claudeApiKey)
                    .orElseFail(new Exception("CLAUDE_API_KEY not set"))
        model <- ZIOChatModelFactory.makeModel(
                   ZIOChatModelFactory.ModelType.Claude,
                   ZIOChatModelFactory.ModelConfig(
                     apiKey = Some(apiKey),
                     modelName = Some(IntegrationTestConfig.claudeModelName)
                   )
                 )
        memory <- ZIOChatMemory.createInMemory(10)
        agent = LangchainAgent(model, memory, "test-claude-agent")
        response <- agent.processSync(IntegrationTestConfig.simplePrompt)
      } yield assertTrue(
        response.nonEmpty && 
        response.toLowerCase.contains("paris")
      )
    },

    test("should stream responses from Claude API") {
      for {
        apiKey <- ZIO.fromOption(IntegrationTestConfig.claudeApiKey)
                    .orElseFail(new Exception("CLAUDE_API_KEY not set"))
        model <- ZIOChatModelFactory.makeModel(
                   ZIOChatModelFactory.ModelType.Claude,
                   ZIOChatModelFactory.ModelConfig(
                     apiKey = Some(apiKey),
                     modelName = Some(IntegrationTestConfig.claudeModelName)
                   )
                 )
        memory <- ZIOChatMemory.createInMemory(10)
        agent = LangchainAgent(model, memory, "test-claude-agent")
        chunks <- agent.process(IntegrationTestConfig.countingPrompt).runCollect
      } yield assertTrue(
        chunks.nonEmpty
      )
    },

    test("should handle multi-turn conversations") {
      for {
        apiKey <- ZIO.fromOption(IntegrationTestConfig.claudeApiKey)
                    .orElseFail(new Exception("CLAUDE_API_KEY not set"))
        model <- ZIOChatModelFactory.makeModel(
                   ZIOChatModelFactory.ModelType.Claude,
                   ZIOChatModelFactory.ModelConfig(
                     apiKey = Some(apiKey),
                     modelName = Some(IntegrationTestConfig.claudeModelName)
                   )
                 )
        memory <- ZIOChatMemory.createInMemory(10)
        agent = LangchainAgent(model, memory, "test-claude-agent")
        _ <- agent.processSync("My name is Test User.")
        response <- agent.processSync("What's my name?")
      } yield assertTrue(
        response.nonEmpty && 
        response.toLowerCase.contains("test user")
      )
    },

    test("should handle errors gracefully") {
      for {
        // Use invalid API key
        model <- ZIOChatModelFactory.makeModel(
                   ZIOChatModelFactory.ModelType.Claude,
                   ZIOChatModelFactory.ModelConfig(
                     apiKey = Some("invalid-api-key"),
                     modelName = Some(IntegrationTestConfig.claudeModelName)
                   )
                 )
        memory <- ZIOChatMemory.createInMemory(10)
        agent = LangchainAgent(model, memory, "test-claude-agent")
        result <- agent.processSync(IntegrationTestConfig.simplePrompt).exit
      } yield assertTrue(result.isFailure)
    }
  ) @@ (if (shouldRunTests) identity else ignore) @@ timeout(2.minutes)
}
