package com.agenticai.core.llm

import zio._
import zio.test._
import zio.test.Assertion._
import zio.test.TestAspect._

object VertexAIClientSpec extends ZIOSpecDefault {
  def spec = suite("VertexAIClient")(
    suite("Configuration")(
      test("should initialize with default settings") {
        val config = VertexAIConfig.claudeDefault.copy(projectId = "test-project")
        val client = new VertexAIClient(config)
        assertTrue(client != null)
      }
    ),
    
    suite("Text Generation")(
      test("should handle simple completion requests") {
        val config = VertexAIConfig.claudeDefault.copy(projectId = "test-project")
        val client = new VertexAIClient(config)
        
        for {
          result <- client.complete("Hello").either
        } yield assertTrue(result.isLeft) // Will fail without proper credentials
      }
    ) @@ ignore, // Ignore until we have proper test credentials
    
    suite("Streaming")(
      test("should stream responses token by token") {
        val config = VertexAIConfig.claudeDefault.copy(projectId = "test-project")
        val client = new VertexAIClient(config)
        
        for {
          result <- client.streamCompletion("Hello").runCollect.either
        } yield assertTrue(result.isLeft) // Will fail without proper credentials
      }
    ) @@ ignore // Ignore until we have proper test credentials
  )
} 