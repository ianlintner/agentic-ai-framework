package com.agenticai.examples.workflow.model

import zio.*
import java.util.UUID

/** Represents a complete workflow with nodes and connections
  *
  * @param id
  *   Unique identifier for the workflow
  * @param name
  *   Human-readable name for the workflow
  * @param nodes
  *   Nodes in the workflow
  * @param connections
  *   Connections between nodes
  */
case class Workflow(
    id: String = UUID.randomUUID().toString,
    name: String,
    nodes: List[BaseWorkflowNode],
    connections: List[NodeConnection]
):
  /** Find a node by its ID
    *
    * @param nodeId
    *   ID of the node to find
    * @return
    *   Option containing the node if found
    */
  def findNode(nodeId: String): Option[BaseWorkflowNode] =
    nodes.find(_.id == nodeId)

  /** Find all nodes that connect to a specific node
    *
    * @param nodeId
    *   ID of the node to find connections to
    * @return
    *   List of nodes that connect to the specified node
    */
  def findInputNodes(nodeId: String): List[BaseWorkflowNode] =
    connections
      .filter(_.targetId == nodeId)
      .flatMap(conn => findNode(conn.sourceId))

  /** Find all nodes that a specific node connects to
    *
    * @param nodeId
    *   ID of the node to find connections from
    * @return
    *   List of nodes that the specified node connects to
    */
  def findOutputNodes(nodeId: String): List[BaseWorkflowNode] =
    connections
      .filter(_.sourceId == nodeId)
      .flatMap(conn => findNode(conn.targetId))

  /** Find all entry nodes (nodes with no incoming connections)
    *
    * @return
    *   List of entry nodes
    */
  def findEntryNodes(): List[BaseWorkflowNode] =
    nodes.filter(node =>
      !connections.exists(_.targetId == node.id)
    )

  /** Find all exit nodes (nodes with no outgoing connections)
    *
    * @return
    *   List of exit nodes
    */
  def findExitNodes(): List[BaseWorkflowNode] =
    nodes.filter(node =>
      !connections.exists(_.sourceId == node.id)
    )