package com.agenticai.examples.webdashboard.agents

import com.agenticai.core.BaseAgent
import com.agenticai.core.memory._
import com.agenticai.examples.webdashboard.models._
import zio._
import zio.stream._
import java.time.Instant
import java.util.UUID
import com.agenticai.core.llm._

/**
 * An agent that processes task requests by breaking them down into subtasks
 * and coordinating their execution
 */
class TaskProcessorAgent extends BaseAgent[TaskRequest, TaskResponse]("TaskProcessorAgent") {
  // Helper to safely unwrap memory system
  private def withMemory[R, A](effect: ZIO[MemorySystem, MemoryError, A]): ZIO[Any, Throwable, A] = {
    effect
      .provideLayer(ZLayer.fromZIO(MemorySystem.make))
      .mapError(err => new RuntimeException(s"Memory error: ${err.getMessage}"))
  }

  /**
   * Process a task request and return a response
   */
  override protected def processMessage(request: TaskRequest): ZStream[Any, Throwable, TaskResponse] = {
    ZStream.fromZIO(
      for {
        // Store the request in memory for tracking
        _ <- withMemory(for {
          system <- ZIO.service[MemorySystem]
          cell <- system.createCellWithTags(request, Set("request", request.id))
        } yield ())
        
        // Log the processing start
        _ <- ZIO.logInfo(s"Processing task: ${request.title} (${request.id})")
        
        // Decompose the task into subtasks
        subtasks <- decomposeTask(request)
        
        // Process each subtask
        subtaskResults <- ZIO.foreach(subtasks)(processSubtask)
        
        // Create the final response
        response = createResponse(request, subtaskResults)
        
        // Store the response in memory
        _ <- withMemory(for {
          system <- ZIO.service[MemorySystem]
          cell <- system.createCellWithTags(response, Set("response", request.id))
        } yield ())
        
        // Log completion
        _ <- ZIO.logInfo(s"Completed task: ${request.title} (${request.id})")
      } yield response
    )
  }
  
  /**
   * Break down a task into component subtasks
   */
  private def decomposeTask(request: TaskRequest): ZIO[Any, Nothing, List[Subtask]] = {
    // Split on both single and double newlines, and look for numbered items
    val subtaskDescriptions = request.description
      .split("\n")
      .map(_.trim)
      .filter(_.nonEmpty)
      .foldLeft(List.empty[String]) { (acc, line) =>
        if (line.matches("\\d+\\..*")) {
          // Start a new subtask for numbered items
          line :: acc
        } else if (acc.isEmpty) {
          // Start with the first line
          line :: acc
        } else {
          // Append to the current subtask unless it's a clear section break
          if (line.endsWith(":") || line.length < acc.head.length / 2) {
            line :: acc
          } else {
            (acc.head + " " + line) :: acc.tail
          }
        }
      }
      .reverse
    
    // If no clear divisions, create at least one subtask
    val descriptions = if (subtaskDescriptions.isEmpty) {
      List(request.description)
    } else {
      subtaskDescriptions
    }
    
    // Create subtasks
    ZIO.succeed(
      descriptions.zipWithIndex.map { case (description, index) =>
        Subtask(
          id = UUID.randomUUID().toString,
          parentTaskId = request.id,
          title = s"${request.title} - Part ${index + 1}",
          description = description,
          status = "Pending",
          priority = request.priority
        )
      }
    )
  }
  
  /**
   * Process an individual subtask
   */
  private def processSubtask(subtask: Subtask): ZIO[Any, Nothing, SubtaskResult] = {
    // Simulate processing time based on complexity (length of description)
    val processingTime = math.max(100, subtask.description.length * 10).millis
    
    for {
      _ <- ZIO.logInfo(s"Processing subtask: ${subtask.title} (${subtask.id})")
      
      // Simulate processing work
      _ <- ZIO.sleep(processingTime)
      
      // Generate a result
      result = SubtaskResult(
        subtaskId = subtask.id,
        status = "Completed",
        output = s"Processed: ${subtask.description.take(50)}...",
        completedAt = Instant.now()
      )
      
      _ <- ZIO.logInfo(s"Completed subtask: ${subtask.title} (${subtask.id})")
    } yield result
  }
  
  /**
   * Create a final response from the processed subtasks
   */
  private def createResponse(request: TaskRequest, results: List[SubtaskResult]): TaskResponse = {
    // Determine overall status
    val status = if (results.exists(_.status == "Failed")) {
      "Failed"
    } else if (results.forall(_.status == "Completed")) {
      "Completed"
    } else {
      "InProgress"
    }
    
    // Generate a summary
    val summary = s"Task '${request.title}' processed with ${results.size} subtasks. " +
      s"${results.count(_.status == "Completed")} completed, " +
      s"${results.count(_.status == "Failed")} failed."
    
    TaskResponse(
      requestId = request.id,
      status = status,
      results = results,
      summary = summary,
      completedAt = Instant.now()
    )
  }
}

/**
 * Companion object with example runner
 */
object TaskProcessorAgent extends ZIOAppDefault {
  def run = {
    val agent = new TaskProcessorAgent()
    
    for {
      _ <- ZIO.logInfo("Starting TaskProcessorAgent example...")
      
      // Create an example task
      task = TaskRequest(
        title = "Research quantum computing applications",
        description = 
          """Investigate the current state of quantum computing and its potential applications.
            |
            |Analyze the major quantum computing platforms currently available.
            |
            |Evaluate potential use cases for quantum computing in machine learning.
            |
            |Identify challenges and limitations of current quantum computing technologies.""".stripMargin,
        priority = "High",
        tags = Set("research", "quantum", "computing")
      )
      
      // Process the task
      result <- agent.process(task)
        .runHead
        .someOrFail(new RuntimeException("No result produced"))
        .provideLayer(ZLayer.fromZIO(MemorySystem.make))
        .mapError(err => new RuntimeException(s"Memory error: ${err.getMessage}"))
      
      // Display the result
      _ <- ZIO.logInfo(s"Task completed with status: ${result.status}")
      _ <- ZIO.logInfo(s"Summary: ${result.summary}")
      _ <- ZIO.logInfo("Subtask results:")
      _ <- ZIO.foreach(result.results) { subtaskResult =>
        ZIO.logInfo(s"  - ${subtaskResult.subtaskId}: ${subtaskResult.output}")
      }
    } yield ()
  }
}