package com.agenticai.examples.workflow.model

/** Represents a connection between two nodes in a workflow
  *
  * @param sourceId
  *   ID of the source node
  * @param targetId
  *   ID of the target node
  */
case class NodeConnection(
    sourceId: String,
    targetId: String
)