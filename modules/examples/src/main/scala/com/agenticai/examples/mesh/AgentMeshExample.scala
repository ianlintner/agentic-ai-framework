package com.agenticai.examples.mesh

import zio.*
import com.agenticai.core.agent.Agent
import com.agenticai.mesh.*
import com.agenticai.mesh.protocol.*
import com.agenticai.mesh.discovery.*
import scala.util.Random

/** Example demonstrating distributed agent mesh functionality.
  */
object AgentMeshExample extends ZIOAppDefault:

  /** Simple calculator agent.
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

  /** Text processing agent.
    */
  class TextProcessingAgent extends Agent[String, String]:

    def process(input: String): Task[String] = ZIO.succeed {
      val tokens = input.split(" ")
      tokens.map(_.capitalize).mkString(" ")
    }

  /** Weather forecast agent (simulated).
    */
  class WeatherAgent extends Agent[String, String]:
    private val conditions = Array("Sunny", "Cloudy", "Rainy", "Stormy", "Windy", "Snowy")
    private val random     = new Random()

    def process(input: String): Task[String] = ZIO.succeed {
      val temperature = 60 + random.nextInt(40)
      val condition   = conditions(random.nextInt(conditions.length))
      s"Weather forecast for $input: $condition with a temperature of $temperature°F"
    }

  /** Composite agent that delegates to multiple agents.
    */
  class CompositeAgent(
      calculatorAgent: Agent[String, Double],
      textAgent: Agent[String, String],
      weatherAgent: Agent[String, String]
  ) extends Agent[String, String]:

    def process(input: String): Task[String] =
      if input.startsWith("calculate") then
        calculatorAgent
          .process(input.substring("calculate ".length))
          .map(result => s"Calculation result: $result")
      else if input.startsWith("format") then
        textAgent
          .process(input.substring("format ".length))
          .map(result => s"Formatted text: $result")
      else if input.startsWith("weather") then
        weatherAgent.process(input.substring("weather ".length))
      else
        ZIO.succeed(
          "I don't understand that command. Try starting with 'calculate', 'format', or 'weather'."
        )

  /** Implementation that demonstrates the full mesh network functionality.
    */
  def run: Task[Unit] =
    for
      // Create the mesh network with discovery capabilities
      mesh <- ZIO.succeed(AgentMeshWithDiscovery())

      // Create the local agents
      calculatorAgent <- ZIO.succeed(new CalculatorAgent())
      textAgent       <- ZIO.succeed(new TextProcessingAgent())
      weatherAgent    <- ZIO.succeed(new WeatherAgent())

      // Define agent metadata with capabilities
      calculatorMeta = AgentMetadata(
        capabilities = Set("math", "arithmetic", "calculation"),
        inputType = "String",
        outputType = "Double",
        properties = Map("name" -> "Calculator", "description" -> "Performs basic arithmetic operations")
      )

      textMeta = AgentMetadata(
        capabilities = Set("text", "formatting", "capitalization"),
        inputType = "String",
        outputType = "String",
        properties = Map("name" -> "Text Processor", "description" -> "Formats and processes text")
      )

      weatherMeta = AgentMetadata(
        capabilities = Set("weather", "forecast", "temperature"),
        inputType = "String",
        outputType = "String",
        properties = Map("name" -> "Weather Service", "description" -> "Provides weather forecasts")
      )

      // Register agents with the mesh
      _ <- Console.printLine("Registering agents with the mesh network...")
      calcRef <- mesh.registerWithCapabilities(calculatorAgent, calculatorMeta)
      textRef <- mesh.registerWithCapabilities(textAgent, textMeta)
      weatherRef <- mesh.registerWithCapabilities(weatherAgent, weatherMeta)

      // Get remote agent references
      remoteCalcAgent <- mesh.getRemoteAgent(calcRef)
      remoteTextAgent <- mesh.getRemoteAgent(textRef)
      remoteWeatherAgent <- mesh.getRemoteAgent(weatherRef)

      // Create a composite agent that uses the remote agents
      compositeAgent <- ZIO.succeed(
        new CompositeAgent(remoteCalcAgent, remoteTextAgent, remoteWeatherAgent)
      )

      // Display information about the example
      _ <- Console.printLine("Agent Mesh Example (Distributed Mode)")
      _ <- Console.printLine(
        "Agents are now running in a distributed mesh network with discovery capabilities."
      )

      // Test the composite agent using the mesh
      _       <- Console.printLine("\nTesting distributed composite agent:")
      result1 <- compositeAgent.process("calculate add 5 3")
      _       <- Console.printLine(s"  - $result1")

      result2 <- compositeAgent.process("format hello agent mesh network")
      _       <- Console.printLine(s"  - $result2")

      result3 <- compositeAgent.process("weather San Francisco")
      _       <- Console.printLine(s"  - $result3")

      // Demonstrate agent discovery
      _ <- Console.printLine("\nDemonstrating agent discovery:")

      // Find agents by capabilities
      _ <- Console.printLine("Finding agents with 'math' capability:")
      mathAgents <- mesh.findAgentsByCapabilities(Set("math"))
      _ <- ZIO.foreach(mathAgents) { agent =>
        val name = agent.metadata.properties.getOrElse("name", "Unnamed Agent")
        val description = agent.metadata.properties.getOrElse("description", "No description")
        Console.printLine(s"  - $name: $description")
      }

      // Find agents by input/output types
      _ <- Console.printLine("\nFinding agents that process String input and produce String output:")
      textAgents <- mesh.findAgentsByTypes("String", "String")
      _ <- ZIO.foreach(textAgents) { agent =>
        val name = agent.metadata.properties.getOrElse("name", "Unnamed Agent")
        val description = agent.metadata.properties.getOrElse("description", "No description")
        Console.printLine(s"  - $name: $description")
      }

      // Final message
      _ <- Console.printLine("\nDistributed agent mesh functionality successfully implemented!")
    yield ()
