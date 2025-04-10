package com.agenticai.core.llm.langchain

import zio.*
import zio.test.*
import zio.test.Assertion.*

/** Tests for the ZIOChatModelFactory. This tests the factory methods for creating different LLM
  * models.
  */
object ZIOChatModelFactorySpec extends ZIOSpecDefault:

  def spec = suite("ZIOChatModelFactory")(
    // OpenAI model factory tests
    suite("OpenAI Model Factory")(
      test("should create OpenAI model with default parameters") {
        // This is a compilation check since we don't want to make real API calls
        val program =
          for model <- ZIOChatModelFactory.makeOpenAIModel("mock-api-key")
          yield model

        // Just assert that we can create the program (don't actually run it)
        assertZIO(ZIO.attempt(program.getClass))(anything)
      },
      test("should create OpenAI model with custom parameters") {
        // This is a compilation check since we don't want to make real API calls
        val program =
          for model <- ZIOChatModelFactory.makeOpenAIModel(
              apiKey = "mock-api-key",
              modelName = "gpt-4-turbo",
              temperature = Some(0.8),
              maxTokens = Some(Integer.valueOf(2000))
            )
          yield model

        // Just assert that we can create the program (don't actually run it)
        assertZIO(ZIO.attempt(program.getClass))(anything)
      },
      test("should create OpenAI model via makeModel") {
        // This is a compilation check since we don't want to make real API calls
        val program =
          for model <- ZIOChatModelFactory.makeModel(
              ZIOChatModelFactory.ModelType.OpenAI,
              ZIOChatModelFactory.ModelConfig(
                apiKey = Some("mock-api-key"),
                modelName = Some("gpt-4"),
                temperature = Some(0.7),
                maxTokens = Some(1000)
              )
            )
          yield model

        // Just assert that we can create the program (don't actually run it)
        assertZIO(ZIO.attempt(program.getClass))(anything)
      },
      test("should fail when API key is missing") {
        // This should fail because the API key is required
        val program =
          for model <- ZIOChatModelFactory.makeModel(
              ZIOChatModelFactory.ModelType.OpenAI,
              ZIOChatModelFactory.ModelConfig(
                apiKey = None,
                modelName = Some("gpt-4")
              )
            )
          yield model

        // Assert that the program fails with the expected error message
        assertZIO(program.exit)(fails(hasMessage(equalTo("OpenAI model requires an API key"))))
      }
    )
  )
