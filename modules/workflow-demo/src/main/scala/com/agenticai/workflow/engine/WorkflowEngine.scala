package com.agenticai.workflow.engine

import com.agenticai.workflow.model.*
import com.agenticai.workflow.agent.*
import com.agenticai.telemetry.SimpleTelemetry
import zio.*
import java.util.concurrent.TimeUnit

/** Engine that executes workflows by coordinating agents
  */
class WorkflowEngine(
    textTransformer: TextTransformerAgent,
    textSplitter: TextSplitterAgent,
    summarizer: SummarizationAgent,
    buildAgent: BuildAgent
):

  /** Execute a workflow with the specified input
    *
    * @param workflow
    *   The workflow to execute
    * @param input
    *   The input text to process
    * @return
    *   The output after processing through all workflow nodes
    */
  def executeWorkflow(workflow: Workflow, input: String): ZIO[Any, Throwable, String] = {
    val workflowTags = Map(
      "workflow_id" -> workflow.id,
      "workflow_name" -> workflow.name,
      "node_count" -> workflow.nodes.size.toString
    )
    
    for {
      // Start workflow execution telemetry
      startTime <- Clock.currentTime(TimeUnit.MILLISECONDS)
      _ <- SimpleTelemetry.recordStart("workflow.execute", workflowTags)
      
      // Build execution plan from the workflow
      executionPlan <- ZIO.succeed(buildExecutionPlan(workflow))
      _ <- SimpleTelemetry.recordMetric("workflow.plan.steps", executionPlan.size.toDouble, workflowTags)
      
      // Process input through the execution plan
      result <- runExecutionPlan(executionPlan, input)
        .tapError { error =>
          SimpleTelemetry.recordError(
            "workflow.execute",
            error.getClass.getSimpleName,
            error.getMessage,
            workflowTags
          )
        }
      
      // End workflow execution telemetry
      endTime <- Clock.currentTime(TimeUnit.MILLISECONDS)
      duration = endTime - startTime
      _ <- SimpleTelemetry.recordEnd("workflow.execute", duration, workflowTags)
      _ <- SimpleTelemetry.recordMetric("workflow.execution.time", duration, workflowTags)
    } yield result
  }

  /** Build an execution plan from a workflow definition This converts the declarative workflow into
    * an executable sequence of operations
    */
  private def buildExecutionPlan(workflow: Workflow): List[WorkflowStep] =
    // Simplistic implementation: just determine an ordering based on connections

    // Build a directed graph (adjacency list) from the connections
    val graph = workflow.connections.groupBy(_.sourceNodeId).map { case (src, conns) =>
      src -> conns.map(_.targetNodeId)
    }

    // Find nodes with no incoming connections (start nodes)
    val incomingConnections = workflow.connections.groupBy(_.targetNodeId)
    val startNodeIds =
      workflow.nodes.map(_.id).filter(id => !incomingConnections.keySet.contains(id))

    // Topological sort
    val visited  = scala.collection.mutable.Set[String]()
    val ordering = scala.collection.mutable.ListBuffer[String]()

    def visit(nodeId: String): Unit =
      if !visited.contains(nodeId) then
        visited.add(nodeId)

        // Visit all neighbors
        graph.get(nodeId).foreach { neighbors =>
          neighbors.foreach(visit)
        }

        // Add to result
        ordering.prepend(nodeId)

    // Visit all start nodes
    startNodeIds.foreach(visit)

    // Add any remaining nodes (in case of cycles or disconnected nodes)
    workflow.nodes.foreach { node =>
      if !visited.contains(node.id) then ordering.prepend(node.id)
    }

    // Convert node IDs to workflow steps
    ordering.toList.map { nodeId =>
      val node = workflow.nodes.find(_.id == nodeId).get
      nodeToStep(node)
    }

  /** Convert a workflow node to an executable step
    */
  private def nodeToStep(node: WorkflowNode): WorkflowStep =
    node.nodeType match
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

  /** Run an execution plan on an input
    */
  /** Run an execution plan on an input with telemetry
    */
  private def runExecutionPlan(
      steps: List[WorkflowStep],
      input: String
  ): ZIO[Any, Throwable, String] = {
    val planTags = Map("step_count" -> steps.size.toString)
    
    for {
      // Start execution plan telemetry
      startTime <- Clock.currentTime(TimeUnit.MILLISECONDS)
      _ <- SimpleTelemetry.recordStart("workflow.plan.execute", planTags)
      
      // Execute each step
      result <- steps.zipWithIndex.foldLeft(ZIO.succeed(input)) { case (resultZIO, (step, index)) =>
        resultZIO.flatMap(intermediateResult => {
          val stepTags = Map(
            "step_index" -> index.toString,
            "step_type" -> step.getClass.getSimpleName
          )
          
          for {
            _ <- SimpleTelemetry.recordStart(s"workflow.step.execute", stepTags)
            stepStartTime <- Clock.currentTime(TimeUnit.MILLISECONDS)
            
            stepResult <- step
              .execute(intermediateResult)
              .catchAll(error => {
                for {
                  _ <- ZIO.logError(s"Step execution failed: ${error.getMessage}")
                  _ <- SimpleTelemetry.recordError(
                    "workflow.step.execute",
                    error.getClass.getSimpleName,
                    error.getMessage,
                    stepTags
                  )
                } yield s"Error processing text: ${error.getMessage}"
              })
              
            stepEndTime <- Clock.currentTime(TimeUnit.MILLISECONDS)
            stepDuration = stepEndTime - stepStartTime
            _ <- SimpleTelemetry.recordEnd(s"workflow.step.execute", stepDuration, stepTags)
            _ <- SimpleTelemetry.recordMetric("workflow.step.duration", stepDuration, stepTags)
          } yield stepResult
        })
      }
      
      // End execution plan telemetry
      endTime <- Clock.currentTime(TimeUnit.MILLISECONDS)
      duration = endTime - startTime
      _ <- SimpleTelemetry.recordEnd("workflow.plan.execute", duration, planTags)
      _ <- SimpleTelemetry.recordMetric("workflow.plan.duration", duration, planTags)
    } yield result
  }

object WorkflowEngine:

  /** Create a layer that provides a WorkflowEngine
    */
  val live: ZLayer[
    TextTransformerAgent & TextSplitterAgent & SummarizationAgent & BuildAgent,
    Nothing,
    WorkflowEngine
  ] =
    ZLayer.fromFunction(
      (
          transformer: TextTransformerAgent,
          splitter: TextSplitterAgent,
          summarizer: SummarizationAgent,
          buildAgent: BuildAgent
      ) => new WorkflowEngine(transformer, splitter, summarizer, buildAgent)
    )

/** A step in a workflow execution plan
  */
sealed trait WorkflowStep:
  def execute(input: String): ZIO[Any, Throwable, String]

/** Text transformation step
  */
case class TextTransformStep(agent: TextTransformerAgent, transform: String) extends WorkflowStep:
  def execute(input: String): ZIO[Any, Throwable, String] =
    agent.processWithTelemetry(s"transform-${transform}", input)

/** Text splitting step
  */
case class TextSplitStep(agent: TextSplitterAgent, delimiter: String) extends WorkflowStep:
  def execute(input: String): ZIO[Any, Throwable, String] =
    agent.processWithTelemetry(s"split-${delimiter}", input)

/** Summarization step
  */
case class SummarizeStep(agent: SummarizationAgent) extends WorkflowStep:
  def execute(input: String): ZIO[Any, Throwable, String] =
    agent.processWithTelemetry("summarize", input)

/** Passthrough step that doesn't modify the input
  */
case class PassthroughStep() extends WorkflowStep:
  def execute(input: String): ZIO[Any, Throwable, String] =
    SimpleTelemetry.traceEffect("step.passthrough", Map.empty) {
      ZIO.succeed(input)
    }

/** Build step
  */
case class BuildStep(agent: BuildAgent) extends WorkflowStep:
  def execute(input: String): ZIO[Any, Throwable, String] =
    agent.processWithTelemetry("build", input)
