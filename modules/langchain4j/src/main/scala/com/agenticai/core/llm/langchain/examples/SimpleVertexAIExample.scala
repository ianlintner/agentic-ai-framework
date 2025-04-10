package com.agenticai.core.llm.langchain.examples

import com.agenticai.core.llm.langchain.*
import zio.*
import zio.Console.*

/** A simple example application that demonstrates how to use the Langchain4j integration with
  * Google Vertex AI.
  *
  * Usage:
  *   - Make sure you have Google Cloud SDK installed and configured
  *   - Set the GOOGLE_PROJECT_ID environment variable
  *   - Run the example
  *   - Type messages to interact with Gemini
  *   - Type "exit" to quit
  */
object SimpleVertexAIExample extends ZIOAppDefault:

  // Create a Vertex AI Gemini agent
  private def makeVertexAIAgent(projectId: String): ZIO[Any, Throwable, Agent] =
    LangchainAgent.make(
      ZIOChatModelFactory.ModelType.VertexAIGemini,
      ZIOChatModelFactory.ModelConfig(
        projectId = Some(projectId),
        location = Some("us-central1"),
        modelName = Some("gemini-1.5-pro"),
        temperature = Some(0.7)
      ),
      name = "gemini-assistant",
      maxHistory = 10
    )

  // Main conversation loop
  private def conversationLoop(agent: Agent): ZIO[Any, Throwable, Unit] =
    for
      // Prompt the user for input
      _         <- printLine("You: ")
      userInput <- readLine

      // Check if the user wants to exit
      _ <- ZIO.when(userInput.toLowerCase == "exit") {
        printLine("Exiting...") *> ZIO.interrupt
      }

      // Process the user input
      _ <- printLine("Gemini: ")
      // Use the streaming API to show response as it comes in
      _ <- agent
        .process(userInput)
        .tap(chunk => printLine(chunk).orDie)
        .runDrain
        .fork
        .flatMap(_.join)

      // Print a blank line for readability
      _ <- printLine("")

      // Continue the conversation loop
      _ <- conversationLoop(agent)
    yield ()

  // Main program
  def run =
    // Get the project ID from the environment
    val program = for
      projectId <- System
        .env("GOOGLE_PROJECT_ID")
        .someOrFail(
          new RuntimeException(
            "Please set the GOOGLE_PROJECT_ID environment variable"
          )
        )

      // Create the agent
      _     <- printLine("Initializing Vertex AI Gemini agent...")
      agent <- makeVertexAIAgent(projectId)
      _     <- printLine("Gemini agent ready. Type 'exit' to quit.")
      _     <- printLine("")

      // Start the conversation loop
      _ <- conversationLoop(agent)
    yield ()

    program.catchAll { error =>
      printLine(s"Error: ${error.getMessage}")
    }
