package com.agenticai.examples.webdashboard.api

import com.agenticai.core.llm._
import com.agenticai.core.memory._
import com.agenticai.core.memory.MemoryError
import com.agenticai.examples.webdashboard.agents.TaskProcessorAgent
import com.agenticai.examples.webdashboard.models._
import zio._
import zio.json._
import zio.stream._

import java.time.Instant
import java.util.UUID

/**
 * JSON encoders and decoders for our model classes
 */
object JsonCodecs {
  // TaskRequest codec
  implicit val taskRequestEncoder: JsonEncoder[TaskRequest] = DeriveJsonEncoder.gen[TaskRequest]
  implicit val taskRequestDecoder: JsonDecoder[TaskRequest] = DeriveJsonDecoder.gen[TaskRequest]
  
  // Subtask codec
  implicit val subtaskEncoder: JsonEncoder[Subtask] = DeriveJsonEncoder.gen[Subtask]
  implicit val subtaskDecoder: JsonDecoder[Subtask] = DeriveJsonDecoder.gen[Subtask]
  
  // SubtaskResult codec
  implicit val subtaskResultEncoder: JsonEncoder[SubtaskResult] = DeriveJsonEncoder.gen[SubtaskResult]
  implicit val subtaskResultDecoder: JsonDecoder[SubtaskResult] = DeriveJsonDecoder.gen[SubtaskResult]
  
  // TaskResponse codec
  implicit val taskResponseEncoder: JsonEncoder[TaskResponse] = DeriveJsonEncoder.gen[TaskResponse]
  implicit val taskResponseDecoder: JsonDecoder[TaskResponse] = DeriveJsonDecoder.gen[TaskResponse]
  
  // Simple data structure for task list
  case class TaskSummary(
    id: String,
    title: String,
    status: String,
    createdAt: Long
  )
  
  implicit val taskSummaryEncoder: JsonEncoder[TaskSummary] = DeriveJsonEncoder.gen[TaskSummary]
}

/**
 * Mock HTTP API for interacting with our agent framework 
 * (simplified version for the example without HTTP server)
 */
object AgentAPI extends ZIOAppDefault {
  import JsonCodecs._
  
  // Create a TaskProcessorAgent instance
  val taskAgent = new TaskProcessorAgent()
  
  // In-memory task storage for demo
  private val tasks = scala.collection.mutable.Map.empty[String, TaskResponse]
  
  /**
   * Process a task request
   */
  def processTaskRequest(jsonRequest: String): ZIO[Any, Throwable, String] = {
    for {
      _ <- ZIO.log(s"Received task request: $jsonRequest")
      taskRequest <- ZIO.fromEither(jsonRequest.fromJson[TaskRequest])
        .mapError(error => new RuntimeException(s"Invalid JSON: $error"))
      _ <- ZIO.log(s"Processing task: ${taskRequest.title}")
      response <- taskAgent.process(taskRequest)
        .runHead
        .someOrFail(new RuntimeException("No response generated"))
        .provideLayer(ZLayer.fromZIO(MemorySystem.make))
        .mapError(err => new RuntimeException(s"Memory error: ${err.getMessage}"))
      _ <- ZIO.succeed(tasks.put(response.requestId, response))  // Store in our mock database
      jsonResponse <- ZIO.succeed(response.toJson)
      _ <- ZIO.log(s"Task processed successfully: ${response.requestId}")
    } yield jsonResponse
  }
  
  /**
   * Get a task by ID
   */
  def getTaskStatus(taskId: String): ZIO[Any, Throwable, String] = {
    for {
      _ <- ZIO.log(s"Retrieving status for task: $taskId")
      response <- ZIO.fromOption(tasks.get(taskId))
        .orElseSucceed(
          TaskResponse(
            requestId = taskId,
            status = "Completed",
            results = List.empty,
            summary = "Example task (mock data)",
            completedAt = Instant.now
          )
        )
      jsonResponse <- ZIO.succeed(response.toJson)
      _ <- ZIO.log(s"Returning task status for: $taskId")
    } yield jsonResponse
  }
  
  /**
   * Get list of all tasks
   */
  def getTaskList: ZIO[Any, Nothing, String] = {
    for {
      _ <- ZIO.log("Retrieving task list")
      // Create summaries from our tasks map
      taskSummaries = tasks.values.map(response => 
        TaskSummary(
          id = response.requestId,
          title = "Task " + response.requestId.substring(0, 8),
          status = response.status,
          createdAt = response.completedAt.toEpochMilli
        )
      ).toList
      
      // Add a mock task if empty
      summaries = if (taskSummaries.isEmpty) {
        List(
          TaskSummary(
            id = "example-task-1",
            title = "Example Task",
            status = "Completed",
            createdAt = Instant.now().minusSeconds(300).toEpochMilli
          )
        )
      } else {
        taskSummaries
      }
      
      jsonResponse <- ZIO.succeed(summaries.toJson)
      _ <- ZIO.log(s"Returning ${summaries.size} tasks")
    } yield jsonResponse
  }
  
  /**
   * Simulate a websocket connection with updates
   */
  def startWebSocketUpdates: ZIO[Any, Nothing, Unit] = {
    ZStream
      .fromSchedule(Schedule.spaced(2.seconds))
      .foreach { count =>
        val message = s"""{"type":"agentUpdate","message":"Agent heartbeat $count"}"""
        ZIO.log(s"WebSocket message: $message")
      }
  }
  
  def run = {
    val exampleTask = TaskRequest(
      title = "Example Task",
      description = """Investigate the current state of quantum computing and its potential applications.
        |Focus on recent developments in quantum supremacy and practical applications.""".stripMargin,
      priority = "Medium",
      deadline = Some(Instant.now().plusSeconds(3600)),
      tags = Set("research", "quantum", "technology"),
      metadata = Map("category" -> "research", "complexity" -> "high")
    )
    
    for {
      _ <- ZIO.log("Starting Agent API demo...")
      _ <- processTaskRequest(exampleTask.toJson)
      _ <- getTaskList
      _ <- startWebSocketUpdates
    } yield ()
  }
}