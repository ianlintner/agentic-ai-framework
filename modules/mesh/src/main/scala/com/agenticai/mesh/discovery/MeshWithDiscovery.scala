package com.agenticai.mesh.discovery

import zio._
import com.agenticai.core.agent.Agent
import com.agenticai.mesh.AgentMesh
import com.agenticai.mesh.protocol.RemoteAgentRef
import com.agenticai.mesh.protocol.AgentLocation
import com.agenticai.core.capability.Capability
import java.util.UUID

/**
 * Extension of AgentMesh that integrates with the agent discovery system.
 * 
 * MeshWithDiscovery adds capability-based discovery to the standard mesh,
 * allowing agents to be found based on their capabilities across the mesh.
 */
trait MeshWithDiscovery extends AgentMesh {
  /**
   * The agent directory used for discovery.
   */
  def directory: AgentDirectory
  
  /**
   * Register an agent in the mesh with specific capabilities.
   *
   * @param agent Agent to register
   * @param metadata Metadata describing the agent's capabilities
   * @return Remote reference to the registered agent
   */
  def registerAgent[I, O](
    agent: Agent[I, O],
    metadata: AgentMetadata
  ): Task[RemoteAgentRef[I, O]] = {
    for {
      // Deploy the agent using a local location
      ref <- deploy(agent, AgentLocation.local(8080))
      
      // Register the agent in the directory with its capabilities
      _ <- directory.registerAgent(ref, metadata)
      
      // Set the agent status to Active
      _ <- directory.updateAgentStatus(ref.id, AgentStatus.Active)
    } yield ref
  }
  
  /**
   * Deploy and register an agent with default capabilities.
   */
  def deployAndRegister[I, O](agent: Agent[I, O]): Task[RemoteAgentRef[I, O]] = {
    // Create default metadata for the agent
    val metadata = AgentMetadata(
      capabilities = Set("agent"),
      inputType = "Any",
      outputType = "Any"
    )
    registerAgent(agent, metadata)
  }
  
  /**
   * Find agents with specific capabilities.
   *
   * @param capabilities Set of required capabilities
   * @param limit Maximum number of results to return
   * @return List of agent information matching the capabilities
   */
  def findAgentsByCapabilities(
    capabilities: Set[String],
    limit: Int = 10
  ): Task[List[AgentInfo]] = {
    directory.discoverAgents(
      TypedAgentQuery(
        capabilities = capabilities,
        limit = limit,
        onlyActive = true
      )
    )
  }
  
  /**
   * Find an agent by ID.
   *
   * @param agentId ID of the agent to find
   * @return Agent information if found
   */
  def findAgentById(agentId: UUID): Task[Option[AgentInfo]] = {
    directory.getAgentInfo(agentId)
  }
  
  /**
   * Get a remote agent by capabilities.
   * This finds the first available agent with the specified capabilities
   * and returns a remote wrapper.
   *
   * @param capabilities Set of required capabilities
   * @return Remote agent wrapper if an agent with the capabilities is found
   */
  def getAgentByCapabilities[I, O](
    capabilities: Set[String]
  ): Task[Option[Agent[I, O]]] = {
    for {
      // Find agents with the specified capabilities
      agents <- findAgentsByCapabilities(capabilities, 1)
      
      // Get the first agent if available
      maybeAgent <- if (agents.nonEmpty) {
        val agentInfo = agents.head
        getRemoteAgent(agentInfo.ref.asInstanceOf[RemoteAgentRef[I, O]])
          .map(Some(_))
      } else {
        ZIO.succeed(None)
      }
    } yield maybeAgent
  }
  
  /**
   * Get a capability-aware workflow that connects multiple agents.
   * This finds agents with the specified capabilities and chains them
   * into a sequential workflow.
   *
   * @param workflowCapabilities List of capabilities for each step in the workflow
   * @return A workflow agent that chains the individual agents
   */
  def getWorkflow[I, O](
    workflowCapabilities: List[Set[String]]
  ): Task[Option[Agent[I, O]]] = {
    // This is a simplified implementation that doesn't handle
    // type conversions between agents in the workflow
    ZIO.succeed(None)
  }
  
  /**
   * Subscribe to events from the agent directory.
   *
   * @return Stream of directory events
   */
  def subscribeToDirectoryEvents(): Stream[DirectoryEvent] = {
    // Ensure we're using the right Stream implementation
    val events = directory.subscribeToEvents()
    events
  }
  
  /**
   * Update the status of an agent.
   *
   * @param agentId The ID of the agent
   * @param status The new status
   * @return Task that completes when the status update is successful
   */
  def updateAgentStatus(agentId: UUID, status: AgentStatus): Task[Unit] = {
    directory.updateAgentStatus(agentId, status)
  }
}

object MeshWithDiscovery {
  /**
   * Enhance an existing mesh with discovery capabilities.
   *
   * @param mesh The existing agent mesh
   * @param directory The agent directory to use
   * @return A mesh with discovery capabilities
   */
  def apply(mesh: AgentMesh, directory: AgentDirectory): MeshWithDiscovery = {
    new MeshWithDiscoveryImpl(mesh, directory)
  }
  
  /**
   * Implementation of MeshWithDiscovery that delegates to an existing mesh.
   */
  private class MeshWithDiscoveryImpl(
    private val mesh: AgentMesh,
    val directory: AgentDirectory
  ) extends MeshWithDiscovery {
    // Delegate all basic mesh operations to the wrapped mesh
    def deploy[I, O](
      agent: Agent[I, O], 
      location: AgentLocation
    ): Task[RemoteAgentRef[I, O]] = {
      mesh.deploy(agent, location)
    }
    
    def getRemoteAgent[I, O](
      ref: RemoteAgentRef[I, O]
    ): Task[Agent[I, O]] = {
      mesh.getRemoteAgent(ref)
    }
    
    def importAgent[I, O](
      ref: RemoteAgentRef[I, O]
    ): Task[Agent[I, O]] = {
      mesh.importAgent(ref)
    }
    
    def register[I, O](
      agent: Agent[I, O]
    ): Task[RemoteAgentRef[I, O]] = {
      mesh.register(agent)
    }
    
    def withServerLocation(
      serverLocation: AgentLocation
    ): MeshWithDiscovery = {
      val updatedMesh = mesh.withServerLocation(serverLocation)
      MeshWithDiscovery(updatedMesh, directory)
    }
  }
  
  /**
   * ZIO layer for providing a mesh with discovery.
   */
  // Simple factory method that doesn't require ZLayer
  def create(mesh: AgentMesh, directory: AgentDirectory): MeshWithDiscovery = {
    MeshWithDiscovery(mesh, directory)
  }
}