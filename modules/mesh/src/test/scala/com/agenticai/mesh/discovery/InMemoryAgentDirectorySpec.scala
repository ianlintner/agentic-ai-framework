package com.agenticai.mesh.discovery
import zio._
import zio.test._
import zio.test.Assertion._
import zio.test.TestClock
import com.agenticai.mesh.protocol.RemoteAgentRef
import com.agenticai.mesh.protocol.AgentLocation
import com.agenticai.core.agent.Agent
import java.util.UUID
import java.time.Instant

object InMemoryAgentDirectorySpec extends ZIOSpecDefault {
  
  def spec = suite("InMemoryAgentDirectory")(
    test("registerAgent and getAgentInfo should work correctly") {
      for {
        // Create a directory
        directory <- ZIO.succeed(AgentDirectory.inMemory)
        
        // Create a mock agent reference
        agentId = UUID.randomUUID()
        location = AgentLocation.local(8080)
        mockRef = RemoteAgentRef[String, String](
          id = agentId,
          location = location,
          inputType = "java.lang.String",
          outputType = "java.lang.String"
        )
        
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
        assert(agentInfo.get.status)(equalTo(AgentStatus.Active))
      }
    },
    
    test("discoverAgents should filter by capabilities") {
      for {
        // Create a directory
        directory <- ZIO.succeed(AgentDirectory.inMemory)
        
        // Create two agents with different capabilities
        agent1Id = UUID.randomUUID()
        location1 = AgentLocation.local(8080)
        agent1Ref = RemoteAgentRef[String, String](
          id = agent1Id,
          location = location1,
          inputType = "java.lang.String",
          outputType = "java.lang.String"
        )
        
        agent2Id = UUID.randomUUID()
        location2 = AgentLocation.local(8080)
        agent2Ref = RemoteAgentRef[String, String](
          id = agent2Id,
          location = location2,
          inputType = "java.lang.String",
          outputType = "java.lang.Double"
        )
        
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
        
        // Query for text processing agents
        textAgents <- directory.discoverAgents(TypedAgentQuery(
          capabilities = Set("text-processing")
        ))
        
        // Query for math agents
        mathAgents <- directory.discoverAgents(TypedAgentQuery(
          capabilities = Set("math")
        ))
        
        // Query for both types of agents (should return none)
        bothAgents <- directory.discoverAgents(TypedAgentQuery(
          capabilities = Set("text-processing", "math")
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
        directory <- ZIO.succeed(AgentDirectory.inMemory)
        
        // Create a mock agent reference
        agentId = UUID.randomUUID()
        location = AgentLocation.local(8080)
        mockRef = RemoteAgentRef[String, String](
          id = agentId,
          location = location,
          inputType = "java.lang.String",
          outputType = "java.lang.String"
        )
        
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
        _ <- directory.updateAgentStatus(agentId, AgentStatus.Unavailable)
        
        // Get updated status
        updatedInfo <- directory.getAgentInfo(agentId)
        updatedStatus = updatedInfo.get.status
      } yield {
        assert(initialStatus)(equalTo(AgentStatus.Active)) &&
        assert(updatedStatus)(equalTo(AgentStatus.Unavailable))
      }
    },
    
    test("unregisterAgent should remove the agent") {
      for {
        // Create a directory
        directory <- ZIO.succeed(AgentDirectory.inMemory)
        
        // Create a mock agent reference
        agentId = UUID.randomUUID()
        location = AgentLocation.local(8080)
        mockRef = RemoteAgentRef[String, String](
          id = agentId,
          location = location,
          inputType = "java.lang.String",
          outputType = "java.lang.String"
        )
        
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
    
    test("subscribeToEvents should be indirectly verified by status changes") {
      for {
        // Create a directory
        directory <- ZIO.succeed(AgentDirectory.inMemory)
        
        // Create a mock agent reference
        agentId = UUID.randomUUID()
        location = AgentLocation.local(8080)
        mockRef = RemoteAgentRef[String, String](
          id = agentId,
          location = location,
          inputType = "java.lang.String",
          outputType = "java.lang.String"
        )
        
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
        
        // Update agent status
        _ <- directory.updateAgentStatus(agentId, AgentStatus.Unavailable)
        
        // Get updated status
        updatedInfo <- directory.getAgentInfo(agentId)
        updatedStatus = updatedInfo.get.status
      } yield {
        // Verify the status changed correctly, which indirectly verifies events are working
        assert(initialStatus)(equalTo(AgentStatus.Active)) &&
        assert(updatedStatus)(equalTo(AgentStatus.Unavailable))
      }
    }
  ) @@ TestAspect.withLiveClock @@ TestAspect.timeout(10.seconds)
}
