package com.agenticai.examples.workflow.engine

import com.agenticai.examples.workflow.model.*
import com.agenticai.examples.workflow.agent.*
import zio.*
import java.util.UUID
import scala.collection.mutable
import scala.reflect.{classTag, ClassTag}

/** Engine for executing workflows
  */
class WorkflowEngine(transformerAgent: TextTransformerAgent,
                     splitterAgent: TextSplitterAgent,
                     summarizationAgent: SummarizationAgent,
                     buildAgent: BuildAgent):

  private val workflowCache = mutable.Map[String, Workflow]()
  private val resultCache = mutable.Map[String, Any]()

  /** Create a simple text processing workflow
    *
    * @return
    *   A workflow that transforms, splits, and summarizes text
    */
  def createTextProcessingWorkflow(): Workflow =
    // Create nodes
    val transformerNode = WorkflowNode[String, String](
      name = "Text Transformer",
      agent = transformerAgent,
      inputType = "String",
      outputType = "String"
    )
    
    val splitterNode = WorkflowNode[String, List[List[String]]](
      name = "Text Splitter",
      agent = splitterAgent,
      inputType = "String",
      outputType = "List[List[String]]"
    )
    
    val summarizerNode = WorkflowNode[List[List[String]], String](
      name = "Summarizer",
      agent = summarizationAgent,
      inputType = "List[List[String]]",
      outputType = "String"
    )
    
    val buildNode = WorkflowNode[String, String](
      name = "Build",
      agent = buildAgent,
      inputType = "String",
      outputType = "String"
    )
    
    // Create connections
    val connections = List(
      NodeConnection(transformerNode.id, splitterNode.id),
      NodeConnection(splitterNode.id, summarizerNode.id),
      NodeConnection(summarizerNode.id, buildNode.id)
    )
    
    // Create workflow
    val workflow = Workflow(
      name = "Text Processing Workflow",
      nodes = List(transformerNode, splitterNode, summarizerNode, buildNode),
      connections = connections
    )
    
    // Cache the workflow and return it
    workflowCache(workflow.id) = workflow
    workflow

  /** Execute a workflow with a given input
    *
    * @param workflowId
    *   ID of the workflow to execute
    * @param input
    *   Input to the workflow
    * @return
    *   The result of the workflow execution
    */
  def executeWorkflow[I, O](workflowId: String, input: I)(using I: ClassTag[I], O: ClassTag[O]): ZIO[Any, Throwable, O] =
    for
      workflow <- ZIO.fromOption(workflowCache.get(workflowId))
        .mapError(_ => new RuntimeException(s"Workflow not found: $workflowId"))
      
      // Find entry nodes
      entryNodes <- ZIO.succeed(workflow.findEntryNodes())
      _ <- ZIO.unless(entryNodes.nonEmpty)(
        ZIO.fail(new RuntimeException("Workflow has no entry nodes"))
      )
      entryNode = entryNodes.head
      
      // Execute the workflow by traversing the nodes
      executionId = UUID.randomUUID().toString
      result <- executeNode(workflow, entryNode.id, input, executionId)
      typedResult <- ZIO.fromEither(
        try Right(result.asInstanceOf[O])
        catch case e: ClassCastException =>
          Left(new IllegalArgumentException(s"Expected output of type ${O.runtimeClass.getName} but got ${result.getClass.getName}"))
      )
    yield typedResult

  /** Execute a single node and follow its connections
    *
    * @param workflow
    *   The workflow being executed
    * @param nodeId
    *   ID of the node to execute
    * @param input
    *   Input to the node
    * @param executionId
    *   ID of the current execution
    * @return
    *   The result of executing the node and its downstream nodes
    */
  private def executeNode[I](
      workflow: Workflow,
      nodeId: String,
      input: I,
      executionId: String
  )(using ClassTag[I]): ZIO[Any, Throwable, Any] =
    for
      // Find the node
      node <- ZIO.fromOption(workflow.findNode(nodeId))
        .mapError(_ => new RuntimeException(s"Node not found: $nodeId"))
      
      // Process the input through the node's processInput method
      result <- node.processInput(input)
      
      // Cache the result
      _ <- ZIO.succeed(resultCache(s"$executionId:$nodeId") = result)
      
      // Find output nodes
      outputNodes = workflow.findOutputNodes(nodeId)
      
      // If there are no output nodes, return the result
      finalResult <-
        if outputNodes.isEmpty then
          ZIO.succeed(result)
        else
          // Otherwise, execute the next node
          executeNode(workflow, outputNodes.head.id, result, executionId)
    yield finalResult

  /** Get the result of a node execution
    *
    * @param executionId
    *   ID of the execution
    * @param nodeId
    *   ID of the node
    * @return
    *   The result of the node execution
    */
  def getNodeResult(executionId: String, nodeId: String): Option[Any] =
    resultCache.get(s"$executionId:$nodeId")