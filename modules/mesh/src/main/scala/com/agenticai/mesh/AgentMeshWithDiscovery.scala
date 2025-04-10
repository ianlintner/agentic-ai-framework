package com.agenticai.mesh

import zio.*
import com.agenticai.core.agent.Agent
import com.agenticai.mesh.protocol.*
import com.agenticai.mesh.discovery.*
import com.agenticai.mesh.discovery.Stream.WithError
import java.util.UUID

/** Enhanced AgentMesh interface with discovery capabilities.
  *
  * This extends the core AgentMesh interface with methods for agent discovery, capability matching,
  * and status monitoring.
  */
trait AgentMeshWithDiscovery extends AgentMesh:

  /** Deploy an agent with specified capabilities.
    *
    * @param agent
    *   Agent to deploy
    * @param location
    *   Location to deploy to
    * @param metadata
    *   Metadata describing the agent's capabilities
    * @return
    *   Remote reference to the deployed agent
    */
  def deployWithCapabilities[I, O](
      agent: Agent[I, O],
      location: AgentLocation,
      metadata: AgentMetadata
  ): Task[RemoteAgentRef[I, O]]

  /** Register an agent with specified capabilities.
    *
    * @param agent
    *   Agent to register
    * @param metadata
    *   Metadata describing the agent's capabilities
    * @return
    *   Remote reference to the registered agent
    */
  def registerWithCapabilities[I, O](
      agent: Agent[I, O],
      metadata: AgentMetadata
  ): Task[RemoteAgentRef[I, O]]

  /** Discover agents matching a capability query.
    *
    * @param query
    *   The query specifying search criteria
    * @return
    *   List of matching agent references with their info
    */
  def discoverAgents(query: AgentQuery): Task[List[AgentInfo]]

  /** Find agents with specific capabilities.
    *
    * @param capabilities
    *   Set of required capabilities
    * @param limit
    *   Maximum number of results
    * @return
    *   List of matching agent references with their info
    */
  def findAgentsByCapabilities(
      capabilities: Set[String],
      limit: Int = 10
  ): Task[List[AgentInfo]] =
    discoverAgents(AgentQuery(capabilities = capabilities, limit = limit))

  /** Find agents that can process specific input and output types.
    *
    * @param inputType
    *   Required input type
    * @param outputType
    *   Required output type
    * @param limit
    *   Maximum number of results
    * @return
    *   List of matching agent references with their info
    */
  def findAgentsByTypes(
      inputType: String,
      outputType: String,
      limit: Int = 10
  ): Task[List[AgentInfo]] =
    discoverAgents(
      AgentQuery(
        inputType = Some(inputType),
        outputType = Some(outputType),
        limit = limit
      )
    )

  /** Get detailed information about a specific agent.
    *
    * @param agentId
    *   The ID of the agent
    * @return
    *   Agent information if found
    */
  def getAgentInfo(agentId: UUID): Task[Option[AgentInfo]]

  /** Update the status of an agent.
    *
    * @param agentId
    *   The ID of the agent
    * @param status
    *   The new status
    * @return
    *   Task that completes when the update is successful
    */
  def updateAgentStatus(agentId: UUID, status: AgentStatus): Task[Unit]

  /** Subscribe to directory events.
    *
    * @return
    *   Stream of directory events
    */
  def subscribeToDirectoryEvents(): Stream[DirectoryEvent]

  /** Find the best agent that matches the given query.
    *
    * @param query
    *   The query specifying search criteria
    * @return
    *   The best matching agent info if found
    */
  def findBestAgent(query: AgentQuery): Task[Option[AgentInfo]] =
    discoverAgents(query.copy(limit = 1)).map(_.headOption)

  /** Get a remote agent wrapper from agent info.
    *
    * @param info
    *   Agent information containing the remote reference
    * @return
    *   Remote agent that behaves like a local agent
    */
  def getAgent(info: AgentInfo): Task[Agent[_, _]] =
    getRemoteAgent(info.ref.asInstanceOf[RemoteAgentRef[Any, Any]])
      .map(_.asInstanceOf[Agent[_, _]])

object AgentMeshWithDiscovery:

  /** Create a new agent mesh with discovery capabilities.
    *
    * @return
    *   AgentMeshWithDiscovery instance
    */
  def apply(): AgentMeshWithDiscovery =
    val directory = AgentDirectory.inMemory
    val mesh      = AgentMesh()
    new AgentMeshWithDiscoveryImpl(mesh, directory)

  /** Create a new agent mesh with specified protocol, server location, and directory.
    *
    * @param protocol
    *   Protocol to use
    * @param serverLocation
    *   Default server location
    * @param directory
    *   Agent directory implementation
    * @return
    *   AgentMeshWithDiscovery instance
    */
  def apply(
      protocol: Protocol,
      serverLocation: AgentLocation,
      directory: AgentDirectory
  ): AgentMeshWithDiscovery =
    val mesh = AgentMesh(protocol, serverLocation)
    new AgentMeshWithDiscoveryImpl(mesh, directory)
