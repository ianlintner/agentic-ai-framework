package com.agenticai.examples.mesh

import zio._
import com.agenticai.core.agent.Agent
import com.agenticai.core.mesh._
import com.agenticai.core.mesh.protocol._
import com.agenticai.core.mesh.server.HttpServer

/**
 * Example demonstrating the distributed agent mesh.
 * 
 * This example shows how to deploy and use agents in a distributed mesh network.
 * It creates multiple nodes, deploys agents to these nodes, and demonstrates remote
 * agent communication.
 */
object DistributedAgentMeshExample extends ZIOAppDefault {
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
   * Run the distributed agent mesh example.
   */
  def run: Task[Unit] = {
    // Configuration for the nodes
    val node1Port = 8080
    val node2Port = 8081
    
    // Create serialization for both nodes
    val serialization = JsonSerialization()
    
    // Program that demonstrates the mesh functionality
    val program = for {
      // Start node 1
      _ <- Console.printLine("Starting Node 1...")
      node1Server <- HttpServer(node1Port, serialization).start
      
      // Start node 2
      _ <- Console.printLine("Starting Node 2...")
      node2Server <- HttpServer(node2Port, serialization).start
      
      // Wait for servers to initialize
      _ <- ZIO.sleep(1.second)
      
      // Create mesh interfaces for both nodes
      node1Location = AgentLocation.local(node1Port)
      node2Location = AgentLocation.local(node2Port)
      
      // Create protocol for HTTP communication
      protocol = HttpProtocol(serialization)
      
      // Create mesh interfaces
      node1Mesh = AgentMesh(protocol, node1Location)
      node2Mesh = AgentMesh(protocol, node2Location)
      
      // Create agent instances
      calculatorAgent = new CalculatorAgent()
      textAgent = new TextProcessingAgent()
      
      // Deploy agents to their respective nodes
      _ <- Console.printLine("Deploying agents to mesh nodes...")
      calculatorRef <- node1Mesh.deploy(calculatorAgent, node1Location)
      textRef <- node2Mesh.deploy(textAgent, node2Location)
      
      // Get remote agent wrappers
      _ <- Console.printLine("Getting remote agent wrappers...")
      remoteCalculator <- node2Mesh.getRemoteAgent(calculatorRef)
      remoteTextProcessor <- node1Mesh.getRemoteAgent(textRef)
      
      // Use the remote agents
      _ <- Console.printLine("\nTesting distributed agent mesh:")
      
      // Call calculator agent from node 2
      _ <- Console.printLine("Calling calculator agent from node 2...")
      calculationResult <- remoteCalculator.process("add 10 25")
      _ <- Console.printLine(s"  Calculation result: $calculationResult")
      
      // Call text processor agent from node 1
      _ <- Console.printLine("Calling text processor agent from node 1...")
      textResult <- remoteTextProcessor.process("hello distributed agent mesh network")
      _ <- Console.printLine(s"  Text processing result: $textResult")
      
      // Final message
      _ <- Console.printLine("\nDistributed agent mesh example completed successfully!")
      _ <- Console.printLine("The agents are deployed on different nodes and can communicate with each other.")
      
      // Cleanup
      _ <- Console.printLine("\nShutting down nodes...")
      _ <- node1Server.interrupt
      _ <- node2Server.interrupt
    } yield ()
    
    // Run the program with error handling
    program.catchAll { error =>
      Console.printLine(s"Error: ${error.getMessage}")
    }
  }
}