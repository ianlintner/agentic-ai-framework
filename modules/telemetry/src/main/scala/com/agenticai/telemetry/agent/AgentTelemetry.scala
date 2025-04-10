package com.agenticai.telemetry.agent

import com.agenticai.telemetry.core.{TelemetryAspect, TelemetryProvider}
import io.opentelemetry.api.metrics.{LongCounter, LongHistogram}
import io.opentelemetry.api.trace.SpanKind
import zio.*

/**
 * Agent-specific telemetry instrumentation that provides metrics and tracing
 * for agent operations, LLM interactions, and workflow execution.
 */
trait AgentTelemetry {
  def recordAgentExecution(agentId: String, operationType: String, durationMs: Long): UIO[Unit]
  def recordLLMTokens(agentId: String, promptTokens: Long, completionTokens: Long, cost: Double): UIO[Unit]
  def recordError(agentId: String, errorType: String): UIO[Unit]
  def recordWorkflowStep(workflowId: String, stepId: String, status: String): UIO[Unit]
}

object AgentTelemetry {
  def live: ZLayer[TelemetryProvider, Nothing, AgentTelemetry] = ZLayer.scoped {
    for {
      telemetry <- ZIO.service[TelemetryProvider]
      meter <- ZIO.succeed(telemetry.tracer("agentic-ai").map(_.meter))
      
      // Agent execution metrics
      executionTime <- ZIO.succeed(meter.histogramBuilder("agent.execution.duration")
        .setDescription("Duration of agent execution in milliseconds")
        .setUnit("ms")
        .build())
      
      throughput <- ZIO.succeed(meter.counterBuilder("agent.operations")
        .setDescription("Number of agent operations")
        .build())
      
      // LLM metrics  
      tokenUsage <- ZIO.succeed(meter.counterBuilder("llm.tokens")
        .setDescription("Number of tokens used in LLM operations")
        .build())
      
      llmCost <- ZIO.succeed(meter.counterBuilder("llm.cost")
        .setDescription("Cost of LLM operations in USD")
        .build())
      
      // Error metrics
      errors <- ZIO.succeed(meter.counterBuilder("agent.errors")
        .setDescription("Number of agent errors by type")
        .build())
      
      // Workflow metrics
      workflowSteps <- ZIO.succeed(meter.counterBuilder("workflow.steps")
        .setDescription("Number of workflow steps by status")
        .build())
        
    } yield new AgentTelemetry {
      override def recordAgentExecution(
        agentId: String, 
        operationType: String,
        durationMs: Long
      ): UIO[Unit] = ZIO.succeed {
        executionTime.record(
          durationMs,
          "agent_id", agentId,
          "operation", operationType
        )
        throughput.add(1, "agent_id", agentId)
      }

      override def recordLLMTokens(
        agentId: String,
        promptTokens: Long,
        completionTokens: Long,
        cost: Double
      ): UIO[Unit] = ZIO.succeed {
        tokenUsage.add(
          promptTokens + completionTokens,
          "agent_id", agentId,
          "type", "prompt",
          "tokens", promptTokens.toString
        )
        llmCost.add(
          cost.toLong,
          "agent_id", agentId,
          "cost_usd", cost.toString
        )
      }

      override def recordError(
        agentId: String,
        errorType: String
      ): UIO[Unit] = ZIO.succeed {
        errors.add(
          1,
          "agent_id", agentId,
          "error_type", errorType
        )
      }

      override def recordWorkflowStep(
        workflowId: String,
        stepId: String,
        status: String
      ): UIO[Unit] = ZIO.succeed {
        workflowSteps.add(
          1,
          "workflow_id", workflowId,
          "step_id", stepId,
          "status", status
        )
      }
    }
  }

  // Accessor methods
  def recordAgentExecution(agentId: String, operationType: String, durationMs: Long): URIO[AgentTelemetry, Unit] =
    ZIO.serviceWithZIO(_.recordAgentExecution(agentId, operationType, durationMs))

  def recordLLMTokens(agentId: String, promptTokens: Long, completionTokens: Long, cost: Double): URIO[AgentTelemetry, Unit] =
    ZIO.serviceWithZIO(_.recordLLMTokens(agentId, promptTokens, completionTokens, cost))

  def recordError(agentId: String, errorType: String): URIO[AgentTelemetry, Unit] =
    ZIO.serviceWithZIO(_.recordError(agentId, errorType))

  def recordWorkflowStep(workflowId: String, stepId: String, status: String): URIO[AgentTelemetry, Unit] =
    ZIO.serviceWithZIO(_.recordWorkflowStep(workflowId, stepId, status))
}