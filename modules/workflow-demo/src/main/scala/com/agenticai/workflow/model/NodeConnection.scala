package com.agenticai.workflow.model

import zio.json._

/**
 * Represents a connection between two nodes in a workflow
 *
 * @param id Unique identifier for the connection
 * @param sourceNodeId ID of the source node (output)
 * @param targetNodeId ID of the target node (input)
 */
case class NodeConnection(
  id: String,
  sourceNodeId: String,
  targetNodeId: String
)

object NodeConnection {
  // JSON encoder/decoder for NodeConnection
  implicit val encoder: JsonEncoder[NodeConnection] = DeriveJsonEncoder.gen[NodeConnection]
  implicit val decoder: JsonDecoder[NodeConnection] = DeriveJsonDecoder.gen[NodeConnection]
}