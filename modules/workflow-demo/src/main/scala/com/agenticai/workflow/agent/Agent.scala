package com.agenticai.workflow.agent

import zio.*
import com.agenticai.telemetry.SimpleTelemetry

/** Agent trait for workflow-demo module that processes inputs to outputs
  *
  * @tparam I Input type
  * @tparam O Output type
  */
trait Agent[-I, +O]:
  /** Process an input value to produce an output
    *
    * @param input The input value
    * @return ZIO effect containing the output or error
    */
  def process(input: I): ZIO[Any, Throwable, O]
  
  /** Process with telemetry instrumentation
    *
    * @param agentId The agent identifier for telemetry recording
    * @param input The input value to process
    * @return ZIO effect with telemetry instrumentation
    */
  def processWithTelemetry(agentId: String, input: I): ZIO[Any, Throwable, O] = 
    SimpleTelemetry.traceEffect(s"agent.$agentId.process", Map("agent_id" -> agentId)) {
      process(input)
    }

object Agent:
  /** Create an Agent from a function that returns a ZIO effect
    *
    * @param f Function from input to ZIO effect
    * @return A new Agent
    */
  def apply[I, O](f: I => ZIO[Any, Throwable, O]): Agent[I, O] =
    new Agent[I, O]:
      def process(input: I): ZIO[Any, Throwable, O] = f(input)

  /** Create an Agent that returns a constant value
    *
    * @param value The constant value to return
    * @return A new Agent that ignores its input
    */
  def succeed[I, O](value: O): Agent[I, O] =
    apply(_ => ZIO.succeed(value))
