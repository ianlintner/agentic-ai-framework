package com.agenticai.core.llm.langchain

import com.agenticai.core.llm.langchain.util.IntegrationTestConfig
import zio._
import zio.test._
import zio.test.Assertion._
import zio.test.TestAspect._

/**
 * Integration tests for the Vertex AI Gemini model integration.
 * These tests verify that the Langchain4j Vertex AI integration works correctly
 * with actual API calls.
 *
 * To run these tests, you need to set the GOOGLE_CLOUD_PROJECT environment variable
 * and have valid Google Cloud credentials configured.
 * Tests will be skipped if the project ID is not available.
 */
object VertexAIGeminiIntegrationSpec extends ZIOSpecDefault {
  // Skip all tests if project ID not available
  private val shouldRunTests = IntegrationTestConfig.shouldRunVertexTests

  def spec = suite("Vertex AI Gemini Integration Tests")(
    test("should connect to Vertex AI and get a response") {
      for {
        projectId <- ZIO.fromOption(IntegrationTestConfig.vertexProjectId)
                       .orElseFail(new Exception("GOOGLE_CLOUD_PROJECT not set"))
        model <- ZIOChatModelFactory.makeModel(
                   ZIOChatModelFactory.ModelType.VertexAIGemini,
                   ZIOChatModelFactory.ModelConfig(
                     projectId = Some(projectId),
                     location = Some(IntegrationTestConfig.vertexLocation),
                     modelName = Some(IntegrationTestConfig.vertexModelName)
                   )
                 )
        memory <- ZIOChatMemory.createInMemory(10)
        agent = LangchainAgent(model, memory, "test-vertex-agent")
        response <- agent.processSync(IntegrationTestConfig.simplePrompt)
      } yield assertTrue(
        response.nonEmpty && 
        response.toLowerCase.contains("paris")
      )
    },

    test("should stream responses from Vertex AI") {
      for {
        projectId <- ZIO.fromOption(IntegrationTestConfig.vertexProjectId)
                       .orElseFail(new Exception("GOOGLE_CLOUD_PROJECT not set"))
        model <- ZIOChatModelFactory.makeModel(
                   ZIOChatModelFactory.ModelType.VertexAIGemini,
                   ZIOChatModelFactory.ModelConfig(
                     projectId = Some(projectId),
                     location = Some(IntegrationTestConfig.vertexLocation),
                     modelName = Some(IntegrationTestConfig.vertexModelName)
                   )
                 )
        memory <- ZIOChatMemory.createInMemory(10)
        agent = LangchainAgent(model, memory, "test-vertex-agent")
        chunks <- agent.process(IntegrationTestConfig.countingPrompt).runCollect
      } yield assertTrue(
        chunks.nonEmpty,
        chunks.mkString("").contains("1")
      )
    },

    test("should handle multi-turn conversations") {
      for {
        projectId <- ZIO.fromOption(IntegrationTestConfig.vertexProjectId)
                       .orElseFail(new Exception("GOOGLE_CLOUD_PROJECT not set"))
        model <- ZIOChatModelFactory.makeModel(
                   ZIOChatModelFactory.ModelType.VertexAIGemini,
                   ZIOChatModelFactory.ModelConfig(
                     projectId = Some(projectId),
                     location = Some(IntegrationTestConfig.vertexLocation),
                     modelName = Some(IntegrationTestConfig.vertexModelName)
                   )
                 )
        memory <- ZIOChatMemory.createInMemory(10)
        agent = LangchainAgent(model, memory, "test-vertex-agent")
        _ <- agent.processSync("My name is Test User.")
        response <- agent.processSync("What's my name?")
      } yield assertTrue(
        response.nonEmpty && 
        response.toLowerCase.contains("test user")
      )
    },

    test("should handle reasoning tasks") {
      for {
        projectId <- ZIO.fromOption(IntegrationTestConfig.vertexProjectId)
                       .orElseFail(new Exception("GOOGLE_CLOUD_PROJECT not set"))
        model <- ZIOChatModelFactory.makeModel(
                   ZIOChatModelFactory.ModelType.VertexAIGemini,
                   ZIOChatModelFactory.ModelConfig(
                     projectId = Some(projectId),
                     location = Some(IntegrationTestConfig.vertexLocation),
                     modelName = Some(IntegrationTestConfig.vertexModelName)
                   )
                 )
        memory <- ZIOChatMemory.createInMemory(10)
        agent = LangchainAgent(model, memory, "test-vertex-agent")
        response <- agent.processSync(IntegrationTestConfig.reasoningPrompt)
      } yield assertTrue(
        response.nonEmpty && 
        (response.contains("120") || response.toLowerCase.contains("120 miles"))
      )
    },

    test("should handle errors gracefully") {
      for {
        // Use invalid project ID
        model <- ZIOChatModelFactory.makeModel(
                   ZIOChatModelFactory.ModelType.VertexAIGemini,
                   ZIOChatModelFactory.ModelConfig(
                     projectId = Some("invalid-project-id"),
                     location = Some(IntegrationTestConfig.vertexLocation),
                     modelName = Some(IntegrationTestConfig.vertexModelName)
                   )
                 )
        memory <- ZIOChatMemory.createInMemory(10)
        agent = LangchainAgent(model, memory, "test-vertex-agent")
        result <- agent.processSync(IntegrationTestConfig.simplePrompt).exit
      } yield assertTrue(result.isFailure)
    }
  ) @@ (if (shouldRunTests) identity else ignore) @@ timeout(2.minutes)
}
