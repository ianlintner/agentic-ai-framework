package com.agenticai.mesh

import zio.*
import com.agenticai.core.agent.Agent
import com.agenticai.mesh.protocol.*

/** High-level API for interacting with distributed agents.
  *
  * AgentMesh provides a unified interface for deploying, discovering, and communicating with agents
  * across a distributed network of nodes.
  */
trait AgentMesh:

  /** Deploy an agent to a remote location.
    *
    * @param agent
    *   Agent to deploy
    * @param location
    *   Location to deploy to
    * @return
    *   Remote reference to the deployed agent
    */
  def deploy[I, O](
      agent: Agent[I, O],
      location: AgentLocation
  ): Task[RemoteAgentRef[I, O]]

  /** Get a remote agent wrapper.
    *
    * @param ref
    *   Remote agent reference
    * @return
    *   Remote agent that behaves like a local agent
    */
  def getRemoteAgent[I, O](
      ref: RemoteAgentRef[I, O]
  ): Task[Agent[I, O]]

  /** Import a remote agent from another mesh node.
    *
    * @param ref
    *   Remote agent reference
    * @return
    *   Imported agent
    */
  def importAgent[I, O](
      ref: RemoteAgentRef[I, O]
  ): Task[Agent[I, O]]

  /** Register a local agent in the mesh.
    *
    * @param agent
    *   Agent to register
    * @return
    *   Reference to the registered agent
    */
  def register[I, O](
      agent: Agent[I, O]
  ): Task[RemoteAgentRef[I, O]]

  /** Create a new mesh with a specific server location.
    *
    * @param serverLocation
    *   Location of the mesh server
    * @return
    *   New mesh instance
    */
  def withServerLocation(serverLocation: AgentLocation): AgentMesh

object AgentMesh:
  /** Create a new agent mesh with default protocol.
    *
    * @return
    *   Agent mesh instance
    */
  def apply(): AgentMesh = new AgentMeshImpl(Protocol.inMemory, AgentLocation.local(8080))

  /** Create a new agent mesh with specified protocol and server location.
    *
    * @param protocol
    *   Protocol to use
    * @param serverLocation
    *   Default server location
    * @return
    *   Agent mesh instance
    */
  def apply(protocol: Protocol, serverLocation: AgentLocation): AgentMesh =
    new AgentMeshImpl(protocol, serverLocation)

  /** Implementation of AgentMesh.
    */
  private class AgentMeshImpl(protocol: Protocol, defaultLocation: AgentLocation) extends AgentMesh:
    private val localAgents = scala.collection.concurrent.TrieMap[String, Agent[_, _]]()

    def deploy[I, O](
        agent: Agent[I, O],
        location: AgentLocation
    ): Task[RemoteAgentRef[I, O]] =
      protocol.sendAgent(agent, location)

    def getRemoteAgent[I, O](
        ref: RemoteAgentRef[I, O]
    ): Task[Agent[I, O]] =
      // Create a wrapper that calls the remote agent
      ZIO.succeed {
        new Agent[I, O]:
          def process(input: I): Task[O] =
            protocol.callRemoteAgent(ref, input)

          override def toString: String = s"RemoteAgent(${ref.id}, ${ref.location})"
      }

    def importAgent[I, O](
        ref: RemoteAgentRef[I, O]
    ): Task[Agent[I, O]] =
      for
        maybeAgent <- protocol.getRemoteAgent(ref)
        agent <- ZIO
          .fromOption(maybeAgent)
          .orElseFail(
            new NoSuchElementException(s"Agent not found: ${ref.id} at ${ref.location}")
          )
      yield agent

    def register[I, O](
        agent: Agent[I, O]
    ): Task[RemoteAgentRef[I, O]] =
      deploy(agent, defaultLocation)

    def withServerLocation(serverLocation: AgentLocation): AgentMesh =
      new AgentMeshImpl(protocol, serverLocation)
