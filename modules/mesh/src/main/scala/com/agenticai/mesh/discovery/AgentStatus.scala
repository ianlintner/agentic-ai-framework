package com.agenticai.mesh.discovery

/**
 * Represents the current status of an agent in the mesh.
 */
sealed trait AgentStatus

object AgentStatus {
  /**
   * The agent is actively available and can process requests.
   */
  case object Active extends AgentStatus
  
  /**
   * The agent is temporarily unavailable but may become available again.
   */
  case object Unavailable extends AgentStatus
  
  /**
   * The agent is overloaded and should be used only for critical operations.
   */
  case object Overloaded extends AgentStatus
  
  /**
   * The agent is initializing and not yet ready to process requests.
   */
  case object Initializing extends AgentStatus
  
  /**
   * The agent is shutting down and will soon be unavailable.
   */
  case object ShuttingDown extends AgentStatus
  
  /**
   * The agent has been permanently removed from the mesh.
   */
  case object Deregistered extends AgentStatus
}