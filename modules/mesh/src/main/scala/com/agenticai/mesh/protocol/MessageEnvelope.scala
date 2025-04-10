package com.agenticai.mesh.protocol

import java.util.UUID

/** Envelope for all protocol messages.
  *
  * @param id
  *   Unique message ID
  * @param messageType
  *   Type of message
  * @param payload
  *   Message payload as bytes
  * @param metadata
  *   Additional metadata
  */
case class MessageEnvelope(
    id: UUID,
    messageType: String,
    payload: Array[Byte],
    metadata: Map[String, String] = Map.empty
):
  /** Get a metadata value.
    *
    * @param key
    *   Metadata key
    * @return
    *   Value if present
    */
  def getMetadata(key: String): Option[String] = metadata.get(key)

  /** Add metadata to this envelope.
    *
    * @param key
    *   Metadata key
    * @param value
    *   Metadata value
    * @return
    *   New envelope with added metadata
    */
  def withMetadata(key: String, value: String): MessageEnvelope =
    copy(metadata = metadata + (key -> value))

  /** Add multiple metadata entries.
    *
    * @param entries
    *   Metadata entries
    * @return
    *   New envelope with added metadata
    */
  def withMetadata(entries: (String, String)*): MessageEnvelope =
    copy(metadata = metadata ++ entries)

  override def equals(that: Any): Boolean = that match
    case other: MessageEnvelope =>
      id == other.id &&
      messageType == other.messageType &&
      java.util.Arrays.equals(payload, other.payload) &&
      metadata == other.metadata
    case _ => false

  override def hashCode(): Int =
    val payloadHash = java.util.Arrays.hashCode(payload)
    31 * (31 * (31 * id.hashCode + messageType.hashCode) + payloadHash) + metadata.hashCode

  override def toString: String =
    s"MessageEnvelope($id, $messageType, [${payload.length} bytes], ${metadata.size} metadata entries)"

object MessageEnvelope:

  /** Create a new message envelope with a random ID.
    *
    * @param messageType
    *   Type of message
    * @param payload
    *   Message payload
    * @param metadata
    *   Additional metadata
    * @return
    *   New message envelope
    */
  def create(
      messageType: String,
      payload: Array[Byte],
      metadata: Map[String, String] = Map.empty
  ): MessageEnvelope =
    MessageEnvelope(UUID.randomUUID(), messageType, payload, metadata)

  /** Message type for agent deployment.
    */
  val AGENT_DEPLOY = "AGENT_DEPLOY"

  /** Message type for agent calls.
    */
  val AGENT_CALL = "AGENT_CALL"

  /** Message type for agent responses.
    */
  val AGENT_RESPONSE = "AGENT_RESPONSE"

  /** Message type for agent retrieval.
    */
  val AGENT_GET = "AGENT_GET"

  /** Message type for agent found response.
    */
  val AGENT_FOUND = "AGENT_FOUND"

  /** Message type for agent not found response.
    */
  val AGENT_NOT_FOUND = "AGENT_NOT_FOUND"

  /** Message type for error response.
    */
  val ERROR = "ERROR"
