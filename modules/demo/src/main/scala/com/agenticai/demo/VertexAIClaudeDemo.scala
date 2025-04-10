package com.agenticai.demo

import zio.*
import zio.stream.*
import scala.concurrent.duration.*
import scala.util.Try

/** Claude 3.7 on Vertex AI Demo - Scala 3.3.1 compatible version with ZIO
  *
  * This demo showcases a functional approach to integrating with Vertex AI for Claude 3.7 using ZIO
  * effects for pure functional programming.
  */
object VertexAIClaudeDemo extends ZIOAppDefault:

  // Using Scala 3 syntax for nested classes with indentation
  case class VertexAIConfig(
      projectId: String = scala.sys.env.getOrElse("GOOGLE_CLOUD_PROJECT", ""),
      location: String = "us-central1",
      modelId: String = "claude-3-7-haiku-20240307",
      publisher: String = "anthropic",
      maxOutputTokens: Int = 8192,
      temperature: Float = 0.2f
  )

  // Scala 3 enum with proper type (instead of sealed trait + case objects in Scala 2)
  enum Role:
    case User, Assistant, System

  // Scala 3 case class for messages
  case class Message(role: Role, content: String)

  // Main program as a ZIO effect
  override def run =
    program.exitCode

  // Our ZIO program that executes all demo functionality
  val program: ZIO[Any, Throwable, Unit] = for
    _ <- Console.printLine("=== Claude 3.7 with Vertex AI Demo (Scala 3.3.1 with ZIO) ===")

    // Create configuration
    config = VertexAIConfig(
      projectId = "your-gcp-project-id" // Replace with actual project ID
    )

    _ <- Console.printLine(s"Using Claude 3.7 via Vertex AI")
    _ <- Console.printLine(s"Project: ${config.projectId}")
    _ <- Console.printLine(s"Model: ${config.publisher}/${config.modelId}")

    // Create client
    client = VertexAIClient(config)

    // Test prompts
    prompts = List(
      "Explain what an agentic AI framework is in simple terms.",
      "What makes Claude 3.7 different from earlier language models?"
    )

    // Process each prompt using ZStream
    _ <- ZStream
      .fromIterable(prompts)
      .mapZIO(prompt => processPrompt(client, prompt))
      .runDrain

    _ <- Console.printLine("\n=== Demo Complete ===")
  yield ()

  // Process a single prompt with the client
  def processPrompt(client: VertexAIClient, prompt: String): ZIO[Any, Throwable, Unit] = for
    _ <- Console.printLine(s"\n===== PROMPT =====\n$prompt\n")
    _ <- Console.printLine("===== RESPONSE =====")
    response <- client
      .generateText(prompt)
      .timeoutFail(new Exception("Request timed out"))(10.seconds)
      .catchAll(ex => ZIO.succeed(s"Error: ${ex.getMessage}"))
    _ <- Console.printLine(response)
  yield ()

/** Vertex AI client implementation using Scala 3 features and ZIO
  */
class VertexAIClient(config: VertexAIClaudeDemo.VertexAIConfig):
  import VertexAIClaudeDemo.*

  // Build the endpoint for the Vertex AI API
  private val endpoint =
    s"projects/${config.projectId}/locations/${config.location}/publishers/${config.publisher}/models/${config.modelId}"

  /** Generate text from Claude 3.7 using Vertex AI Uses ZIO for proper functional effect handling
    */
  def generateText(prompt: String): ZIO[Any, Throwable, String] =
    // In a real implementation, we would:
    // 1. Create a VertexAI PredictionServiceClient using ZIO's blocking context
    // 2. Format the request properly for Claude 3.7
    // 3. Make the API call wrapped in a ZIO effect
    // 4. Parse and return the response

    for
      _ <- Console.printLine(s"Calling Vertex AI endpoint: $endpoint")
      // Simulate network latency in a safe way with ZIO
      _ <- ZIO.sleep(500.milliseconds)

      // Return simulated response
      response =
        s"""As an agentic AI framework built with Scala 3 and ZIO, I would help developers create:

1. Functional, effect-based autonomous systems that can act toward goals
2. Type-safe memory systems with proper error handling
3. Composable agent capabilities through monadic structures
4. Tools and external resource integrations via ZIO modules
5. Pure functional error recovery strategies

The key advantages of this approach include referential transparency, composition via monads, 
and the powerful type checking of Scala 3 to prevent entire classes of errors at compile time.

This response demonstrates how Claude 3.7 could be integrated into your Scala 3 framework with ZIO."""
    yield response
