package com.agenticai.mesh

import zio.*
import com.agenticai.core.agent.Agent
import com.agenticai.mesh.protocol.*
import com.agenticai.mesh.discovery.*
import com.agenticai.mesh.discovery.AgentQueryConverter
import java.util.UUID

/** Implementation of AgentMeshWithDiscovery that combines an AgentMesh with an AgentDirectory to
  * provide capability-based discovery.
  *
  * @param mesh
  *   The underlying agent mesh
  * @param directory
  *   The agent directory for discovery
  */
private[mesh] class AgentMeshWithDiscoveryImpl(
    private val mesh: AgentMesh,
    private val directory: AgentDirectory
) extends AgentMeshWithDiscovery:

  /** Deploy an agent to a remote location. Delegates to the underlying mesh.
    */
  override def deploy[I, O](
      agent: Agent[I, O],
      location: AgentLocation
  ): Task[RemoteAgentRef[I, O]] =
    mesh.deploy(agent, location)

  /** Get a remote agent wrapper. Delegates to the underlying mesh.
    */
  override def getRemoteAgent[I, O](
      ref: RemoteAgentRef[I, O]
  ): Task[Agent[I, O]] =
    mesh.getRemoteAgent(ref)

  /** Import a remote agent from another mesh node. Delegates to the underlying mesh.
    */
  override def importAgent[I, O](
      ref: RemoteAgentRef[I, O]
  ): Task[Agent[I, O]] =
    mesh.importAgent(ref)

  /** Register a local agent in the mesh. Delegates to the underlying mesh.
    */
  override def register[I, O](
      agent: Agent[I, O]
  ): Task[RemoteAgentRef[I, O]] =
    mesh.register(agent)

  /** Create a new mesh with a specific server location. Returns a new instance with both mesh and
    * directory.
    */
  override def withServerLocation(serverLocation: AgentLocation): AgentMesh =
    new AgentMeshWithDiscoveryImpl(
      mesh.withServerLocation(serverLocation),
      directory
    )

  /** Deploy an agent with specified capabilities. Combines deploying the agent via the mesh and
    * registering its capabilities.
    */
  override def deployWithCapabilities[I, O](
      agent: Agent[I, O],
      location: AgentLocation,
      metadata: AgentMetadata
  ): Task[RemoteAgentRef[I, O]] =
    for
      // Deploy the agent to get a remote reference
      ref <- deploy(agent, location)

      // Register the agent with its capabilities in the directory
      _ <- directory.registerAgent(ref, metadata)
    yield ref

  /** Register an agent with specified capabilities. Combines registering the agent in the mesh and
    * its capabilities in the directory.
    */
  override def registerWithCapabilities[I, O](
      agent: Agent[I, O],
      metadata: AgentMetadata
  ): Task[RemoteAgentRef[I, O]] =
    for
      // Register the agent in the mesh
      ref <- register(agent)

      // Register the agent's capabilities in the directory
      _ <- directory.registerAgent(ref, metadata)
    yield ref

  /** Discover agents matching a capability query. Delegates to the directory.
    */
  override def discoverAgents(query: AgentQuery): Task[List[AgentInfo]] =
    // Create a TypedAgentQuery directly instead of using the converter
    val typedQuery = TypedAgentQuery(
      capabilities = query.capabilities,
      inputType = query.inputType,
      outputType = query.outputType,
      properties = query.properties,
      limit = query.limit,
      onlyActive = query.onlyActive
    )
    directory.discoverAgents(typedQuery)

  /** Get detailed information about a specific agent. Delegates to the directory.
    */
  override def getAgentInfo(agentId: UUID): Task[Option[AgentInfo]] =
    directory.getAgentInfo(agentId)

  /** Update the status of an agent. Delegates to the directory.
    */
  override def updateAgentStatus(agentId: UUID, status: AgentStatus): Task[Unit] =
    directory.updateAgentStatus(agentId, status)

  /** Subscribe to directory events. Delegates to the directory.
    */
  override def subscribeToDirectoryEvents(): Stream[DirectoryEvent] =
    // Use our single-parameter Stream
    directory.subscribeToEvents()

  /** Unregister an agent from the mesh and directory.
    *
    * @param agentId
    *   The ID of the agent to unregister
    * @return
    *   Task that completes when unregistration is successful
    */
  def unregisterAgent(agentId: UUID): Task[Unit] =
    // For now, just unregister from the directory
    // In a more complete implementation, we would also need to
    // handle removing the agent from the underlying mesh
    directory.unregisterAgent(agentId)

  /** Get all registered agents.
    *
    * @return
    *   List of all agent information
    */
  def getAllAgents(): Task[List[AgentInfo]] =
    directory.getAllAgents()
