package com.agenticai.core

import zio._
import zio.stream._
import java.util.UUID
import com.agenticai.core.memory.MemorySystem

/**
 * Base implementation of the Agent trait that provides common functionality
 */
abstract class BaseAgent[I, O](agentName: String) extends Agent[I, O] {
  private val agentId = UUID.randomUUID().toString
  protected val stateRef: UIO[Ref[AgentState]] = Ref.make(
    AgentState(
      id = agentName,
      status = AgentStatus.Idle,
      metadata = Map.empty
    )
  )

  override def state: UIO[AgentState] = stateRef.flatMap(_.get)

  override def name: String = agentName

  /**
   * Protected method to update the agent's state
   */
  override def updateStatus(newStatus: AgentStatus, metadata: Map[String, String] = Map.empty): UIO[Unit] =
    stateRef.flatMap(_.update(state => state.copy(status = newStatus, metadata = metadata)))

  /**
   * Process a message and handle state transitions
   */
  override def process(input: I): ZStream[MemorySystem, Throwable, O] = {
    ZStream.fromZIO(
      updateStatus(AgentStatus.Processing)
    ) *> processMessage(input).ensuring(
      updateStatus(AgentStatus.Idle)
    )
  }

  /**
   * Abstract method to be implemented by concrete agents
   */
  protected def processMessage(input: I): ZStream[MemorySystem, Throwable, O]

  override def processStream(inputs: ZStream[Any, Throwable, I]): ZStream[MemorySystem, Throwable, O] =
    inputs.flatMap(process)
} 