package com.agenticai.telemetry.agent

import com.agenticai.telemetry.core.{TelemetryAspect, TelemetryProvider}
import io.opentelemetry.api.trace.SpanKind
import zio.*
import scala.concurrent.duration.*

/**
 * Provides ZIO aspects for instrumenting agent operations with telemetry.
 * Builds on core telemetry aspects to add agent-specific instrumentation.
 */
object AgentTelemetryAspect {

  /**
   * Instruments an agent operation with telemetry, including execution time and error tracking.
   *
   * @param agentId The ID of the agent being instrumented
   * @param operationType The type of operation being performed
   * @return An aspect that can be applied to any agent operation
   */
  def instrumentAgent(
    agentId: String,
    operationType: String
  ): ZIOAspect[Nothing, AgentTelemetry & TelemetryProvider, Nothing, Any, Nothing, Any] =
    new ZIOAspect[Nothing, AgentTelemetry & TelemetryProvider, Nothing, Any, Nothing, Any] {
      override def apply[R <: AgentTelemetry & TelemetryProvider, E, A](
        zio: ZIO[R, E, A]
      )(implicit trace: Trace): ZIO[R, E, A] = {
        for {
          startTime <- Clock.currentTime(TimeUnit.MILLISECONDS)
          
          // Add tracing
          result <- TelemetryAspect.traced(
            s"agent.$operationType",
            Map(
              "agent_id" -> agentId,
              "operation" -> operationType
            )
          )(zio)
          
          // Record metrics
          endTime <- Clock.currentTime(TimeUnit.MILLISECONDS)
          duration = endTime - startTime
          _ <- AgentTelemetry.recordAgentExecution(agentId, operationType, duration)
        } yield result
      }.catchAll { error =>
        AgentTelemetry.recordError(agentId, error.getClass.getSimpleName) *>
        ZIO.fail(error)
      }
    }

  /**
   * Instruments an LLM operation with telemetry, tracking token usage and costs.
   *
   * @param agentId The ID of the agent making the LLM call
   * @return An aspect that can be applied to LLM operations
   */
  def instrumentLLM(
    agentId: String
  ): ZIOAspect[Nothing, AgentTelemetry & TelemetryProvider, Nothing, Any, Nothing, Any] =
    new ZIOAspect[Nothing, AgentTelemetry & TelemetryProvider, Nothing, Any, Nothing, Any] {
      override def apply[R <: AgentTelemetry & TelemetryProvider, E, A](
        zio: ZIO[R, E, A]
      )(implicit trace: Trace): ZIO[R, E, A] = {
        TelemetryAspect.traced(
          "llm.completion",
          Map("agent_id" -> agentId)
        )(zio).tap { result =>
          // Extract token counts and cost from result
          // This is a simplified example - actual implementation would need to
          // extract these from the LLM response type
          val (promptTokens, completionTokens, cost) = result match {
            case r: LLMResponse => (r.promptTokens, r.completionTokens, r.cost)
            case _ => (0L, 0L, 0.0) // Default if not an LLM response
          }
          AgentTelemetry.recordLLMTokens(agentId, promptTokens, completionTokens, cost)
        }
      }
    }

  /**
   * Instruments a workflow step with telemetry.
   *
   * @param workflowId The ID of the workflow
   * @param stepId The ID of the workflow step
   * @return An aspect that can be applied to workflow step operations
   */
  def instrumentWorkflowStep(
    workflowId: String,
    stepId: String
  ): ZIOAspect[Nothing, AgentTelemetry & TelemetryProvider, Nothing, Any, Nothing, Any] =
    new ZIOAspect[Nothing, AgentTelemetry & TelemetryProvider, Nothing, Any, Nothing, Any] {
      override def apply[R <: AgentTelemetry & TelemetryProvider, E, A](
        zio: ZIO[R, E, A]
      )(implicit trace: Trace): ZIO[R, E, A] = {
        for {
          _ <- AgentTelemetry.recordWorkflowStep(workflowId, stepId, "started")
          result <- TelemetryAspect.traced(
            "workflow.step",
            Map(
              "workflow_id" -> workflowId,
              "step_id" -> stepId
            )
          )(zio)
          _ <- AgentTelemetry.recordWorkflowStep(workflowId, stepId, "completed")
        } yield result
      }.catchAll { error =>
        AgentTelemetry.recordWorkflowStep(workflowId, stepId, "failed") *>
        ZIO.fail(error)
      }
    }
}

// Placeholder for LLM response type - this would be defined elsewhere
case class LLMResponse(
  promptTokens: Long,
  completionTokens: Long,
  cost: Double
)