package com.agenticai.telemetry.examples

import com.agenticai.core.agent.Agent
import com.agenticai.mesh.discovery.{AgentDirectory, AgentMetadata, InMemoryAgentDirectory, TypedAgentQuery}
import com.agenticai.mesh.protocol.{AgentLocation, Protocol, Serialization}
import com.agenticai.mesh.server.HttpServer
import com.agenticai.telemetry.core.{TelemetryConfig, TelemetryProvider}
import com.agenticai.telemetry.mesh._
import io.opentelemetry.sdk.trace.SdkTracerProvider
import zio.*

import java.util.UUID

/**
 * Example showing mesh telemetry integration with the agent mesh.
 * Demonstrates tracing instrumentation, metrics collection, and context propagation
 * for distributed agent communication.
 */
object MeshTelemetryExample extends ZIOAppDefault {

  // Simple test agent that capitalizes text
  class TextAgent(id: String) extends Agent[String, String] {
    def process(input: String): Task[String] = 
      for {
        _ <- Console.printLine(s"[$id] Processing: $input")
        result = input.toUpperCase
        _ <- Clock.sleep(100.millis) // Simulate work
      } yield result
  }

  // Create instrumented mesh components for a distributed example
  def setupInstrumentedMesh(
    port: Int, 
    nodeId: String
  ): ZIO[MeshTelemetry & TelemetryProvider, Throwable, (HttpServer, Protocol, AgentDirectory)] = {
    for {
      // Create the core components
      serialization <- ZIO.succeed(Serialization.test)
      baseServer = HttpServer(port, serialization)
      baseProtocol = Protocol.inMemory
      baseDirectory = new InMemoryAgentDirectory()
      
      // Wrap them with telemetry instrumentation
      server = InstrumentedHttpServer(baseServer, nodeId)
      protocol = InstrumentedProtocol(baseProtocol)
      directory = InstrumentedAgentDirectory(baseDirectory)
    } yield (server, protocol, directory)
  }

  def runMeshExample: ZIO[MeshTelemetry & TelemetryProvider, Throwable, Unit] = {
    for {
      // Set up two mesh nodes
      (server1, protocol1, directory1) <- setupInstrumentedMesh(8001, "node-1")
      (server2, protocol2, directory2) <- setupInstrumentedMesh(8002, "node-2")
      
      // Start the servers
      _ <- Console.printLine("Starting mesh nodes...")
      _ <- server1.start
      _ <- server2.start
      
      // Create some agents
      agent1 = new TextAgent("agent-1")
      agent2 = new TextAgent("agent-2")
      
      // Register the agents with the mesh
      _ <- Console.printLine("Registering agents...")
      ref1 <- protocol1.sendAgent(
        agent1, 
        server1.location
      )
      
      ref2 <- protocol2.sendAgent(
        agent2,
        server2.location
      )
      
      // Register agents in directories
      _ <- directory1.registerAgent(
        ref1,
        AgentMetadata(
          "text-agent",
          Map("capability" -> "text-processing"),
          Some(0.1) // Initial load
        )
      )
      
      _ <- directory2.registerAgent(
        ref2,
        AgentMetadata(
          "text-agent",
          Map("capability" -> "text-processing"),
          Some(0.2) // Initial load
        )
      )
      
      // Run a discovery query to find agents
      _ <- Console.printLine("Discovering agents...")
      query = TypedAgentQuery.byCapability("text-processing")
      agents1 <- directory1.discoverAgents(query)
      _ <- Console.printLine(s"Node 1 discovered ${agents1.size} agents")
      
      // Simulate agent communication with context propagation
      _ <- Console.printLine("Sending messages between agents...")
      
      // Prepare test input and simulate processing across nodes
      startTime <- Clock.currentTime(TimeUnit.MILLISECONDS)
      
      // Call the first agent
      _ <- Console.printLine("Calling first agent...")
      result1 <- protocol1.callRemoteAgent(ref1, "Hello from mesh telemetry!")
      _ <- Console.printLine(s"Result from agent 1: $result1")
      
      // Call the second agent with the result from the first
      _ <- Console.printLine("Calling second agent...")
      result2 <- protocol2.callRemoteAgent(ref2, result1)
      _ <- Console.printLine(s"Result from agent 2: $result2")
      
      endTime <- Clock.currentTime(TimeUnit.MILLISECONDS)
      totalTime = endTime - startTime
      
      // Update agent load as the system operates
      _ <- Console.printLine("Updating agent load factors...")
      _ <- directory1.updateAgentLoadFactor(ref1.id, 0.5) // Increased load
      _ <- directory2.updateAgentLoadFactor(ref2.id, 0.8) // High load
      
      // Display results
      _ <- Console.printLine(s"Total processing time: $totalTime ms")
      _ <- Console.printLine("Mesh telemetry example completed successfully!")
    } yield ()
  }

  override def run = {
    // Set up the configuration for our telemetry
    val tracerProvider = SdkTracerProvider.builder().build()
    val spanProcessor = tracerProvider.getSpanProcessor()
    val telemetryConfig = TelemetryConfig(spanProcessor)
    
    // Run our mesh example
    runMeshExample.provide(
      // Core telemetry
      TelemetryProvider.live,
      ZLayer.succeed(telemetryConfig),
      
      // Mesh telemetry
      MeshTelemetry.live
    )
  }
}