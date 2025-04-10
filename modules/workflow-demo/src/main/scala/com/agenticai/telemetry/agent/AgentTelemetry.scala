package com.agenticai.telemetry.agent

import com.agenticai.telemetry.core.TelemetryProvider
import zio._

/**
 * Agent-specific telemetry instrumentation for agent operations.
 * This is a simplified stand-in implementation.
 */
trait AgentTelemetry {
  def recordAgentExecution(agentId: String, operationType: String, durationMs: Long): UIO[Unit]
  def recordError(agentId: String, errorType: String): UIO[Unit]
  def recordWorkflowStep(workflowId: String, stepId: String, status: String): UIO[Unit]
}

object AgentTelemetry {
  val live: ZLayer[TelemetryProvider, Nothing, AgentTelemetry] = ZLayer.succeed {
    new AgentTelemetry {
      override def recordAgentExecution(
        agentId: String, 
        operationType: String,
        durationMs: Long
      ): UIO[Unit] = 
        ZIO.logDebug(s"AGENT_METRIC: $agentId.$operationType duration=$durationMs ms")
        
      override def recordError(
        agentId: String,
        errorType: String
      ): UIO[Unit] = 
        ZIO.logDebug(s"AGENT_ERROR: $agentId error=$errorType")
        
      override def recordWorkflowStep(
        workflowId: String,
        stepId: String,
        status: String
      ): UIO[Unit] = 
        ZIO.logDebug(s"WORKFLOW_STEP: $workflowId.$stepId status=$status")
    }
  }
  
  // Accessor methods
  def recordAgentExecution(agentId: String, operationType: String, durationMs: Long): URIO[AgentTelemetry, Unit] =
    ZIO.serviceWithZIO(_.recordAgentExecution(agentId, operationType, durationMs))
    
  def recordError(agentId: String, errorType: String): URIO[AgentTelemetry, Unit] =
    ZIO.serviceWithZIO(_.recordError(agentId, errorType))
    
  def recordWorkflowStep(workflowId: String, stepId: String, status: String): URIO[AgentTelemetry, Unit] =
    ZIO.serviceWithZIO(_.recordWorkflowStep(workflowId, stepId, status))
}