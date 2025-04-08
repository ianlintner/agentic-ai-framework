package com.agenticai.mesh.discovery

import zio._
import com.agenticai.mesh.protocol.RemoteAgentRef
import java.util.UUID

/**
 * Core interface for agent discovery in the mesh.
 * 
 * The AgentDirectory provides mechanisms for registering agents,
 * discovering agents based on capabilities, and receiving
 * notifications about agent availability changes.
 */
trait AgentDirectory {
  /**
   * Register an agent with its capabilities and metadata.
   *
   * @param ref The reference to the agent being registered
   * @param metadata The agent's metadata including capabilities
   * @return Task that completes when registration is successful
   */
  def registerAgent[I, O](
    ref: RemoteAgentRef[I, O], 
    metadata: AgentMetadata
  ): Task[Unit]
  
  /**
   * Unregister an agent from the directory.
   *
   * @param agentId The ID of the agent to unregister
   * @return Task that completes when unregistration is successful
   */
  def unregisterAgent(agentId: UUID): Task[Unit]
  
  /**
   * Discover agents matching specific criteria.
   *
   * @param query The query specifying the search criteria
   * @return List of agent references that match the criteria
   */
  def discoverAgents(query: TypedAgentQuery): Task[List[AgentInfo]]
  
  /**
   * Get detailed information about a specific agent.
   *
   * @param agentId The ID of the agent
   * @return Agent information if found
   */
  def getAgentInfo(agentId: UUID): Task[Option[AgentInfo]]
  
  /**
   * Subscribe to events from the agent directory.
   *
   * @return Stream of directory events
   */
  def subscribeToEvents(): Stream[DirectoryEvent]
  
  /**
   * Update the status of an agent.
   *
   * @param agentId The ID of the agent
   * @param status The new status
   * @return Task that completes when the status update is successful
   */
  def updateAgentStatus(agentId: UUID, status: AgentStatus): Task[Unit]
  
  /**
   * Get all registered agents.
   *
   * @return List of all agent information
   */
  def getAllAgents(): Task[List[AgentInfo]]
}

/**
 * Companion object for AgentDirectory.
 */
object AgentDirectory {
  /**
   * Create an in-memory agent directory.
   *
   * @return In-memory implementation of AgentDirectory
   */
  def inMemory: AgentDirectory = new InMemoryAgentDirectory()
}