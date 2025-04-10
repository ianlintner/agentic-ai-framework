package com.agenticai.mesh.protocol

import java.util.UUID

/** Reference to a remote agent.
  *
  * @param id
  *   Unique ID of the agent
  * @param location
  *   Location of the agent
  * @param inputType
  *   Input type name for serialization
  * @param outputType
  *   Output type name for serialization
  * @tparam I
  *   Input type
  * @tparam O
  *   Output type
  */
case class RemoteAgentRef[I, O](
    id: UUID,
    location: AgentLocation,
    inputType: String,
    outputType: String
):
  /** Create a URI for this agent reference.
    *
    * @return
    *   URI for this agent
    */
  def uri: String = s"${location.uri}/agents/$id"

  /** Check if this agent is local.
    *
    * @return
    *   True if this agent is on a local server
    */
  def isLocal: Boolean = location.isLocal

  /** Create a string representation.
    *
    * @return
    *   String representation
    */
  override def toString: String =
    s"RemoteAgentRef($id, $location, $inputType -> $outputType)"

object RemoteAgentRef:

  /** Create a new agent reference with a random ID.
    *
    * @param location
    *   Location of the agent
    * @param inputType
    *   Input type name for serialization
    * @param outputType
    *   Output type name for serialization
    * @return
    *   New agent reference
    */
  def create[I, O](
      location: AgentLocation,
      inputType: String,
      outputType: String
  ): RemoteAgentRef[I, O] =
    RemoteAgentRef(UUID.randomUUID(), location, inputType, outputType)

  /** Create a new agent reference with a random ID, inferring type names.
    *
    * @param location
    *   Location of the agent
    * @return
    *   New agent reference
    */
  def create[I: scala.reflect.ClassTag, O: scala.reflect.ClassTag](
      location: AgentLocation
  ): RemoteAgentRef[I, O] =
    val inputType  = scala.reflect.classTag[I].runtimeClass.getName
    val outputType = scala.reflect.classTag[O].runtimeClass.getName
    create[I, O](location, inputType, outputType)
