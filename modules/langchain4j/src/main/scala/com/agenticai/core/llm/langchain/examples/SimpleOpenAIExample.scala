package com.agenticai.core.llm.langchain.examples

import com.agenticai.core.llm.langchain._
import zio._
import zio.Console._

/**
 * A simple example application that demonstrates how to use the Langchain4j integration
 * with OpenAI.
 *
 * Usage:
 * - Set the OPENAI_API_KEY environment variable
 * - Run the example
 * - Type messages to interact with OpenAI
 * - Type "exit" to quit
 */
object SimpleOpenAIExample extends ZIOAppDefault {
  
  // Create an OpenAI agent
  private def makeOpenAIAgent(apiKey: String): ZIO[Any, Throwable, Agent] = {
    LangchainAgent.make(
      ZIOChatModelFactory.ModelType.OpenAI,
      ZIOChatModelFactory.ModelConfig(
        apiKey = Some(apiKey),
        modelName = Some("gpt-4"),
        temperature = Some(0.7)
      ),
      name = "openai-assistant",
      maxHistory = 10
    )
  }

  // Main conversation loop
  private def conversationLoop(agent: Agent): ZIO[Any, Throwable, Unit] = {
    for {
      // Prompt the user for input
      _ <- printLine("You: ")
      userInput <- readLine
      
      // Check if the user wants to exit
      _ <- ZIO.when(userInput.toLowerCase == "exit") {
        printLine("Exiting...") *> ZIO.interrupt
      }
      
      // Process the user input
      _ <- printLine("OpenAI: ")
      // Use the streaming API to show response as it comes in
      _ <- agent.process(userInput)
        .tap(chunk => printLine(chunk).orDie)
        .runDrain
        .fork
        .flatMap(_.join)
      
      // Print a blank line for readability
      _ <- printLine("")
      
      // Continue the conversation loop
      _ <- conversationLoop(agent)
    } yield ()
  }

  // Main program
  def run = {
    // Get the API key from the environment
    val program = for {
      apiKey <- System.env("OPENAI_API_KEY").someOrFail(new RuntimeException(
        "Please set the OPENAI_API_KEY environment variable"
      ))
      
      // Create the agent
      _ <- printLine("Initializing OpenAI agent...")
      agent <- makeOpenAIAgent(apiKey)
      _ <- printLine("OpenAI agent ready. Type 'exit' to quit.")
      _ <- printLine("")
      
      // Start the conversation loop
      _ <- conversationLoop(agent)
    } yield ()
    
    program.catchAll { error =>
      printLine(s"Error: ${error.getMessage}")
    }
  }
}
