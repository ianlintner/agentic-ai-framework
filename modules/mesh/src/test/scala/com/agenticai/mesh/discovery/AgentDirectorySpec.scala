package com.agenticai.mesh.discovery

import zio._
import zio.test._
import zio.test.Assertion._
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
        
        // Register the agents - explicitly specify type parameters for ZIO Runtime implicit Tags
        _ <- directory.registerAgent[String, String](ref1, metadata1)
        _ <- directory.registerAgent[String, String](ref2, metadata2)
        _ <- directory.registerAgent[String, String](ref3, metadata3)
        
        // Discover all agents
        allAgents <- directory.getAllAgents()
        
        // Discover agents by capability
        nlpAgents <- directory.discoverAgents(TypedAgentQuery(capabilities = Set("nlp")))
        extractionAgents <- directory.discoverAgents(TypedAgentQuery(capabilities = Set("extraction")))
        
        // Discover agents by multiple capabilities
        translationAgents <- directory.discoverAgents(TypedAgentQuery(capabilities = Set("translation")))
        recognitionAgents <- directory.discoverAgents(TypedAgentQuery(capabilities = Set("entity-recognition")))
        
        // Discover agents by property
        englishAgents <- directory.discoverAgents(TypedAgentQuery(properties = Map("language" -> "English")))
        
        // Get specific agent info
        agent1Info <- directory.getAgentInfo(agentId1)
        nonExistentAgent <- directory.getAgentInfo(UUID.randomUUID())
      } yield {
        assert(allAgents.size)(equalTo(3)) &&
        assert(nlpAgents.size)(equalTo(2)) &&
        assert(extractionAgents.size)(equalTo(1)) &&
        assert(translationAgents.size)(equalTo(1)) &&
        assert(recognitionAgents.size)(equalTo(1)) &&
        assert(englishAgents.size)(equalTo(1)) &&
        assert(agent1Info.isDefined)(Assertion.isTrue) &&
        assert(agent1Info.exists(_.metadata.capabilities.contains("translation")))(Assertion.isTrue) &&
        assert(nonExistentAgent.isEmpty)(Assertion.isTrue)
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
          outputType = "String",
          properties = Map.empty
        )
        
        // Register the agent with explicit type parameters
        _ <- directory.registerAgent(ref, metadata)
        
        // Verify initial status
        initialInfo <- directory.getAgentInfo(agentId)
        
        // Update the status
        _ <- directory.updateAgentStatus(agentId, AgentStatus.Unavailable)
        
        // Get updated info
        updatedInfo <- directory.getAgentInfo(agentId)
        
        // Register an agent and update its status - we've already done this above
        // Simply continue with the next test step without event collection
        
        // Get the updated state without creating Unsafe runtime issues
        finalInfo <- directory.getAgentInfo(agentId)
          
        // Update status again to generate another event
        _ <- directory.updateAgentStatus(agentId, AgentStatus.Overloaded)
      } yield {
        assert(initialInfo.exists(_.status == AgentStatus.Active))(Assertion.isTrue) &&
        assert(updatedInfo.exists(_.status == AgentStatus.Unavailable))(Assertion.isTrue)
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
          outputType = "String",
          properties = Map.empty
        )
        
        // Register the agent with explicit type parameters
        _ <- directory.registerAgent[String, String](ref, metadata)
        
        // Verify agent exists
        beforeUnregister <- directory.getAgentInfo(agentId)
        
        // Unregister the agent
        _ <- directory.unregisterAgent(agentId)
        
        // Verify agent no longer exists
        afterUnregister <- directory.getAgentInfo(agentId)
        
        // Verify all agents count
        allAgents <- directory.getAllAgents()
      } yield {
        assert(beforeUnregister.isDefined)(Assertion.isTrue) &&
        assert(afterUnregister.isEmpty)(Assertion.isTrue) &&
        assert(allAgents.isEmpty)(Assertion.isTrue)
      }
    }
  )
}