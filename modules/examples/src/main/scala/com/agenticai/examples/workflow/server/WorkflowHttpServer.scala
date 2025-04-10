package com.agenticai.examples.workflow.server

import com.agenticai.examples.workflow.agent.*
import com.agenticai.examples.workflow.engine.*
import com.agenticai.examples.workflow.model.*

import zio.*
import zio.json.*
import java.util.UUID
import scala.collection.mutable

/** HTTP Server implementation for Workflow examples
  *
  * This is a simplified implementation that demonstrates how to host workflow services. It's a
  * placeholder until the ZIO HTTP compatibility issues are addressed.
  */
object WorkflowHttpServer:

  /** In-memory workflow result storage
    */
  private val workflowResults = mutable.Map[String, Any]()

  /** In-memory workflow status storage
    */
  private val workflowStatus = mutable.Map[String, String]()

  /** Run the HTTP server
    *
    * @return
    *   ZIO program that runs the server
    */
  def run: ZIO[Any, Throwable, ExitCode] =
    // Create workflow components
    val transformer = TextTransformerAgent.make()
    val splitter = TextSplitterAgent.make()
    val summarizer = SummarizationAgent.make()
    val buildAgent = BuildAgent.make()
    val engine = new WorkflowEngine(transformer, splitter, summarizer, buildAgent)

    // Create a workflow for demonstration
    val workflow = engine.createTextProcessingWorkflow()

    // Message explaining the situation
    val infoMessage = ZIO.succeed {
      println("=== Agentic AI Framework - Workflow Examples Server ===")
      println(
        "This is a simplified version of a Workflow HTTP server due to ZIO HTTP compatibility issues."
      )
      println("\nIn a complete implementation, the server would expose these endpoints:")
      println("  - POST /api/workflow/execute    - Execute a new workflow")
      println("  - GET  /api/workflow/status/:id - Get workflow status")
      println("  - GET  /api/workflow/result/:id - Get workflow result")
      println("  - GET  /api/workflow/progress/:id - Get workflow progress")
      println("  - POST /api/workflow/cancel/:id - Cancel workflow")
    }

    // Execute a sample workflow to demonstrate functionality
    val runSampleWorkflow = for
      _ <- infoMessage
      _ <- Console.printLine("\n------ Running Sample Workflow ------")
      id = UUID.randomUUID().toString
      _ <- updateStatus(id, "STARTED")
      _ <- Console.printLine(s"Workflow started with ID: $id")
      _ <- Console.printLine("Input text: \"This is a sample text for our workflow example. It will be processed through multiple agents.\"")
      
      // Start workflow execution in the background
      fiber <- executeWorkflow(
        engine,
        workflow.id,
        "This is a sample text for our workflow example. It will be processed through multiple agents.",
        id
      ).fork
      
      // Wait for a bit to let the workflow make progress
      _ <- ZIO.sleep(2.seconds)
      _ <- Console.printLine("\nWorkflow is executing... Checking status:")
      _ <- Console.printLine(s"Status: ${workflowStatus.getOrElse(id, "UNKNOWN")}")
      
      // Wait for completion
      result <- fiber.join
      _ <- updateStatus(id, "COMPLETED")
      _ <- Console.printLine(s"\nWorkflow completed with result: $result")
      
      // Wait for user input to exit
      _ <- Console.printLine("\nPress Enter to exit...")
      _ <- ZIO.succeed(scala.io.StdIn.readLine())
    yield ()

    runSampleWorkflow.exitCode

  /** Execute a workflow with the given input
    *
    * @param engine
    *   The workflow engine
    * @param workflowId
    *   ID of the workflow to execute
    * @param input
    *   Input text
    * @param executionId
    *   ID for this execution
    * @return
    *   ZIO effect that completes when the workflow completes
    */
  private def executeWorkflow(
      engine: WorkflowEngine,
      workflowId: String,
      input: String,
      executionId: String
  ): ZIO[Any, Throwable, String] =
    for
      // Log progress
      _ <- ZIO.logInfo(s"Executing workflow $workflowId with input: $input")
      _ <- updateStatus(executionId, "PROCESSING")
      
      // Update progress at 25%
      _ <- ZIO.sleep(500.milliseconds)
      _ <- updateStatus(executionId, "PROCESSING:25")
      _ <- ZIO.logInfo("Workflow 25% complete")
      
      // Update progress at 50%
      _ <- ZIO.sleep(500.milliseconds)
      _ <- updateStatus(executionId, "PROCESSING:50")
      _ <- ZIO.logInfo("Workflow 50% complete")
      
      // Update progress at 75%
      _ <- ZIO.sleep(500.milliseconds)
      _ <- updateStatus(executionId, "PROCESSING:75")
      _ <- ZIO.logInfo("Workflow 75% complete")
      
      // Execute the workflow
      result <- engine.executeWorkflow[String, String](workflowId, input)
      
      // Store result
      _ <- ZIO.succeed(workflowResults(executionId) = result)
      _ <- updateStatus(executionId, "COMPLETED")
      _ <- ZIO.logInfo(s"Workflow completed with result: $result")
    yield result

  /** Update the status of a workflow execution
    *
    * @param executionId
    *   ID of the execution
    * @param status
    *   New status
    * @return
    *   ZIO effect
    */
  private def updateStatus(executionId: String, status: String): UIO[Unit] =
    ZIO.succeed(workflowStatus(executionId) = status)