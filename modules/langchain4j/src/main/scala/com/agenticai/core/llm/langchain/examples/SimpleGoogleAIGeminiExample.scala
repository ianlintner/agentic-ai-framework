package com.agenticai.core.llm.langchain.examples

import com.agenticai.core.llm.langchain.*
import zio.*
import zio.Console.*

/** A simple example application that demonstrates how to use the Langchain4j integration with
  * Google AI Gemini API.
  *
  * Usage:
  *   - Get a Google AI API key from https://ai.google.dev/
  *   - Set the GOOGLE_AI_API_KEY environment variable
  *   - Run the example
  *   - Type messages to interact with Gemini
  *   - Type "exit" to quit
  */
object SimpleGoogleAIGeminiExample extends ZIOAppDefault:

  // Create a Google AI Gemini agent
  private def makeGoogleAIGeminiAgent(apiKey: String): ZIO[Any, Throwable, Agent] =
    LangchainAgent.make(
      ZIOChatModelFactory.ModelType.GoogleAIGemini,
      ZIOChatModelFactory.ModelConfig(
        apiKey = Some(apiKey),
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
    // Get the API key from the environment
    val program = for
      apiKey <- System
        .env("GOOGLE_AI_API_KEY")
        .someOrFail(
          new RuntimeException(
            "Please set the GOOGLE_AI_API_KEY environment variable"
          )
        )

      // Create the agent
      _     <- printLine("Initializing Google AI Gemini agent...")
      agent <- makeGoogleAIGeminiAgent(apiKey)
      _     <- printLine("Gemini agent ready. Type 'exit' to quit.")
      _     <- printLine("")

      // Start the conversation loop
      _ <- conversationLoop(agent)
    yield ()

    program.catchAll { error =>
      printLine(s"Error: ${error.getMessage}")
    }
