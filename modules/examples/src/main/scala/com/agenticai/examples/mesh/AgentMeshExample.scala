package com.agenticai.examples.mesh

import zio._
import com.agenticai.core.agent.Agent
import scala.util.Random

/**
 * Simplified version of the mesh example that will compile.
 * This is a placeholder until the mesh module is integrated.
 */
object AgentMeshExample extends ZIOAppDefault {

  /**
   * Simple calculator agent.
   */
  class CalculatorAgent extends Agent[String, Double] {
    def process(input: String): Task[Double] = ZIO.attempt {
      val tokens = input.split(" ")
      val operation = tokens(0)
      val a = tokens(1).toDouble
      val b = tokens(2).toDouble
      
      operation match {
        case "add" => a + b
        case "subtract" => a - b
        case "multiply" => a * b
        case "divide" => a / b
        case _ => throw new IllegalArgumentException(s"Unknown operation: $operation")
      }
    }
  }

  /**
   * Text processing agent.
   */
  class TextProcessingAgent extends Agent[String, String] {
    def process(input: String): Task[String] = ZIO.succeed {
      val tokens = input.split(" ")
      tokens.map(_.capitalize).mkString(" ")
    }
  }
  
  /**
   * Weather forecast agent (simulated).
   */
  class WeatherAgent extends Agent[String, String] {
    private val conditions = Array("Sunny", "Cloudy", "Rainy", "Stormy", "Windy", "Snowy")
    private val random = new Random()
    
    def process(input: String): Task[String] = ZIO.succeed {
      val temperature = 60 + random.nextInt(40)
      val condition = conditions(random.nextInt(conditions.length))
      s"Weather forecast for $input: $condition with a temperature of $temperature°F"
    }
  }
  
  /**
   * Composite agent that delegates to multiple agents.
   */
  class CompositeAgent(
    calculatorAgent: Agent[String, Double],
    textAgent: Agent[String, String],
    weatherAgent: Agent[String, String]
  ) extends Agent[String, String] {
    def process(input: String): Task[String] = {
      if (input.startsWith("calculate")) {
        calculatorAgent.process(input.substring("calculate ".length))
          .map(result => s"Calculation result: $result")
      } else if (input.startsWith("format")) {
        textAgent.process(input.substring("format ".length))
          .map(result => s"Formatted text: $result")
      } else if (input.startsWith("weather")) {
        weatherAgent.process(input.substring("weather ".length))
      } else {
        ZIO.succeed("I don't understand that command. Try starting with 'calculate', 'format', or 'weather'.")
      }
    }
  }
  
  /**
   * Simple implementation that runs locally.
   * This is a placeholder for the full mesh functionality.
   */
  def run: Task[Unit] = {
    for {
      // Create the local agents
      calculatorAgent <- ZIO.succeed(new CalculatorAgent())
      textAgent <- ZIO.succeed(new TextProcessingAgent())
      weatherAgent <- ZIO.succeed(new WeatherAgent())
      compositeAgent <- ZIO.succeed(new CompositeAgent(calculatorAgent, textAgent, weatherAgent))
      
      // Display information about the example
      _ <- Console.printLine("Agent Mesh Example (Local Mode)")
      _ <- Console.printLine("In a full mesh implementation, these agents would run on separate nodes.")
      
      // Test the composite agent
      _ <- Console.printLine("\nTesting local composite agent:")
      result1 <- compositeAgent.process("calculate add 5 3")
      _ <- Console.printLine(s"  - $result1")
      
      result2 <- compositeAgent.process("format hello agent mesh network")
      _ <- Console.printLine(s"  - $result2")
      
      result3 <- compositeAgent.process("weather San Francisco")
      _ <- Console.printLine(s"  - $result3")
      
      // Final message
      _ <- Console.printLine("\nTo enable distributed agent mesh functionality:")
      _ <- Console.printLine("1. Add the mesh module to build.sbt")
      _ <- Console.printLine("2. Uncomment the mesh-related imports")
      _ <- Console.printLine("3. Implement the full mesh network functionality")
    } yield ()
  }
}