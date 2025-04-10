package com.agenticai.demo

import scala.concurrent.Future
import scala.util.{Failure, Success}
import scala.concurrent.ExecutionContext.Implicits.global

/** Simplified standalone demo for Claude 3.7 via Vertex AI Compatible with Scala 2.12.x
  */
object SimplifiedVertexAIDemo:

  // Simple config class compatible with Scala 2.12
  case class VertexConfig(
      projectId: String,
      location: String,
      modelId: String
  )

  def main(args: Array[String]): Unit =
    println("=== Simplified Claude 3.7 on Vertex AI Demo ===")

    // Create config using legacy-safe approach
    val config = VertexConfig(
      projectId = Option(System.getenv("GOOGLE_CLOUD_PROJECT")).getOrElse("your-project-id"),
      location = "us-central1",
      modelId = "claude-3-7-haiku-20240307"
    )

    println(s"Project: ${config.projectId}")
    println(s"Model: ${config.modelId}")

    // Sample prompt
    val prompt = "Explain what an agentic AI framework is in simple terms."
    println(s"\nPrompt: $prompt")

    // Simulate API call
    simulateResponse(config, prompt)

    // Wait for completion
    Thread.sleep(1000)

    println("\n=== Demo Complete ===")

  private def simulateResponse(config: VertexConfig, prompt: String): Unit =
    println("\nSending to Claude 3.7...")

    // Simulate async API call
    val future = Future {
      Thread.sleep(500) // Simulate network latency

      // Construct a sample response
      s"""An agentic AI framework is a software system that helps developers build AI applications 
         |that can take actions autonomously to achieve goals. 
         |
         |The key components typically include:
         |
         |1. A memory system for storing context and history
         |2. Decision-making capabilities to determine what actions to take
         |3. A way to interact with external tools and resources
         |4. Built-in safeguards and monitoring
         |
         |Think of it as the foundation that makes AI more helpful by enabling it to do things 
         |for you rather than just respond to questions.
         |
         |This is a simulated response to demonstrate the Claude 3.7 integration concept.""".stripMargin
    }

    future.onComplete {
      case Success(response) =>
        println("\n--- Claude 3.7 Response ---")
        println(response)
      case Failure(e) =>
        println(s"Error: ${e.getMessage}")
    }
