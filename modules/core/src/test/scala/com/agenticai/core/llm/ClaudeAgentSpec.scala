package com.agenticai.core.llm

import zio.*
import zio.test.*
import zio.test.Assertion.*
import zio.test.TestAspect.*
import com.agenticai.core.memory.{MemorySystem, InMemorySystem}
import zio.stream.ZStream
import java.time.Instant

object ClaudeAgentSpec extends ZIOSpecDefault {
  def spec = suite("ClaudeAgent")(
    test("should handle basic prompts") {
      for {
        agent <- ClaudeAgent.make(VertexAIConfig.claudeDefault)
        result <- agent.process("test input").runHead.someOrFail(new RuntimeException("No result"))
      } yield assertTrue(result.nonEmpty)
    } @@ TestAspect.withLiveClock,
    
    test("handles errors") {
      val testError = new RuntimeException("test")
      val failingClient = new VertexAIClient {
        def generateText(prompt: String) = ZIO.fail(testError)
        def complete(prompt: String) = ZIO.fail(testError)
        def streamCompletion(prompt: String) = ZStream.fail(testError)
      }
      
      for {
        memory <- ZIO.service[MemorySystem]
        agent = new ClaudeAgent("test", failingClient, memory)
        result <- agent.process("test").runHead.exit
      } yield assertTrue(result.isFailure)
    }
  ).provide(
    ZLayer.succeed[MemorySystem](new InMemorySystem()),
    Scope.default
  ) @@ sequential
}