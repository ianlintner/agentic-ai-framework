package com.agenticai.mesh.discovery

import zio._
import zio.test._
import zio.test.Assertion._
import com.agenticai.core.agent.Agent
import com.agenticai.mesh.AgentMesh
import com.agenticai.mesh.protocol.{AgentLocation, RemoteAgentRef}
import java.util.UUID

object MeshWithDiscoverySpec extends ZIOSpecDefault {

  // Simple test agent implementation
  class TestAgent extends Agent[String, String] {
    def process(input: String): Task[String] = ZIO.succeed(input.toUpperCase())
  }

  // Mock mesh for testing
  class MockAgentMesh extends AgentMesh {
    val deployedAgents = new scala.collection.concurrent.TrieMap[UUID, Agent[_, _]]()
    
    def deploy[I, O](
      agent: Agent[I, O],
      location: AgentLocation
    ): Task[RemoteAgentRef[I, O]] = {
      val id = UUID.randomUUID()
      val ref = new RemoteAgentRef[I, O] {
        val id: UUID = id
        def call(input: I): Task[O] = agent.process(input)
        def location: AgentLocation = AgentLocation.local(8080)
      }
      deployedAgents.put(id, agent)
      ZIO.succeed(ref)
    }
    
    def getRemoteAgent[I, O](ref: RemoteAgentRef[I, O]): Task[Agent[I, O]] = {
      ZIO.succeed(
        deployedAgents.get(ref.id)
          .map(_.asInstanceOf[Agent[I, O]])
          .getOrElse(new Agent[I, O] {
            def process(input: I): Task[O] = 
              ZIO.fail(new RuntimeException(s"Agent with ID ${ref.id} not found"))
          })
      )
    }
    
    def importAgent[I, O](ref: RemoteAgentRef[I, O]): Task[Agent[I, O]] = {
      getRemoteAgent(ref)
    }
    
    def register[I, O](agent: Agent[I, O]): Task[RemoteAgentRef[I, O]] = {
      deploy(agent, AgentLocation.local(8080))
    }
    
    def withServerLocation(serverLocation: AgentLocation): AgentMesh = {
      this
    }
  }

  def spec = suite("MeshWithDiscovery")(
    test("should register agent with capabilities") {
      for {
        // Create directory and mesh
        directory <- InMemoryAgentDirectory()
        mesh = new MockAgentMesh()
        meshWithDiscovery = MeshWithDiscovery(mesh, directory)
        
        // Create an agent
        agent = new TestAgent()
        
        // Create metadata
        metadata = AgentMetadata(
          capabilities = Set("text-processing", "uppercase"),
          inputType = "java.lang.String",
          outputType = "java.lang.String"
        )
        
        // Register the agent with capabilities
        ref <- meshWithDiscovery.registerAgent(agent, metadata)
        
        // Find agents by capabilities
        textAgents <- meshWithDiscovery.findAgentsByCapabilities(Set("text-processing"))
        uppercaseAgents <- meshWithDiscovery.findAgentsByCapabilities(Set("uppercase"))
        bothAgents <- meshWithDiscovery.findAgentsByCapabilities(Set("text-processing", "uppercase"))
        noneAgents <- meshWithDiscovery.findAgentsByCapabilities(Set("translation"))
      } yield {
        assert(textAgents.size)(equalTo(1)) &&
        assert(uppercaseAgents.size)(equalTo(1)) &&
        assert(bothAgents.size)(equalTo(1)) &&
        assert(noneAgents.size)(equalTo(0)) &&
        assert(textAgents.head.ref.id)(equalTo(ref.id))
      }
    },
    
    test("should get agent by capabilities") {
      for {
        // Create directory and mesh
        directory <- InMemoryAgentDirectory()
        mesh = new MockAgentMesh()
        meshWithDiscovery = MeshWithDiscovery(mesh, directory)
        
        // Create an agent
        agent = new TestAgent()
        
        // Create metadata
        metadata = AgentMetadata(
          capabilities = Set("text-processing", "uppercase"),
          inputType = "java.lang.String",
          outputType = "java.lang.String"
        )
        
        // Register the agent with capabilities
        ref <- meshWithDiscovery.registerAgent(agent, metadata)
        
        // Get agent by capabilities
        maybeAgent <- meshWithDiscovery.getAgentByCapabilities[String, String](Set("uppercase"))
        
        // Use the agent if found
        result <- ZIO.ifZIO(ZIO.succeed(maybeAgent.isDefined))(
          onTrue = maybeAgent.get.process("hello"),
          onFalse = ZIO.succeed("not found")
        )
      } yield {
        assert(maybeAgent.isDefined)(isTrue) &&
        assert(result)(equalTo("HELLO"))
      }
    },
    
    test("should find agent by ID") {
      for {
        // Create directory and mesh
        directory <- InMemoryAgentDirectory()
        mesh = new MockAgentMesh()
        meshWithDiscovery = MeshWithDiscovery(mesh, directory)
        
        // Create an agent
        agent = new TestAgent()
        
        // Create metadata
        metadata = AgentMetadata(
          capabilities = Set("text-processing"),
          inputType = "java.lang.String",
          outputType = "java.lang.String"
        )
        
        // Register the agent with capabilities
        ref <- meshWithDiscovery.registerAgent(agent, metadata)
        
        // Find agent by ID
        maybeAgentInfo <- meshWithDiscovery.findAgentById(ref.id)
        
        // Find a non-existent agent
        nonExistentAgentInfo <- meshWithDiscovery.findAgentById(UUID.randomUUID())
      } yield {
        assert(maybeAgentInfo.isDefined)(isTrue) &&
        assert(maybeAgentInfo.get.ref.id)(equalTo(ref.id)) &&
        assert(nonExistentAgentInfo.isDefined)(isFalse)
      }
    },
    
    test("should update agent status") {
      for {
        // Create directory and mesh
        directory <- InMemoryAgentDirectory()
        mesh = new MockAgentMesh()
        meshWithDiscovery = MeshWithDiscovery(mesh, directory)
        
        // Create an agent
        agent = new TestAgent()
        
        // Create metadata
        metadata = AgentMetadata(
          capabilities = Set("text-processing"),
          inputType = "java.lang.String",
          outputType = "java.lang.String"
        )
        
        // Register the agent with capabilities
        ref <- meshWithDiscovery.registerAgent(agent, metadata)
        
        // Agent starts as Active (set during registration)
        initialInfo <- meshWithDiscovery.findAgentById(ref.id)
        
        // Update agent status to Unavailable
        _ <- meshWithDiscovery.updateAgentStatus(ref.id, AgentStatus.Unavailable)
        
        // Get updated agent info
        updatedInfo <- meshWithDiscovery.findAgentById(ref.id)
      } yield {
        assert(initialInfo.isDefined)(isTrue) &&
        assert(initialInfo.get.status)(equalTo(AgentStatus.Active)) &&
        assert(updatedInfo.isDefined)(isTrue) &&
        assert(updatedInfo.get.status)(equalTo(AgentStatus.Unavailable))
      }
    },
    
    test("should subscribe to directory events") {
      for {
        // Create directory and mesh
        directory <- InMemoryAgentDirectory()
        mesh = new MockAgentMesh()
        meshWithDiscovery = MeshWithDiscovery(mesh, directory)
        
        // Create a queue to collect events
        queue <- Queue.unbounded[DirectoryEvent]
        
        // Create a fiber that forwards events to the queue
        fiber <- meshWithDiscovery.subscribeToDirectoryEvents()
          .foreach(event => queue.offer(event))
          .fork
        
        // Create an agent
        agent = new TestAgent()
        
        // Create metadata
        metadata = AgentMetadata(
          capabilities = Set("text-processing"),
          inputType = "java.lang.String",
          outputType = "java.lang.String"
        )
        
        // Register the agent (should trigger an event)
        ref <- meshWithDiscovery.registerAgent(agent, metadata)
        
        // Update agent status (should trigger another event)
        _ <- meshWithDiscovery.updateAgentStatus(ref.id, AgentStatus.Unavailable)
        
        // Take two events from the queue
        event1 <- queue.take
        event2 <- queue.take
        
        // Clean up
        _ <- fiber.interrupt
      } yield {
        assert(event1.isInstanceOf[DirectoryEvent.AgentRegistered])(isTrue) &&
        assert(event1.agentId)(equalTo(ref.id)) &&
        assert(event2.isInstanceOf[DirectoryEvent.AgentStatusChanged])(isTrue) &&
        assert(event2.agentId)(equalTo(ref.id))
      }
    }
  ).provideLayer(ZLayer.succeed(Runtime.default.unsafe))
}