package com.agenticai.core.llm.langchain

import com.agenticai.core.llm.langchain.util.IntegrationTestConfig
import zio._
import zio.test._
import zio.test.Assertion._
import zio.test.TestAspect._

/**
 * Integration tests for the Google AI Gemini model integration.
 * These tests verify that the Langchain4j Google AI integration works correctly
 * with actual API calls.
 *
 * To run these tests, you need to set the GOOGLE_API_KEY environment variable.
 * Tests will be skipped if the API key is not available.
 */
object GoogleAIGeminiIntegrationSpec extends ZIOSpecDefault {
  // Skip all tests if API key not available
  private val shouldRunTests = IntegrationTestConfig.shouldRunGoogleTests

  def spec = suite("Google AI Gemini Integration Tests")(
    test("should connect to Google AI and get a response") {
      for {
        apiKey <- ZIO.fromOption(IntegrationTestConfig.googleApiKey)
                    .orElseFail(new Exception("GOOGLE_API_KEY not set"))
        model <- ZIOChatModelFactory.makeModel(
                   ZIOChatModelFactory.ModelType.GoogleAIGemini,
                   ZIOChatModelFactory.ModelConfig(
                     apiKey = Some(apiKey),
                     modelName = Some(IntegrationTestConfig.googleModelName)
                   )
                 )
        memory <- ZIOChatMemory.createInMemory(10)
        agent = LangchainAgent(model, memory, "test-google-agent")
        response <- agent.processSync(IntegrationTestConfig.simplePrompt)
      } yield assertTrue(
        response.nonEmpty && 
        response.toLowerCase.contains("paris")
      )
    },

    test("should stream responses from Google AI") {
      for {
        apiKey <- ZIO.fromOption(IntegrationTestConfig.googleApiKey)
                    .orElseFail(new Exception("GOOGLE_API_KEY not set"))
        model <- ZIOChatModelFactory.makeModel(
                   ZIOChatModelFactory.ModelType.GoogleAIGemini,
                   ZIOChatModelFactory.ModelConfig(
                     apiKey = Some(apiKey),
                     modelName = Some(IntegrationTestConfig.googleModelName)
                   )
                 )
        memory <- ZIOChatMemory.createInMemory(10)
        agent = LangchainAgent(model, memory, "test-google-agent")
        chunks <- agent.process(IntegrationTestConfig.countingPrompt).runCollect
      } yield assertTrue(
        chunks.nonEmpty
      )
    },

    test("should handle multi-turn conversations") {
      for {
        apiKey <- ZIO.fromOption(IntegrationTestConfig.googleApiKey)
                    .orElseFail(new Exception("GOOGLE_API_KEY not set"))
        model <- ZIOChatModelFactory.makeModel(
                   ZIOChatModelFactory.ModelType.GoogleAIGemini,
                   ZIOChatModelFactory.ModelConfig(
                     apiKey = Some(apiKey),
                     modelName = Some(IntegrationTestConfig.googleModelName)
                   )
                 )
        memory <- ZIOChatMemory.createInMemory(10)
        agent = LangchainAgent(model, memory, "test-google-agent")
        _ <- agent.processSync("My name is Test User.")
        response <- agent.processSync("What's my name?")
      } yield assertTrue(
        response.nonEmpty && 
        response.toLowerCase.contains("test user")
      )
    },

    test("should handle reasoning tasks") {
      for {
        apiKey <- ZIO.fromOption(IntegrationTestConfig.googleApiKey)
                    .orElseFail(new Exception("GOOGLE_API_KEY not set"))
        model <- ZIOChatModelFactory.makeModel(
                   ZIOChatModelFactory.ModelType.GoogleAIGemini,
                   ZIOChatModelFactory.ModelConfig(
                     apiKey = Some(apiKey),
                     modelName = Some(IntegrationTestConfig.googleModelName)
                   )
                 )
        memory <- ZIOChatMemory.createInMemory(10)
        agent = LangchainAgent(model, memory, "test-google-agent")
        response <- agent.processSync(IntegrationTestConfig.reasoningPrompt)
      } yield assertTrue(
        response.nonEmpty && 
        (response.contains("120") || response.toLowerCase.contains("120 miles"))
      )
    },

    test("should handle errors gracefully") {
      for {
        // Use invalid API key
        model <- ZIOChatModelFactory.makeModel(
                   ZIOChatModelFactory.ModelType.GoogleAIGemini,
                   ZIOChatModelFactory.ModelConfig(
                     apiKey = Some("invalid-api-key"),
                     modelName = Some(IntegrationTestConfig.googleModelName)
                   )
                 )
        memory <- ZIOChatMemory.createInMemory(10)
        agent = LangchainAgent(model, memory, "test-google-agent")
        result <- agent.processSync(IntegrationTestConfig.simplePrompt).exit
      } yield assertTrue(result.isFailure)
    }
  ) @@ (if (shouldRunTests) identity else ignore) @@ timeout(2.minutes)
}
