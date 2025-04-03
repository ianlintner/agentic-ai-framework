package com.agenticai.examples.webdashboard.models

import java.time.Instant
import java.util.UUID

/**
 * Core data models for the task processing system.
 * These models represent the request/response flow for task processing
 * and the intermediate data structures used during processing.
 */
case class TaskRequest(
  id: String = UUID.randomUUID().toString,
  title: String,
  description: String,
  priority: String = "Medium",
  deadline: Option[Instant] = None,
  tags: Set[String] = Set.empty,
  metadata: Map[String, String] = Map.empty,
  createdAt: Instant = Instant.now
)

/**
 * Represents a subtask that is part of a larger task
 */
case class Subtask(
  id: String = UUID.randomUUID().toString,
  parentTaskId: String,
  title: String,
  description: String,
  status: String = "Pending",
  priority: String = "Medium",
  createdAt: Instant = Instant.now
)

/**
 * Represents the result of a processed subtask
 */
case class SubtaskResult(
  subtaskId: String,
  status: String = "Completed",
  output: String,
  completedAt: Instant = Instant.now
)

/**
 * Represents the final response for a task request
 */
case class TaskResponse(
  requestId: String,
  status: String = "Completed",
  results: List[SubtaskResult] = List.empty,
  summary: String,
  completedAt: Instant = Instant.now
)

case class TaskResult(
  id: String,
  title: String,
  status: String,
  priority: String,
  progress: Double,
  subtasks: List[Subtask],
  results: List[SubtaskResult],
  createdAt: Instant,
  completedAt: Option[Instant] = None
)