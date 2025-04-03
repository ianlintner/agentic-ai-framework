package com.agenticai.examples

import com.agenticai.core.llm._
import com.agenticai.core.memory._
import zio._
import zio.stream._

/**
 * Example demonstrating the use of the Claude 3.7 agent with Google Vertex AI
 */
object ClaudeVertexAIExample extends ZIOAppDefault {

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
 * Interactive example allowing user input via the console
 */
object InteractiveClaudeExample extends ZIOAppDefault {
  def run = {
    // Configure your Google Cloud Project here
    val config = VertexAIConfig.claudeDefault.copy(
      projectId = "your-gcp-project-id", // Replace with your GCP Project ID
      // Optional: provide path to credentials JSON file for authentication
      // credentialsPath = Some("/path/to/credentials.json")
    )
    
    // Create memory system
    for {
      // Log configuration
      _ <- Console.printLine("Starting Interactive Claude 3.7 Session")
      _ <- Console.printLine(s"Using model: ${config.publisher}/${config.modelName}")
      _ <- Console.printLine("Type 'exit' to end the conversation")
      
      // Create the Claude agent
      client <- VertexAIClient.create(config)
      agent = new ClaudeAgent(
        name = "Claude",
        client = client,
        memory = new InMemorySystem()
      )
      
      // Begin interactive loop
      _ <- runInteractiveLoop(agent)
        .ensuring(ZIO.unit)
    } yield ()
  }
  
  def runInteractiveLoop(agent: ClaudeAgent): ZIO[Any, Throwable, Unit] = {
    def loop: ZIO[Any, Throwable, Unit] = {
      for {
        // Prompt for user input
        _ <- Console.printLine("\nYou: ")
        input <- Console.readLine
        
        // Continue or exit based on input
        _ <- if (input.toLowerCase == "exit") {
          Console.printLine("Ending conversation.")
        } else {
          for {
            // Print prompt for Claude's response
            _ <- Console.print("\nClaude: ")
            
            // Stream the response
            _ <- agent.generateStream(input)
              .foreach(token => ZIO.succeed(print(token)))
              .catchAll(error => Console.printLine(s"Error: ${error.getMessage}"))
              
            // Print newline after response
            _ <- Console.printLine("")
            
            // Continue loop
            _ <- loop
          } yield ()
        }
      } yield ()
    }
    
    loop
  }
}