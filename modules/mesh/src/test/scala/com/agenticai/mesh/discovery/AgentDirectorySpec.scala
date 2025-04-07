package com.agenticai.mesh.discovery

import zio._
import zio.test._
import com.agenticai.core.agent.Agent
import com.agenticai.mesh.protocol._
import java.util.UUID
import java.time.Instant

object AgentDirectorySpec extends ZIOSpecDefault {

  // Test implementation of an agent
  class TestAgent(val id: String) extends Agent[String, String] {
    def process(input: String): Task[String] = ZIO.succeed(s"$id: $input")
  }
  
  // Helper function to create a RemoteAgentRef for testing
  def createAgentRef(id: UUID, location: AgentLocation): RemoteAgentRef[String, String] = {
    RemoteAgentRef[String, String](
      id = id,
      location = location,
      inputType = "String",
      outputType = "String"
    )
  }
  
  def spec = suite("AgentDirectory")(
    test("register and discover agents") {
      for {
        // Create a directory
        directory <- ZIO.succeed(AgentDirectory.inMemory)
        
        // Create agent references
        agentId1 = UUID.randomUUID()
        agentId2 = UUID.randomUUID()
        agentId3 = UUID.randomUUID()
        
        location = AgentLocation.local(8080)
        ref1 = createAgentRef(agentId1, location)
        ref2 = createAgentRef(agentId2, location)
        ref3 = createAgentRef(agentId3, location)
        
        // Create agent metadata
        metadata1 = AgentMetadata(
          capabilities = Set("nlp", "translation"),
          inputType = "String",
          outputType = "String",
          properties = Map("language" -> "English")
        )
        
        metadata2 = AgentMetadata(
          capabilities = Set("nlp", "sentiment-analysis"),
          inputType = "String",
          outputType = "String",
          properties = Map("model" -> "basic")
        )
        
        metadata3 = AgentMetadata(
          capabilities = Set("extraction", "entity-recognition"),
          inputType = "String",
          outputType = "String",
          properties = Map("entities" -> "person,organization")
        )
        
        // Register the agents
        _ <- directory.registerAgent(ref1, metadata1)
        _ <- directory.registerAgent(ref2, metadata2)
        _ <- directory.registerAgent(ref3, metadata3)
        
        // Discover all agents
        allAgents <- directory.getAllAgents()
        
        // Discover agents by capability
        nlpAgents <- directory.discoverAgents(AgentQuery(capabilities = Set("nlp")))
        extractionAgents <- directory.discoverAgents(AgentQuery(capabilities = Set("extraction")))
        
        // Discover agents by multiple capabilities
        translationAgents <- directory.discoverAgents(AgentQuery(capabilities = Set("translation")))
        recognitionAgents <- directory.discoverAgents(AgentQuery(capabilities = Set("entity-recognition")))
        
        // Discover agents by property
        englishAgents <- directory.discoverAgents(AgentQuery(properties = Map("language" -> "English")))
        
        // Get specific agent info
        agent1Info <- directory.getAgentInfo(agentId1)
        nonExistentAgent <- directory.getAgentInfo(UUID.randomUUID())
      } yield {
        assertTrue(
          allAgents.size == 3,
          nlpAgents.size == 2,
          extractionAgents.size == 1,
          translationAgents.size == 1,
          recognitionAgents.size == 1,
          englishAgents.size == 1,
          agent1Info.isDefined,
          agent1Info.exists(_.metadata.capabilities.contains("translation")),
          nonExistentAgent.isEmpty
        )
      }
    },
    
    test("update agent status") {
      for {
        // Create a directory
        directory <- ZIO.succeed(AgentDirectory.inMemory)
        
        // Create agent reference
        agentId = UUID.randomUUID()
        location = AgentLocation.local(8080)
        ref = createAgentRef(agentId, location)
        
        // Create agent metadata
        metadata = AgentMetadata(
          capabilities = Set("test"),
          inputType = "String",
          outputType = "String"
        )
        
        // Register the agent
        _ <- directory.registerAgent(ref, metadata)
        
        // Verify initial status
        initialInfo <- directory.getAgentInfo(agentId)
        
        // Update the status
        _ <- directory.updateAgentStatus(agentId, AgentStatus.Unavailable)
        
        // Get updated info
        updatedInfo <- directory.getAgentInfo(agentId)
        
        // Subscribe to events (collect some in a limited time window)
        events <- directory.subscribeToEvents()
          .take(2)
          .runCollect
          .timeoutFail(new Exception("Timeout waiting for events"))(1.second)
          .either
          
        // Update status again to generate another event
        _ <- directory.updateAgentStatus(agentId, AgentStatus.Overloaded)
      } yield {
        assertTrue(
          initialInfo.exists(_.status == AgentStatus.Active),
          updatedInfo.exists(_.status == AgentStatus.Unavailable),
          events.isRight,
          events.toOption.exists(_.size >= 1),
          events.toOption.exists(_.exists(_.isInstanceOf[DirectoryEvent.AgentRegistered])),
          events.toOption.exists(_.exists(e => 
            e.isInstanceOf[DirectoryEvent.AgentStatusChanged] && 
            e.asInstanceOf[DirectoryEvent.AgentStatusChanged].newStatus == AgentStatus.Unavailable
          ))
        )
      }
    },
    
    test("unregister agent") {
      for {
        // Create a directory
        directory <- ZIO.succeed(AgentDirectory.inMemory)
        
        // Create agent reference
        agentId = UUID.randomUUID()
        location = AgentLocation.local(8080)
        ref = createAgentRef(agentId, location)
        
        // Create agent metadata
        metadata = AgentMetadata(
          capabilities = Set("test"),
          inputType = "String",
          outputType = "String"
        )
        
        // Register the agent
        _ <- directory.registerAgent(ref, metadata)
        
        // Verify agent exists
        beforeUnregister <- directory.getAgentInfo(agentId)
        
        // Unregister the agent
        _ <- directory.unregisterAgent(agentId)
        
        // Verify agent no longer exists
        afterUnregister <- directory.getAgentInfo(agentId)
        
        // Verify all agents count
        allAgents <- directory.getAllAgents()
      } yield {
        assertTrue(
          beforeUnregister.isDefined,
          afterUnregister.isEmpty,
          allAgents.isEmpty
        )
      }
    }
  )
}