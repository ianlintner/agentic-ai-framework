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
  
  // Event listener management
  private val eventListeners = new java.util.concurrent.CopyOnWriteArrayList[DirectoryEvent => Unit]()
  
  private def publishEvent(event: DirectoryEvent): Unit = {
    import scala.jdk.CollectionConverters._
    eventListeners.asScala.foreach(listener => listener(event))
  }
  
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
    
    ZIO.succeed {
      // Update the agent store
      agents.put(agentId, info)
      
      // Publish the registration event
      publishEvent(DirectoryEvent.AgentRegistered(agentId, info))
    }
  }
  
  /**
   * Unregister an agent from the directory.
   */
  override def unregisterAgent(agentId: UUID): Task[Unit] = {
    ZIO.succeed {
      // Remove the agent from the store
      agents.remove(agentId)
      
      // Publish the unregistration event
      publishEvent(DirectoryEvent.AgentUnregistered(agentId))
    }
  }
  
  /**
   * Discover agents matching specific criteria.
   */
  override def discoverAgents(query: TypedAgentQuery): Task[List[AgentInfo]] = {
    ZIO.succeed {
      agents.values
        .filter(info => query.matches(info))
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
  override def subscribeToEvents(): Stream[DirectoryEvent] = {
    new Stream[DirectoryEvent] {
      def forEach(listener: DirectoryEvent => Unit): Unit = {
        eventListeners.add(listener)
      }
    }
  }
  
  /**
   * Update the status of an agent.
   */
  override def updateAgentStatus(agentId: UUID, status: AgentStatus): Task[Unit] = {
    for {
      // Get the current agent info
      maybeInfoOpt <- ZIO.succeed(agents.get(agentId))
      _ <- ZIO.when(maybeInfoOpt.isEmpty)(
        ZIO.fail(new NoSuchElementException(s"Agent not found: $agentId"))
      )
      result <- ZIO.succeed {
        maybeInfoOpt.foreach { info =>
          // Create updated info with new status
          val oldStatus = info.status
          val updatedInfo = info.withStatus(status)
          
          // Update the agent store
          agents.put(agentId, updatedInfo)
          
          // Publish the status change event
          publishEvent(DirectoryEvent.AgentStatusChanged(agentId, oldStatus, status))
        }
      }
    } yield result
  }
  
  /**
   * Get all registered agents.
   */
  override def getAllAgents(): Task[List[AgentInfo]] = {
    ZIO.succeed(agents.values.toList)
  }
  
  /**
   * Update an agent's metadata.
   */
  def updateAgentMetadata(agentId: UUID, metadata: AgentMetadata): Task[Unit] = {
    for {
      // Get the current agent info
      maybeInfoOpt <- ZIO.succeed(agents.get(agentId))
      _ <- ZIO.when(maybeInfoOpt.isEmpty)(
        ZIO.fail(new NoSuchElementException(s"Agent not found: $agentId"))
      )
      result <- ZIO.succeed {
        maybeInfoOpt.foreach { info =>
          // Create updated info with new metadata
          val oldMetadata = info.metadata
          val updatedInfo = info.copy(
            metadata = metadata,
            lastUpdated = Instant.now()
          )
          
          // Update the agent store
          agents.put(agentId, updatedInfo)
          
          // Publish the metadata update event
          publishEvent(DirectoryEvent.AgentMetadataUpdated(agentId, oldMetadata, metadata))
        }
      }
    } yield result
  }
  
  /**
   * Update an agent's load factor.
   */
  def updateAgentLoadFactor(agentId: UUID, loadFactor: Double): Task[Unit] = {
    for {
      // Get the current agent info
      maybeInfoOpt <- ZIO.succeed(agents.get(agentId))
      _ <- ZIO.when(maybeInfoOpt.isEmpty)(
        ZIO.fail(new NoSuchElementException(s"Agent not found: $agentId"))
      )
      result <- ZIO.succeed {
        maybeInfoOpt.foreach { info =>
          // Create updated info with new load factor
          val oldLoadFactor = info.loadFactor
          val updatedInfo = info.withLoadFactor(loadFactor)
          
          // Update the agent store
          agents.put(agentId, updatedInfo)
          
          // Publish the load factor change event
          publishEvent(DirectoryEvent.AgentLoadChanged(agentId, oldLoadFactor, loadFactor))
        }
      }
    } yield result
  }
}