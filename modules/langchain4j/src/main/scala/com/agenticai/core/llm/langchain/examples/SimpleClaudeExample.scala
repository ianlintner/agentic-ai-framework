package com.agenticai.core.llm.langchain.examples

import com.agenticai.core.llm.langchain._
import zio._
import zio.Console._

/**
 * A simple example application that demonstrates how to use the Langchain4j integration
 * with Claude.
 *
 * Usage:
 * - Set the CLAUDE_API_KEY environment variable
 * - Run the example
 * - Type messages to interact with Claude
 * - Type "exit" to quit
 */
object SimpleClaudeExample extends ZIOAppDefault {
  
  // Create a Claude agent
  private def makeClaudeAgent(apiKey: String): ZIO[Any, Throwable, Agent] = {
    LangchainAgent.make(
      ZIOChatModelFactory.ModelType.Claude,
      ZIOChatModelFactory.ModelConfig(
        apiKey = Some(apiKey),
        modelName = Some("claude-3-sonnet-20240229"),
        temperature = Some(0.7)
      ),
      name = "claude-assistant",
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
      _ <- printLine("Claude: ")
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
      apiKey <- System.env("CLAUDE_API_KEY").someOrFail(new RuntimeException(
        "Please set the CLAUDE_API_KEY environment variable"
      ))
      
      // Create the agent
      _ <- printLine("Initializing Claude agent...")
      agent <- makeClaudeAgent(apiKey)
      _ <- printLine("Claude agent ready. Type 'exit' to quit.")
      _ <- printLine("")
      
      // Start the conversation loop
      _ <- conversationLoop(agent)
    } yield ()
    
    program.catchAll { error =>
      printLine(s"Error: ${error.getMessage}")
    }
  }
}
