package com.agenticai.core.llm

import zio._
import zio.stream._
import zio.test._
import zio.test.Assertion._
import zio.test.TestAspect._

/**
 * Tests for the Vertex AI client integration
 */
object VertexAIClientSpec extends ZIOSpecDefault {
  
  // Mock client for testing without real API
  private def createMockClient = {
    // Create a mock client with default configuration
    new VertexAIClient(VertexAIConfig.claudeDefault.copy(projectId = "test-project")) {
      override def complete(prompt: String): ZIO[Any, Throwable, String] = {
        ZIO.succeed(s"Mock response for: $prompt")
      }
      
      override def streamCompletion(prompt: String): ZStream[Any, Throwable, String] = {
        val response = s"Mock streaming response for: $prompt"
        ZStream.fromIterable(response.split(" ").map(_ + " "))
      }
    }
  }
  
  // Test environment detector
  private lazy val isLiveTest = false
  
  def spec = suite("VertexAIClient")(
    // Configuration tests
    suite("Configuration")(
      test("should initialize with default settings") {
        val config = VertexAIConfig.claudeDefault.copy(projectId = "test-project")
        val client = new VertexAIClient(config)
        assertTrue(client != null)
      },
      
      test("should use different configurations correctly") {
        val standardConfig = VertexAIConfig.claudeDefault.copy(projectId = "test-project")
        val highThroughputConfig = VertexAIConfig.highThroughput.copy(projectId = "test-project")
        val lowLatencyConfig = VertexAIConfig.lowLatency.copy(projectId = "test-project")
        
        assertTrue(
          standardConfig.modelId == "claude-3-sonnet-20240229" &&
          lowLatencyConfig.modelId == "claude-3-haiku-20240307" &&
          lowLatencyConfig.maxOutputTokens < standardConfig.maxOutputTokens
        )
      }
    ),
    
    // Basic functionality tests
    suite("Basic Functionality")(
      test("should handle completion requests with mock") {
        val client = createMockClient
        
        for {
          result <- client.complete("Hello")
        } yield assertTrue(result.contains("Hello"))
      },
      
      test("should handle streaming with mock") {
        val client = createMockClient
        
        for {
          results <- client.streamCompletion("Hello").runCollect
          // Pre-compute all boolean checks outside of the assertion entirely
          hasElements = results.nonEmpty
          // Check content without using mkString (avoiding macro expansion)
          containsHello = {
            var found = false
            for (element <- results) {
              if (element.contains("Hello")) found = true
            }
            found
          }
        } yield {
          // Use two separate assertions to completely avoid complex macro expansion
          assertTrue(hasElements) && assertTrue(containsHello)
        }
      }
    ),
    
    // Live API tests (ignored by default)
    suite("Live API")(
      test("should complete with real API") {
        for {
          projectIdOpt <- System.env("GOOGLE_CLOUD_PROJECT")
          projectId = projectIdOpt.getOrElse("test-project")
          config = VertexAIConfig.claudeDefault.copy(projectId = projectId)
          client = new VertexAIClient(config)
          result <- client.complete("Hello, my name is Claude.").either
        } yield assertTrue(
          if (projectId == "test-project") result.isLeft
          else result.isRight && result.toOption.get.nonEmpty
        )
      }
    ) @@ (if (isLiveTest) identity else ignore)
  ) @@ TestAspect.timeout(30.seconds)
}
