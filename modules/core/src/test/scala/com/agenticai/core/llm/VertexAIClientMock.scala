package com.agenticai.core.llm

import zio._
import zio.stream._

/**
 * Mock implementation of VertexAIClient for testing
 */
object VertexAIClientMock extends VertexAIClient {
  override def streamCompletion(prompt: String): ZStream[Any, Throwable, String] = {
    ZStream.fromIterable(Seq("This is a mock response to: ", prompt))
  }
  
  override def complete(prompt: String): Task[String] = {
    ZIO.succeed(s"This is a mock response to: $prompt")
  }
  
  override def generateText(prompt: String): Task[String] = {
    ZIO.succeed(s"This is a mock generated text for: $prompt")
  }
} 