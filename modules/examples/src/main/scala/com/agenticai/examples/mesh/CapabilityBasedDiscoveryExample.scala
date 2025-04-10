package com.agenticai.examples.mesh

import zio.*
import com.agenticai.core.agent.Agent
import com.agenticai.mesh.discovery.*
import com.agenticai.mesh.protocol.*

/** Example demonstrating capability-based agent discovery in the mesh.
  *
  * This example shows how to register agents with specific capabilities and then discover them
  * based on those capabilities to create dynamic workflows.
  */
object CapabilityBasedDiscoveryExample extends ZIOAppDefault:

  /** Text processing agent with formatting capabilities.
    */
  class TextFormattingAgent extends Agent[String, String]:

    def process(input: String): Task[String] = ZIO.succeed {
      val tokens = input.split(" ")
      tokens.map(_.capitalize).mkString(" ")
    }

  /** Text summarization agent with text reduction capabilities.
    */
  class TextSummarizationAgent extends Agent[String, String]:

    def process(input: String): Task[String] = ZIO.succeed {
      // Very simple summarization by keeping first and last sentence
      // and truncating the middle
      val sentences = input.split("[.!?]\\s+")
      if sentences.length <= 2 then input
      else s"${sentences.head}. ... ${sentences.last}."
    }

  /** Calculator agent with math capabilities.
    */
  class CalculatorAgent extends Agent[String, Double]:

    def process(input: String): Task[Double] = ZIO.attempt {
      val tokens    = input.split(" ")
      val operation = tokens(0)
      val a         = tokens(1).toDouble
      val b         = tokens(2).toDouble

      operation match
        case "add"      => a + b
        case "subtract" => a - b
        case "multiply" => a * b
        case "divide"   => a / b
        case _          => throw new IllegalArgumentException(s"Unknown operation: $operation")
    }

  /** Run the capability-based discovery example.
    */
  def run =
    // Program that demonstrates capability-based agent discovery
    val program = for
      // Display basic information
      _ <- Console.printLine("Starting capability-based discovery example...")

      // Create agent instances
      formattingAgent    = new TextFormattingAgent()
      summarizationAgent = new TextSummarizationAgent()
      calculatorAgent    = new CalculatorAgent()

      // Define sample inputs
      textInput = "This is a sample text that will be processed by multiple agents. " +
        "We will format it, analyze it, and summarize it. " +
        "It's a great example of dynamic agent discovery and composition."

      // Process with formatting agent
      _             <- Console.printLine("\nProcessing with formatting agent:")
      formattedText <- formattingAgent.process(textInput)
      _             <- Console.printLine(s"Result: $formattedText")

      // Process with summarization agent
      _              <- Console.printLine("\nProcessing with summarization agent:")
      summarizedText <- summarizationAgent.process(textInput)
      _              <- Console.printLine(s"Result: $summarizedText")

      // Process with calculator agent
      _          <- Console.printLine("\nProcessing with calculator agent:")
      calcResult <- calculatorAgent.process("multiply 7 6")
      _          <- Console.printLine(s"Result: $calcResult")

      // Initialize agent directory for discovery
      directory <- ZIO.succeed(AgentDirectory.inMemory)

      // Final message
      _ <- Console.printLine("\nExample completed successfully!")
    yield ()

    // Run the program with error handling
    program.catchAll { error =>
      Console.printLine(s"Error: ${error.getMessage}")
    }
