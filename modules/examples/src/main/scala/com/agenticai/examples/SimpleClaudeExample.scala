package com.agenticai.examples

import com.agenticai.core.llm._
import com.agenticai.core.memory._
import zio._
import zio.stream._
import zio.Console

/**
 * A simplified example of using Claude 3.7 with Vertex AI
 */
object SimpleClaudeExample extends ZIOAppDefault {
  override def run = {
    // Create a client with default configuration
    val config = VertexAIConfig.claudeDefault

    // Run the example
    for {
      _ <- ZIO.logInfo("Starting Claude 3.7 example")
      _ <- ZIO.logInfo(s"Project ID: ${config.projectId}")
      _ <- ZIO.logInfo(s"Model: ${config.publisher}/${config.modelName}")

      // Create a client
      client <- VertexAIClient.create(config)

      // Generate text
      prompt = "What is the meaning of life?"
      _ <- ZIO.logInfo(s"Sending prompt: $prompt")
      response <- client.complete(prompt)
      _ <- ZIO.logInfo(s"Response: $response")

    } yield ()
  }
}

/**
 * A test example that only initializes the client to verify connectivity
 */
object VertexAIConnectivityTest extends ZIOAppDefault {
  def run = {
    // Configure your Google Cloud Project here
    val config = VertexAIConfig.claudeDefault.copy(
      projectId = "your-gcp-project-id", // Replace with your GCP Project ID
    )
    
    for {
      _ <- Console.printLine("Testing Vertex AI connectivity...")
      
      result <- (for {
        client <- VertexAIClient.create(config)
        _ <- ZIO.unit
      } yield true).catchAll { error =>
        Console.printLine(s"Error connecting to Vertex AI: ${error.getMessage}").as(false)
      }
      
      _ <- if (result) {
        Console.printLine("Successfully connected to Vertex AI!")
      } else {
        Console.printLine("Failed to connect to Vertex AI.")
      }
    } yield ()
  }
}