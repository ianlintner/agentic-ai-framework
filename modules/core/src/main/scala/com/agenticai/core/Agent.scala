package com.agenticai.core

import zio._
import zio.stream._
import com.agenticai.core.memory.MemorySystem

/**
 * Represents the core capabilities of an AI agent
 */
trait Agent[Message, Action] {
  /**
   * Process an incoming input and return a stream of outputs
   */
  def process(input: Message): ZStream[MemorySystem, Throwable, Action]

  /**
   * Process a stream of inputs and return a stream of outputs
   */
  def processStream(inputs: ZStream[Any, Throwable, Message]): ZStream[MemorySystem, Throwable, Action]

  /**
   * The agent's name
   */
  def name: String

  /**
   * The agent's current state
   */
  def state: UIO[AgentState]

  /**
   * Update the agent's status and metadata
   */
  def updateStatus(status: AgentStatus, metadata: Map[String, String] = Map.empty): UIO[Unit]
}

/**
 * Represents the state of an agent
 */
case class AgentState(
  id: String,
  status: AgentStatus,
  metadata: Map[String, String] = Map.empty
)

/**
 * Represents the possible states an agent can be in
 */
sealed trait AgentStatus
object AgentStatus {
  case object Idle extends AgentStatus
  case object Processing extends AgentStatus
  case object Error extends AgentStatus
  case object Terminated extends AgentStatus
} 