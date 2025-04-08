package com.agenticai.mesh.discovery

import zio._
import zio.test._
import zio.test.Assertion._
import com.agenticai.mesh.protocol.RemoteAgentRef
import com.agenticai.core.agent.Agent
import java.util.UUID
import java.time.Instant

object InMemoryAgentDirectorySpec extends ZIOSpecDefault {
  
  def spec = suite("InMemoryAgentDirectory")(
    test("registerAgent and getAgentInfo should work correctly") {
      for {
        // Create a directory
        directory <- InMemoryAgentDirectory()
        
        // Create a mock agent reference
        agentId = UUID.randomUUID()
        mockRef = new RemoteAgentRef[String, String] {
          val id: UUID = agentId
          def call(input: String): Task[String] = ZIO.succeed(input)
          def location: com.agenticai.mesh.protocol.AgentLocation = 
            com.agenticai.mesh.protocol.AgentLocation.local(8080)
        }
        
        // Create agent metadata
        metadata = AgentMetadata(
          capabilities = Set("text-processing", "formatting"),
          inputType = "java.lang.String",
          outputType = "java.lang.String"
        )
        
        // Register the agent
        _ <- directory.registerAgent(mockRef, metadata)
        
        // Retrieve agent info
        agentInfo <- directory.getAgentInfo(agentId)
      } yield {
        assert(agentInfo.isDefined)(isTrue) &&
        assert(agentInfo.get.agentId)(equalTo(agentId)) &&
        assert(agentInfo.get.metadata.capabilities)(equalTo(Set("text-processing", "formatting"))) &&
        assert(agentInfo.get.status)(equalTo(AgentStatus.Initializing))
      }
    },
    
    test("discoverAgents should filter by capabilities") {
      for {
        // Create a directory
        directory <- InMemoryAgentDirectory()
        
        // Create two agents with different capabilities
        agent1Id = UUID.randomUUID()
        agent1Ref = new RemoteAgentRef[String, String] {
          val id: UUID = agent1Id
          def call(input: String): Task[String] = ZIO.succeed(input)
          def location: com.agenticai.mesh.protocol.AgentLocation = 
            com.agenticai.mesh.protocol.AgentLocation.local(8080)
        }
        
        agent2Id = UUID.randomUUID()
        agent2Ref = new RemoteAgentRef[String, String] {
          val id: UUID = agent2Id
          def call(input: String): Task[String] = ZIO.succeed(input)
          def location: com.agenticai.mesh.protocol.AgentLocation = 
            com.agenticai.mesh.protocol.AgentLocation.local(8080)
        }
        
        // Create different metadata for each agent
        metadata1 = AgentMetadata(
          capabilities = Set("text-processing", "formatting"),
          inputType = "java.lang.String",
          outputType = "java.lang.String"
        )
        
        metadata2 = AgentMetadata(
          capabilities = Set("math", "calculation"),
          inputType = "java.lang.String",
          outputType = "java.lang.Double"
        )
        
        // Register both agents
        _ <- directory.registerAgent(agent1Ref, metadata1)
        _ <- directory.registerAgent(agent2Ref, metadata2)
        
        // Set agent statuses to active
        _ <- directory.updateAgentStatus(agent1Id, AgentStatus.Active)
        _ <- directory.updateAgentStatus(agent2Id, AgentStatus.Active)
        
        // Query for text processing agents
        textAgents <- directory.discoverAgents(AgentQuery(
          capabilities = Set("text-processing"),
          limit = 10
        ))
        
        // Query for math agents
        mathAgents <- directory.discoverAgents(AgentQuery(
          capabilities = Set("math"),
          limit = 10
        ))
        
        // Query for both types of agents (should return none)
        bothAgents <- directory.discoverAgents(AgentQuery(
          capabilities = Set("text-processing", "math"),
          limit = 10
        ))
      } yield {
        assert(textAgents.size)(equalTo(1)) &&
        assert(textAgents.head.agentId)(equalTo(agent1Id)) &&
        assert(mathAgents.size)(equalTo(1)) &&
        assert(mathAgents.head.agentId)(equalTo(agent2Id)) &&
        assert(bothAgents.size)(equalTo(0))
      }
    },
    
    test("updateAgentStatus should update status and publish events") {
      for {
        // Create a directory
        directory <- InMemoryAgentDirectory()
        
        // Create a mock agent reference
        agentId = UUID.randomUUID()
        mockRef = new RemoteAgentRef[String, String] {
          val id: UUID = agentId
          def call(input: String): Task[String] = ZIO.succeed(input)
          def location: com.agenticai.mesh.protocol.AgentLocation = 
            com.agenticai.mesh.protocol.AgentLocation.local(8080)
        }
        
        // Create agent metadata
        metadata = AgentMetadata(
          capabilities = Set("text-processing"),
          inputType = "java.lang.String",
          outputType = "java.lang.String"
        )
        
        // Register the agent
        _ <- directory.registerAgent(mockRef, metadata)
        
        // Get initial status
        initialInfo <- directory.getAgentInfo(agentId)
        initialStatus = initialInfo.get.status
        
        // Update the status
        _ <- directory.updateAgentStatus(agentId, AgentStatus.Active)
        
        // Get updated status
        updatedInfo <- directory.getAgentInfo(agentId)
        updatedStatus = updatedInfo.get.status
      } yield {
        assert(initialStatus)(equalTo(AgentStatus.Initializing)) &&
        assert(updatedStatus)(equalTo(AgentStatus.Active))
      }
    },
    
    test("unregisterAgent should remove the agent") {
      for {
        // Create a directory
        directory <- InMemoryAgentDirectory()
        
        // Create a mock agent reference
        agentId = UUID.randomUUID()
        mockRef = new RemoteAgentRef[String, String] {
          val id: UUID = agentId
          def call(input: String): Task[String] = ZIO.succeed(input)
          def location: com.agenticai.mesh.protocol.AgentLocation = 
            com.agenticai.mesh.protocol.AgentLocation.local(8080)
        }
        
        // Create agent metadata
        metadata = AgentMetadata(
          capabilities = Set("text-processing"),
          inputType = "java.lang.String",
          outputType = "java.lang.String"
        )
        
        // Register the agent
        _ <- directory.registerAgent(mockRef, metadata)
        
        // Verify it's registered
        beforeUnregister <- directory.getAgentInfo(agentId)
        
        // Unregister the agent
        _ <- directory.unregisterAgent(agentId)
        
        // Verify it's gone
        afterUnregister <- directory.getAgentInfo(agentId)
      } yield {
        assert(beforeUnregister.isDefined)(isTrue) &&
        assert(afterUnregister.isDefined)(isFalse)
      }
    },
    
    test("subscribeToEvents should receive directory events") {
      for {
        // Create a directory
        directory <- InMemoryAgentDirectory()
        
        // Create a mock agent reference
        agentId = UUID.randomUUID()
        mockRef = new RemoteAgentRef[String, String] {
          val id: UUID = agentId
          def call(input: String): Task[String] = ZIO.succeed(input)
          def location: com.agenticai.mesh.protocol.AgentLocation = 
            com.agenticai.mesh.protocol.AgentLocation.local(8080)
        }
        
        // Create agent metadata
        metadata = AgentMetadata(
          capabilities = Set("text-processing"),
          inputType = "java.lang.String",
          outputType = "java.lang.String"
        )
        
        // Create a fiber that collects events
        eventFiber <- directory.subscribeToEvents()
          .take(2)  // We expect 2 events: registration and status change
          .runCollect
          .fork
        
        // Register the agent (should produce a registered event)
        _ <- directory.registerAgent(mockRef, metadata)
        
        // Update status (should produce a status changed event)
        _ <- directory.updateAgentStatus(agentId, AgentStatus.Active)
        
        // Wait for the events to be collected
        events <- eventFiber.join
        
        // Get the events
        registrationEvent = events.head
        statusChangeEvent = events.tail.head
      } yield {
        assert(events.size)(equalTo(2)) &&
        assert(registrationEvent.isInstanceOf[DirectoryEvent.AgentRegistered])(isTrue) &&
        assert(statusChangeEvent.isInstanceOf[DirectoryEvent.AgentStatusChanged])(isTrue) &&
        assert(registrationEvent.agentId)(equalTo(agentId)) &&
        assert(statusChangeEvent.agentId)(equalTo(agentId))
      }
    }
  ).provideLayer(ZLayer.succeed(Runtime.default.unsafe))
}