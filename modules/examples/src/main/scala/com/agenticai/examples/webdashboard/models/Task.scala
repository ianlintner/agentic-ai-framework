package com.agenticai.examples.webdashboard.models

import java.time.Instant
import java.util.UUID

/**
 * Core Task model representing a task in the agent system.
 * This extends the basic TaskRequest model with additional fields for
 * tracking the task's progress and lifecycle.
 *
 * Following functional programming principles, all methods return a new
 * Task instance rather than modifying the existing one.
 */
case class Task(
  id: String = UUID.randomUUID().toString,
  title: String,
  description: String,
  status: String = "Pending",
  priority: String = "Medium",
  progress: Double = 0.0, // 0.0 to 1.0
  subtasks: List[Subtask] = List.empty,
  results: List[SubtaskResult] = List.empty,
  assignedAgent: Option[String] = None,
  tags: Set[String] = Set.empty,
  metadata: Map[String, String] = Map.empty,
  createdAt: Instant = Instant.now,
  startedAt: Option[Instant] = None,
  completedAt: Option[Instant] = None,
  lastUpdatedAt: Instant = Instant.now,
  deadline: Option[Instant] = None
) {
  /**
   * Calculate and return a new Task with updated progress based on subtask completion.
   * This is a pure function that does not modify the original Task.
   *
   * @return A new Task instance with updated progress and timestamp
   */
  def withUpdatedProgress: Task = {
    val newProgress = if (subtasks.isEmpty) {
      if (status == "Completed") 1.0 else 0.0
    } else {
      val completedCount = subtasks.count(_.status == "Completed")
      completedCount.toDouble / subtasks.size
    }
    
    this.copy(
      progress = newProgress,
      lastUpdatedAt = Instant.now()
    )
  }
  
  /**
   * Return a new Task marked as started.
   *
   * @return A new Task instance with updated status and timestamps
   */
  def start: Task = {
    this.copy(
      status = "InProgress",
      startedAt = Some(Instant.now()),
      lastUpdatedAt = Instant.now()
    )
  }
  
  /**
   * Return a new Task marked as completed with the given results.
   *
   * @param results The results to add to the completed task
   * @return A new Task instance marked as completed
   */
  def complete(results: List[SubtaskResult]): Task = {
    this.copy(
      status = "Completed",
      results = results,
      progress = 1.0,
      completedAt = Some(Instant.now()),
      lastUpdatedAt = Instant.now()
    )
  }
  
  /**
   * Return a new Task with the given subtask added.
   *
   * @param subtask The subtask to add
   * @return A new Task instance with the subtask added and progress updated
   */
  def withSubtask(subtask: Subtask): Task = {
    this.copy(
      subtasks = subtasks :+ subtask,
      lastUpdatedAt = Instant.now()
    ).withUpdatedProgress
  }
  
  /**
   * Return a new Task with multiple subtasks added.
   *
   * @param newSubtasks The subtasks to add
   * @return A new Task instance with the subtasks added and progress updated
   */
  def withSubtasks(newSubtasks: List[Subtask]): Task = {
    this.copy(
      subtasks = subtasks ++ newSubtasks,
      lastUpdatedAt = Instant.now()
    ).withUpdatedProgress
  }
  
  /**
   * Return a new Task with updated status for the specified subtask.
   *
   * @param subtaskId The ID of the subtask to update
   * @param newStatus The new status for the subtask
   * @return A new Task instance with the updated subtask
   */
  def withUpdatedSubtaskStatus(subtaskId: String, newStatus: String): Task = {
    val updatedSubtasks = subtasks.map { s =>
      if (s.id == subtaskId) s.copy(status = newStatus) else s
    }
    
    this.copy(
      subtasks = updatedSubtasks,
      lastUpdatedAt = Instant.now()
    ).withUpdatedProgress
  }
  
  /**
   * Calculate the estimated time remaining for this task.
   *
   * @return An Option containing the estimated remaining time in milliseconds, or None if not applicable
   */
  def estimatedTimeRemaining: Option[Long] = {
    (startedAt, deadline) match {
      case (Some(start), Some(end)) if progress > 0 =>
        val totalDuration = end.toEpochMilli - start.toEpochMilli
        val elapsedTime = Instant.now().toEpochMilli - start.toEpochMilli
        val remainingTime = totalDuration - (elapsedTime / progress).toLong
        Some(remainingTime)
      case _ => None
    }
  }
  
  /**
   * Check if the task is overdue.
   *
   * @return True if the task has a deadline and is past it without being completed
   */
  def isOverdue: Boolean = {
    deadline.exists(d => Instant.now().isAfter(d) && status != "Completed")
  }
  
  /**
   * Return a new Task with the given tag added.
   *
   * @param tag The tag to add
   * @return A new Task instance with the tag added
   */
  def withTag(tag: String): Task = {
    this.copy(
      tags = tags + tag,
      lastUpdatedAt = Instant.now()
    )
  }
  
  /**
   * Return a new Task with the given metadata added or updated.
   *
   * @param key The metadata key
   * @param value The metadata value
   * @return A new Task instance with the updated metadata
   */
  def withMetadata(key: String, value: String): Task = {
    this.copy(
      metadata = metadata + (key -> value),
      lastUpdatedAt = Instant.now()
    )
  }
  
  /**
   * Return a new Task with the given agent assignment.
   *
   * @param agentId The ID of the agent to assign
   * @return A new Task instance with the agent assigned
   */
  def assignTo(agentId: String): Task = {
    this.copy(
      assignedAgent = Some(agentId),
      lastUpdatedAt = Instant.now()
    )
  }
  
  /**
   * Return a new Task with the given deadline.
   *
   * @param newDeadline The deadline to set
   * @return A new Task instance with the deadline set
   */
  def withDeadline(newDeadline: Instant): Task = {
    this.copy(
      deadline = Some(newDeadline),
      lastUpdatedAt = Instant.now()
    )
  }
  
  /**
   * Create a Task from a TaskRequest.
   *
   * @param request The TaskRequest to convert
   * @return A new Task instance
   */
  def fromRequest(request: TaskRequest): Task = {
    this.copy(
      id = request.id,
      title = request.title,
      description = request.description,
      priority = request.priority,
      deadline = request.deadline,
      tags = request.tags,
      metadata = request.metadata,
      createdAt = request.createdAt
    )
  }
  
  /**
   * Create a Task for high-priority work.
   *
   * @param title The task title
   * @param description The task description
   * @param deadline Optional deadline
   * @return A new high-priority Task instance
   */
  def highPriority(title: String, description: String, deadline: Option[Instant] = None): Task = {
    this.copy(
      title = title,
      description = description,
      priority = "High",
      deadline = deadline,
      tags = Set("high-priority")
    )
  }
}

/**
 * Companion object with factory methods for creating Task instances.
 */
object Task {
  /**
   * Create a simple Task with minimal information.
   *
   * @param title The task title
   * @param description The task description
   * @return A new Task instance
   */
  def simple(title: String, description: String): Task = {
    Task(
      title = title,
      description = description
    )
  }
  
  /**
   * Create a Task with pre-defined subtasks.
   *
   * @param title The task title
   * @param description The task description
   * @param subtasks The subtasks to include
   * @return A new Task instance with the specified subtasks
   */
  def withSubtasks(title: String, description: String, subtasks: List[Subtask]): Task = {
    Task(
      title = title,
      description = description,
      subtasks = subtasks
    ).withUpdatedProgress
  }
}