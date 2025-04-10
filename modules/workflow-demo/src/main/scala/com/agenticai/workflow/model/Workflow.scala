package com.agenticai.workflow.model

import zio.json.*

/** Represents a complete workflow with nodes and connections
  *
  * @param id
  *   Unique identifier for the workflow
  * @param name
  *   Display name of the workflow
  * @param description
  *   Human-readable description of what the workflow does
  * @param nodes
  *   List of workflow nodes in this workflow
  * @param connections
  *   List of connections between nodes
  */
case class Workflow(
    id: String,
    name: String,
    description: String,
    nodes: List[WorkflowNode],
    connections: List[NodeConnection]
)

object Workflow:
  // JSON encoder/decoder for Workflow
  implicit val encoder: JsonEncoder[Workflow] = DeriveJsonEncoder.gen[Workflow]
  implicit val decoder: JsonDecoder[Workflow] = DeriveJsonDecoder.gen[Workflow]
