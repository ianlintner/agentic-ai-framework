package com.agenticai.mesh.discovery

import java.time.Instant
import java.util.UUID

/** Represents an event that occurred in the agent directory.
  */
sealed trait DirectoryEvent:
  /** The ID of the agent involved in the event.
    */
  def agentId: UUID

  /** When the event occurred.
    */
  def timestamp: Instant

object DirectoryEvent:

  /** Event indicating an agent has been registered.
    *
    * @param agentId
    *   ID of the registered agent
    * @param info
    *   Complete information about the agent
    * @param timestamp
    *   When the registration occurred
    */
  case class AgentRegistered(
      agentId: UUID,
      info: AgentInfo,
      timestamp: Instant = Instant.now()
  ) extends DirectoryEvent

  /** Event indicating an agent has been unregistered.
    *
    * @param agentId
    *   ID of the unregistered agent
    * @param timestamp
    *   When the unregistration occurred
    */
  case class AgentUnregistered(
      agentId: UUID,
      timestamp: Instant = Instant.now()
  ) extends DirectoryEvent

  /** Event indicating an agent's status has changed.
    *
    * @param agentId
    *   ID of the agent
    * @param oldStatus
    *   Previous status
    * @param newStatus
    *   New status
    * @param timestamp
    *   When the status change occurred
    */
  case class AgentStatusChanged(
      agentId: UUID,
      oldStatus: AgentStatus,
      newStatus: AgentStatus,
      timestamp: Instant = Instant.now()
  ) extends DirectoryEvent

  /** Event indicating an agent's metadata has been updated.
    *
    * @param agentId
    *   ID of the agent
    * @param oldMetadata
    *   Previous metadata
    * @param newMetadata
    *   New metadata
    * @param timestamp
    *   When the metadata update occurred
    */
  case class AgentMetadataUpdated(
      agentId: UUID,
      oldMetadata: AgentMetadata,
      newMetadata: AgentMetadata,
      timestamp: Instant = Instant.now()
  ) extends DirectoryEvent

  /** Event indicating an agent's load factor has changed.
    *
    * @param agentId
    *   ID of the agent
    * @param oldLoadFactor
    *   Previous load factor (if any)
    * @param newLoadFactor
    *   New load factor
    * @param timestamp
    *   When the load factor change occurred
    */
  case class AgentLoadChanged(
      agentId: UUID,
      oldLoadFactor: Option[Double],
      newLoadFactor: Double,
      timestamp: Instant = Instant.now()
  ) extends DirectoryEvent
