package com.agenticai.core.llm

import zio.*
import zio.stream.*

/** Mock implementation of VertexAIClient for testing
  * 
  * This provides a simple mock implementation that can be used in tests or examples
  * without requiring actual Google Cloud credentials.
  */
object VertexAIClientMock:

  /** Create a mock VertexAIClient that returns predefined responses
    */
  def make(
      projectId: String = "mock-project",
      location: String = "us-central1",
      modelId: String = "claude-3-sonnet-20240229"
  ): VertexAIClient =
    val config = VertexAIConfig(
      projectId = projectId,
      location = location,
      modelId = modelId
    )
    
    new VertexAIClient(config):
      override def complete(prompt: String): ZIO[Any, Throwable, String] =
        ZIO.succeed(
          s"Mock response to: $prompt\n\nThis is a simulated response from the Vertex AI client mock implementation."
        )

      override def streamCompletion(prompt: String): ZStream[Any, Throwable, String] =
        ZStream
          .fromZIO(complete(prompt))
          .flatMap(response => ZStream.fromIterable(response.split("(?<=\\s)|(?=\\s)")))