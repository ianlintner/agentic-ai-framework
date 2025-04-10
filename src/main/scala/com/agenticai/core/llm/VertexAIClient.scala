package com.agenticai.core.llm

import zio.*
import zio.stream.*

/** Client for interacting with Google Vertex AI
  */
/** Mock client for interacting with Google Vertex AI
  *
  * This is a simplified mock implementation that doesn't require the Google Cloud dependencies. For
  * the actual implementation, see the version in the core module.
  */
class VertexAIClient(config: VertexAIConfig):

  /** Complete a prompt with the model (non-streaming)
    */
  /** Complete a prompt with the model (non-streaming)
    */
  def complete(prompt: String): ZIO[Any, Throwable, String] =
    // This is a mock implementation that returns a predefined response
    ZIO.succeed(
      s"Mock response to: $prompt\n\nThis is a simulated response from the Vertex AI client mock implementation. For actual functionality, use the implementation in the core module."
    )

  /** Stream a completion token by token from the model Since we don't have access to the actual
    * streaming API in our current setup, we'll simulate streaming by splitting the complete
    * response
    */
  /** Stream a completion token by token from the model This mock implementation splits the mock
    * response into tokens
    */
  def streamCompletion(prompt: String): ZStream[Any, Throwable, String] =
    ZStream
      .fromZIO(complete(prompt))
      .flatMap(response => ZStream.fromIterable(response.split("(?<=\\s)|(?=\\s)")))

object VertexAIClient:

  /** Create a new VertexAIClient with the given configuration
    */
  /** Create a new mock VertexAIClient with the given configuration
    */
  def make(
      projectId: String,
      location: String = "us-central1",
      modelId: String = "claude-3-sonnet-20240229"
  ): VertexAIClient =
    val config = VertexAIConfig(
      projectId = projectId,
      location = location,
      modelId = modelId
    )
    new VertexAIClient(config)

  /** For actual implementation, please use the VertexAIClient from the core module: import
    * com.agenticai.core.llm.VertexAIClient
    */
