package com.agenticai.workflow.model

import zio.json._

/**
 * Position of a node in the workflow canvas
 *
 * @param x X coordinate
 * @param y Y coordinate
 */
case class NodePosition(x: Int, y: Int)

object NodePosition {
  implicit val encoder: JsonEncoder[NodePosition] = DeriveJsonEncoder.gen[NodePosition]
  implicit val decoder: JsonDecoder[NodePosition] = DeriveJsonDecoder.gen[NodePosition]
}

/**
 * Represents a node in a workflow
 *
 * @param id Unique identifier for the node
 * @param nodeType Type of the node (e.g., "text-transformer", "summarizer", etc.)
 * @param label Human-readable label for the node
 * @param configuration Map of configuration parameters specific to this node type
 * @param position Position of the node in the workflow canvas
 */
case class WorkflowNode(
  id: String,
  nodeType: String,
  label: String,
  configuration: Map[String, String],
  position: NodePosition
)

object WorkflowNode {
  // JSON encoder/decoder for WorkflowNode
  implicit val encoder: JsonEncoder[WorkflowNode] = DeriveJsonEncoder.gen[WorkflowNode]
  implicit val decoder: JsonDecoder[WorkflowNode] = DeriveJsonDecoder.gen[WorkflowNode]
}