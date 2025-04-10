package com.agenticai.core.llm

import zio.*
import zio.stream.*

/** Mock implementation of VertexAIClient for testing
  */
object VertexAIClientMock extends VertexAIClient(VertexAIConfig.claudeDefault):

  override def streamCompletion(prompt: String): ZStream[Any, Throwable, String] =
    ZStream.fromIterable(Seq("This is a mock response to: ", prompt))

  override def complete(prompt: String): Task[String] =
    ZIO.succeed(s"This is a mock response to: $prompt")

  // Additional method not in the parent class
  def generateText(prompt: String): Task[String] =
    ZIO.succeed(s"This is a mock generated text for: $prompt")
