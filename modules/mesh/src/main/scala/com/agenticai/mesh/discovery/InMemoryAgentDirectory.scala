package com.agenticai.mesh.discovery

import zio._
import com.agenticai.mesh.protocol.RemoteAgentRef
import java.time.Instant
import java.util.UUID
import scala.collection.concurrent.TrieMap

/**
 * In-memory implementation of the AgentDirectory.
 * 
 * This implementation is suitable for testing and small-scale deployments.
 * It stores all agent information in memory and provides simple event publishing.
 */
class InMemoryAgentDirectory extends AgentDirectory {
  // In-memory storage for agent information
  private val agents = TrieMap.empty[UUID, AgentInfo]
  
  // Queue for directory events
  private val eventHub = Runtime.default.unsafe.run(
    Hub.bounded[DirectoryEvent](100)
  ).getOrThrow()
  
  /**
   * Register an agent with its capabilities and metadata.
   */
  override def registerAgent[I, O](
    ref: RemoteAgentRef[I, O], 
    metadata: AgentMetadata
  ): Task[Unit] = {
    val agentId = ref.id
    val now = Instant.now()
    
    // Create the agent info
    val info = AgentInfo(
      agentId = agentId,
      ref = ref,
      metadata = metadata,
      status = AgentStatus.Active,
      registeredAt = now,
      lastUpdated = now
    )
    
    for {
      // Update the agent store
      _ <- ZIO.succeed(agents.put(agentId, info))
      
      // Publish the registration event
      event = DirectoryEvent.AgentRegistered(agentId, info)
      _ <- eventHub.publish(event)
    } yield ()
  }
  
  /**
   * Unregister an agent from the directory.
   */
  override def unregisterAgent(agentId: UUID): Task[Unit] = {
    for {
      // Remove the agent from the store
      _ <- ZIO.succeed(agents.remove(agentId))
      
      // Publish the unregistration event
      event = DirectoryEvent.AgentUnregistered(agentId)
      _ <- eventHub.publish(event)
    } yield ()
  }
  
  /**
   * Discover agents matching specific criteria.
   */
  override def discoverAgents(query: AgentQuery): Task[List[AgentInfo]] = {
    ZIO.succeed {
      agents.values
        .filter(query.matches)
        .toList
        .sortBy(-_.registeredAt.toEpochMilli) // Sort by registration time (newest first)
        .take(query.limit)
    }
  }
  
  /**
   * Get detailed information about a specific agent.
   */
  override def getAgentInfo(agentId: UUID): Task[Option[AgentInfo]] = {
    ZIO.succeed(agents.get(agentId))
  }
  
  /**
   * Subscribe to events from the agent directory.
   */
  override def subscribeToEvents(): Stream[Throwable, DirectoryEvent] = {
    ZStream.fromHub(eventHub)
  }
  
  /**
   * Update the status of an agent.
   */
  override def updateAgentStatus(agentId: UUID, status: AgentStatus): Task[Unit] = {
    for {
      // Get the current agent info
      maybeInfo <- ZIO.succeed(agents.get(agentId))
      info <- ZIO.fromOption(maybeInfo)
        .orElseFail(new NoSuchElementException(s"Agent not found: $agentId"))
      
      // Create updated info with new status
      oldStatus = info.status
      updatedInfo = info.withStatus(status)
      
      // Update the agent store
      _ <- ZIO.succeed(agents.put(agentId, updatedInfo))
      
      // Publish the status change event
      event = DirectoryEvent.AgentStatusChanged(agentId, oldStatus, status)
      _ <- eventHub.publish(event)
    } yield ()
  }
  
  /**
   * Get all registered agents.
   */
  override def getAllAgents(): Task[List[AgentInfo]] = {
    ZIO.succeed(agents.values.toList)
  }
  
  /**
   * Update an agent's metadata.
   *
   * @param agentId The ID of the agent
   * @param metadata The new metadata
   * @return Task that completes when the update is successful
   */
  def updateAgentMetadata(agentId: UUID, metadata: AgentMetadata): Task[Unit] = {
    for {
      // Get the current agent info
      maybeInfo <- ZIO.succeed(agents.get(agentId))
      info <- ZIO.fromOption(maybeInfo)
        .orElseFail(new NoSuchElementException(s"Agent not found: $agentId"))
      
      // Create updated info with new metadata
      oldMetadata = info.metadata
      updatedInfo = info.copy(
        metadata = metadata,
        lastUpdated = Instant.now()
      )
      
      // Update the agent store
      _ <- ZIO.succeed(agents.put(agentId, updatedInfo))
      
      // Publish the metadata update event
      event = DirectoryEvent.AgentMetadataUpdated(agentId, oldMetadata, metadata)
      _ <- eventHub.publish(event)
    } yield ()
  }
  
  /**
   * Update an agent's load factor.
   *
   * @param agentId The ID of the agent
   * @param loadFactor The new load factor (0.0-1.0)
   * @return Task that completes when the update is successful
   */
  def updateAgentLoadFactor(agentId: UUID, loadFactor: Double): Task[Unit] = {
    for {
      // Get the current agent info
      maybeInfo <- ZIO.succeed(agents.get(agentId))
      info <- ZIO.fromOption(maybeInfo)
        .orElseFail(new NoSuchElementException(s"Agent not found: $agentId"))
      
      // Create updated info with new load factor
      oldLoadFactor = info.loadFactor
      updatedInfo = info.withLoadFactor(loadFactor)
      
      // Update the agent store
      _ <- ZIO.succeed(agents.put(agentId, updatedInfo))
      
      // Publish the load factor change event
      event = DirectoryEvent.AgentLoadChanged(agentId, oldLoadFactor, loadFactor)
      _ <- eventHub.publish(event)
    } yield ()
  }
}