package com.agenticai.workflow.engine

import com.agenticai.workflow.model._
import com.agenticai.workflow.agent._
import zio._

/**
 * Engine that executes workflows by coordinating agents
 */
class WorkflowEngine(
  textTransformer: TextTransformerAgent,
  textSplitter: TextSplitterAgent,
  summarizer: SummarizationAgent,
  buildAgent: BuildAgent
) {
  /**
   * Execute a workflow with the specified input
   *
   * @param workflow The workflow to execute
   * @param input The input text to process
   * @return The output after processing through all workflow nodes
   */
  def executeWorkflow(workflow: Workflow, input: String): ZIO[Any, Throwable, String] = {
    // Build execution plan from the workflow
    val executionPlan = buildExecutionPlan(workflow)
    
    // Process input through the execution plan
    runExecutionPlan(executionPlan, input)
  }
  
  /**
   * Build an execution plan from a workflow definition
   * This converts the declarative workflow into an executable sequence of operations
   */
  private def buildExecutionPlan(workflow: Workflow): List[WorkflowStep] = {
    // Simplistic implementation: just determine an ordering based on connections
    
    // Build a directed graph (adjacency list) from the connections
    val graph = workflow.connections.groupBy(_.sourceNodeId).map { case (src, conns) =>
      src -> conns.map(_.targetNodeId)
    }
    
    // Find nodes with no incoming connections (start nodes)
    val incomingConnections = workflow.connections.groupBy(_.targetNodeId)
    val startNodeIds = workflow.nodes.map(_.id).filter(id => 
      !incomingConnections.keySet.contains(id)
    )
    
    // Topological sort
    val visited = scala.collection.mutable.Set[String]()
    val ordering = scala.collection.mutable.ListBuffer[String]()
    
    def visit(nodeId: String): Unit = {
      if (!visited.contains(nodeId)) {
        visited.add(nodeId)
        
        // Visit all neighbors
        graph.get(nodeId).foreach { neighbors =>
          neighbors.foreach(visit)
        }
        
        // Add to result
        ordering.prepend(nodeId)
      }
    }
    
    // Visit all start nodes
    startNodeIds.foreach(visit)
    
    // Add any remaining nodes (in case of cycles or disconnected nodes)
    workflow.nodes.foreach { node =>
      if (!visited.contains(node.id)) {
        ordering.prepend(node.id)
      }
    }
    
    // Convert node IDs to workflow steps
    ordering.toList.map { nodeId =>
      val node = workflow.nodes.find(_.id == nodeId).get
      nodeToStep(node)
    }
  }
  
  /**
   * Convert a workflow node to an executable step
   */
  private def nodeToStep(node: WorkflowNode): WorkflowStep = {
    node.nodeType match {
      case "text-transformer" =>
        val transform = node.configuration.getOrElse("transform", "capitalize")
        TextTransformStep(textTransformer, transform)
        
      case "text-splitter" =>
        val delimiter = node.configuration.getOrElse("delimiter", "\\n")
        TextSplitStep(textSplitter, delimiter)
        
      case "summarizer" =>
        SummarizeStep(summarizer)
        
      case "build" =>
        BuildStep(buildAgent)
        
      case _ =>
        // Default to passthrough for unknown node types
        PassthroughStep()
    }
  }
  
  /**
   * Run an execution plan on an input
   */
  private def runExecutionPlan(steps: List[WorkflowStep], input: String): ZIO[Any, Throwable, String] = {
    steps.foldLeft(ZIO.succeed(input)) { (resultZIO, step) =>
      resultZIO.flatMap(intermediateResult =>
        step.execute(intermediateResult).catchAll(error =>
          ZIO.logError(s"Step execution failed: ${error.getMessage}") *>
          ZIO.succeed(s"Error processing text: ${error.getMessage}")
        )
      )
    }
  }
}

object WorkflowEngine {
  /**
   * Create a layer that provides a WorkflowEngine
   */
  val live: ZLayer[TextTransformerAgent & TextSplitterAgent & SummarizationAgent & BuildAgent, Nothing, WorkflowEngine] =
    ZLayer.fromFunction(
      (transformer: TextTransformerAgent, splitter: TextSplitterAgent, summarizer: SummarizationAgent, buildAgent: BuildAgent) =>
        new WorkflowEngine(transformer, splitter, summarizer, buildAgent)
    )
}

/**
 * A step in a workflow execution plan
 */
sealed trait WorkflowStep {
  def execute(input: String): ZIO[Any, Throwable, String]
}

/**
 * Text transformation step
 */
case class TextTransformStep(agent: TextTransformerAgent, transform: String) extends WorkflowStep {
  def execute(input: String): ZIO[Any, Throwable, String] = agent.process(input)
}

/**
 * Text splitting step
 */
case class TextSplitStep(agent: TextSplitterAgent, delimiter: String) extends WorkflowStep {
  def execute(input: String): ZIO[Any, Throwable, String] = agent.process(input)
}

/**
 * Summarization step
 */
case class SummarizeStep(agent: SummarizationAgent) extends WorkflowStep {
  def execute(input: String): ZIO[Any, Throwable, String] = agent.process(input)
}

/**
 * Passthrough step that doesn't modify the input
 */
case class PassthroughStep() extends WorkflowStep {
  def execute(input: String): ZIO[Any, Throwable, String] = ZIO.succeed(input)
}

/**
 * Build step
 */
case class BuildStep(agent: BuildAgent) extends WorkflowStep {
  def execute(input: String): ZIO[Any, Throwable, String] = agent.process(input)
}