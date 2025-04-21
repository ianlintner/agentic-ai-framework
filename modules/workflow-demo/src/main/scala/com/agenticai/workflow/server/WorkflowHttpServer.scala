package com.agenticai.workflow.server

import com.agenticai.workflow.agent.*
import com.agenticai.workflow.engine.*
import com.agenticai.workflow.model.*

import zio.*
import zio.http.*
import zio.json.*
import java.util.UUID
import scala.collection.mutable
import java.util.concurrent.{Executors, TimeUnit}

/** HTTP Server implementation for Workflow Demo
  */
object WorkflowHttpServer:

  // In-memory storage for workflow executions
  private val workflowExecutions = mutable.Map[String, WorkflowExecution]()

  // Case classes for API requests and responses
  case class ExecuteWorkflowRequest(input: String, workflow: Option[Workflow] = None)
  object ExecuteWorkflowRequest:
    implicit val decoder: JsonDecoder[ExecuteWorkflowRequest] = DeriveJsonDecoder.gen[ExecuteWorkflowRequest]

  case class WorkflowResponse(id: String)
  object WorkflowResponse:
    implicit val encoder: JsonEncoder[WorkflowResponse] = DeriveJsonEncoder.gen[WorkflowResponse]
    implicit val decoder: JsonDecoder[WorkflowResponse] = DeriveJsonDecoder.gen[WorkflowResponse]

  case class StatusResponse(id: String, status: String, progress: Int)
  object StatusResponse:
    implicit val encoder: JsonEncoder[StatusResponse] = DeriveJsonEncoder.gen[StatusResponse]

  case class ResultResponse(id: String, result: String)
  object ResultResponse:
    implicit val encoder: JsonEncoder[ResultResponse] = DeriveJsonEncoder.gen[ResultResponse]

  // Represents a workflow execution
  case class WorkflowExecution(
      id: String,
      input: String,
      workflow: Workflow,
      status: Ref[String],
      progress: Ref[Int],
      result: Promise[Throwable, String],
      cancelFlag: Ref[Boolean]
  )

  // Default example workflow
  private val defaultWorkflow = Workflow(
    id = "default-workflow",
    name = "Enhanced Text Processing Workflow",
    description = "A workflow that transforms, splits, summarizes, and analyzes sentiment of text",
    nodes = List(
      WorkflowNode(
        id = "node-1",
        nodeType = "text-transformer",
        label = "Text Transformer",
        configuration = Map("transform" -> "capitalize"),
        position = NodePosition(100, 100)
      ),
      WorkflowNode(
        id = "node-2",
        nodeType = "text-splitter",
        label = "Text Splitter",
        configuration = Map("delimiter" -> "\\n"),
        position = NodePosition(300, 100)
      ),
      WorkflowNode(
        id = "node-3",
        nodeType = "summarizer",
        label = "Summarizer",
        configuration = Map(),
        position = NodePosition(500, 100)
      ),
      WorkflowNode(
        id = "node-4",
        nodeType = "sentiment-analysis",
        label = "Sentiment Analyzer",
        configuration = Map("mode" -> "detailed"),
        position = NodePosition(700, 100)
      )
    ),
    connections = List(
      NodeConnection(
        id = "conn-1",
        sourceNodeId = "node-1",
        targetNodeId = "node-2"
      ),
      NodeConnection(
        id = "conn-2",
        sourceNodeId = "node-2",
        targetNodeId = "node-3"
      ),
      NodeConnection(
        id = "conn-3",
        sourceNodeId = "node-3",
        targetNodeId = "node-4"
      )
    )
  )

  /** Run the server
    */
  def run: ZIO[Any, Throwable, ExitCode] =
    // Create workflow components
    val transformer = TextTransformerAgent.make()
    val splitter    = TextSplitterAgent.make()
    val summarizer  = SummarizationAgent.make()
    val buildAgent  = BuildAgent.make()
    val sentimentAnalyzer = SentimentAnalysisAgent.make()
    val engine      = new WorkflowEngine(transformer, splitter, summarizer, buildAgent, sentimentAnalyzer)

    // Define routes
    val app = Routes(
      // Root route for server status check
      Method.GET / "" -> handler { (req: Request) =>
        Response.text("Workflow Demo Server is running")
      },

      // Execute a workflow
      Method.POST / "api" / "workflow" / "execute" -> handler { (req: Request) =>
        for {
          bodyString <- req.body.asString
          request <- ZIO.fromEither(bodyString.fromJson[ExecuteWorkflowRequest])
            .catchAll(error => ZIO.fail(new RuntimeException(s"Invalid request format: $error")))

          // Use provided workflow or default
          workflow = request.workflow.getOrElse(defaultWorkflow)

          // Create execution tracking objects
          id = UUID.randomUUID().toString
          status <- Ref.make("running")
          progress <- Ref.make(0)
          result <- Promise.make[Throwable, String]
          cancelFlag <- Ref.make(false)

          // Store execution
          _ <- ZIO.succeed {
            workflowExecutions.put(
              id,
              WorkflowExecution(id, request.input, workflow, status, progress, result, cancelFlag)
            )
          }

          // Execute workflow in background
          _ <- executeWorkflowInBackground(engine, id, request.input, workflow, status, progress, result, cancelFlag)
            .fork

          // Return response
          response = WorkflowResponse(id)
          jsonResponse = response.toJson
        } yield Response.json(jsonResponse)
      },

      // Get workflow status
      Method.GET / "api" / "workflow" / "status" / string("id") -> handler { (id: String, req: Request) =>
        ZIO.succeed {
          workflowExecutions.get(id) match {
            case Some(execution) => 
              execution.status.get.flatMap { status =>
                execution.progress.get.map { progress =>
                  val response = StatusResponse(id, status, progress)
                  Response.json(response.toJson)
                }
              }
            case None => 
              ZIO.succeed(Response.status(Status.NotFound))
          }
        }.flatten
      },

      // Get workflow result
      Method.GET / "api" / "workflow" / "result" / string("id") -> handler { (id: String, req: Request) =>
        ZIO.succeed {
          workflowExecutions.get(id) match {
            case Some(execution) => 
              execution.status.get.flatMap { status =>
                if (status == "completed") {
                  execution.result.poll.flatMap {
                    case Some(Exit.Success(result)) => 
                      ZIO.succeed(Response.json(ResultResponse(id, result).toJson))
                    case Some(Exit.Failure(_)) => 
                      ZIO.succeed(Response.status(Status.InternalServerError))
                    case _ => 
                      ZIO.succeed(Response.status(Status.Processing))
                  }
                } else {
                  ZIO.succeed(Response.status(Status.Processing))
                }
              }
            case None => 
              ZIO.succeed(Response.status(Status.NotFound))
          }
        }.flatten
      },

      // Get workflow progress
      Method.GET / "api" / "workflow" / "progress" / string("id") -> handler { (id: String, req: Request) =>
        ZIO.succeed {
          workflowExecutions.get(id) match {
            case Some(execution) => 
              execution.progress.get.map { progress =>
                val response = StatusResponse(id, "running", progress)
                Response.json(response.toJson)
              }
            case None => 
              ZIO.succeed(Response.status(Status.NotFound))
          }
        }.flatten
      },

      // Cancel workflow
      Method.POST / "api" / "workflow" / "cancel" / string("id") -> handler { (id: String, req: Request) =>
        ZIO.succeed {
          workflowExecutions.get(id) match {
            case Some(execution) => 
              execution.cancelFlag.set(true) *>
              execution.status.set("cancelled") *>
              ZIO.succeed(Response.status(Status.Ok))
            case None => 
              ZIO.succeed(Response.status(Status.NotFound))
          }
        }.flatten
      }
    )

    // Create a simple HttpApp that returns a success response
    val httpApp = Routes(
      Method.GET / "" -> handler { (_: Request) =>
        Response.text("Workflow Demo Server is running")
      }
    ).toHttpApp

    // Start the server
    val port = 8083
    val startupMessage = ZIO.succeed {
      println("=== Agentic AI Workflow Demo Server ===")
      println(s"Server started on http://localhost:$port")
      println("\nAPI Endpoints:")
      println("  - POST /api/workflow/execute    - Execute a new workflow")
      println("  - GET  /api/workflow/status/:id - Get workflow status")
      println("  - GET  /api/workflow/result/:id - Get workflow result")
      println("  - GET  /api/workflow/progress/:id - Get workflow progress")
      println("  - POST /api/workflow/cancel/:id - Cancel workflow")
      println("\nAccess the Workflow Demo UI directly in your browser at:")
      println("file://" + java.lang.System.getProperty("user.dir") + "/modules/workflow-demo/src/main/resources/public/local-test.html")
    }

    for {
      _ <- startupMessage
      _ <- Server.serve(httpApp).provide(Server.defaultWithPort(port))
    } yield ExitCode.success

  /** Execute a workflow in the background
    */
  private def executeWorkflowInBackground(
      engine: WorkflowEngine,
      id: String,
      input: String,
      workflow: Workflow,
      status: Ref[String],
      progress: Ref[Int],
      result: Promise[Throwable, String],
      cancelFlag: Ref[Boolean]
  ): ZIO[Any, Nothing, Unit] =
    // Simulate progress updates
    val updateProgress = for {
      isCancelled <- cancelFlag.get
      _ <- ZIO.when(!isCancelled) {
        for {
          currentProgress <- progress.get
          newProgress = Math.min(currentProgress + 10, 90) // Cap at 90% until complete
          _ <- progress.set(newProgress)
          _ <- ZIO.sleep(1.second)
        } yield ()
      }
    } yield ()

    // Execute the workflow with progress updates
    val execution = for {
      // Simulate some initial delay and progress
      _ <- ZIO.sleep(500.milliseconds)
      _ <- progress.set(10)
      _ <- ZIO.sleep(500.milliseconds)

      // Check for cancellation
      isCancelled <- cancelFlag.get
      _ <- ZIO.when(isCancelled) {
        ZIO.fail(new RuntimeException("Workflow execution was cancelled"))
      }

      // Start progress updates in background
      progressFiber <- updateProgress.repeat(Schedule.spaced(1.second)).fork

      // Execute the workflow
      output <- engine.executeWorkflow(workflow, input)
        .catchAll(error => ZIO.succeed(s"Error: ${error.getMessage}"))

      // Complete the execution
      _ <- progressFiber.interrupt
      _ <- progress.set(100)
      _ <- status.set("completed")
      _ <- result.succeed(output)
    } yield ()

    // Handle errors and ensure status is updated
    execution.catchAll { error =>
      for {
        _ <- status.set("failed")
        _ <- result.fail(error)
      } yield ()
    }.ensuring {
      for {
        isCancelled <- cancelFlag.get
        _ <- if (isCancelled) status.set("cancelled") else ZIO.unit
      } yield ()
    }
