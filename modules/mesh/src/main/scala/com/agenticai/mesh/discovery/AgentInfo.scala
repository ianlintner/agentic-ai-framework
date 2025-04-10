package com.agenticai.mesh.discovery

import com.agenticai.mesh.protocol.RemoteAgentRef
import java.time.Instant
import java.util.UUID

/** Comprehensive information about an agent in the mesh.
  *
  * @param agentId
  *   Unique identifier for the agent
  * @param ref
  *   Remote reference to the agent
  * @param metadata
  *   Metadata describing the agent's capabilities
  * @param status
  *   Current status of the agent
  * @param registeredAt
  *   Time when the agent was registered
  * @param lastUpdated
  *   Time of the last status update
  * @param loadFactor
  *   Optional load factor indicating agent utilization (0.0-1.0)
  */
case class AgentInfo(
    agentId: UUID,
    ref: RemoteAgentRef[?, ?],
    metadata: AgentMetadata,
    status: AgentStatus,
    registeredAt: Instant,
    lastUpdated: Instant,
    loadFactor: Option[Double] = None
):

  /** Check if the agent has a specific capability.
    *
    * @param capability
    *   The capability to check for
    * @return
    *   True if the agent has the capability
    */
  def hasCapability(capability: String): Boolean =
    metadata.capabilities.contains(capability)

  /** Check if the agent is currently active.
    *
    * @return
    *   True if the agent is active
    */
  def isActive: Boolean = status == AgentStatus.Active

  /** Create a copy with updated status.
    *
    * @param newStatus
    *   The new status
    * @return
    *   Updated agent info
    */
  def withStatus(newStatus: AgentStatus): AgentInfo =
    copy(status = newStatus, lastUpdated = Instant.now())

  /** Create a copy with updated load factor.
    *
    * @param newLoadFactor
    *   The new load factor
    * @return
    *   Updated agent info
    */
  def withLoadFactor(newLoadFactor: Double): AgentInfo =
    copy(loadFactor = Some(newLoadFactor), lastUpdated = Instant.now())
