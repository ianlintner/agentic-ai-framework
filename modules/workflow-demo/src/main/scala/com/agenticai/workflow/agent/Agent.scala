package com.agenticai.workflow.agent

import zio._

/**
 * Simplified Agent trait for workflow-demo module
 * Represents a function-like entity that processes inputs to outputs
 *
 * @tparam I Input type
 * @tparam O Output type
 */
trait Agent[-I, +O] {
  /**
   * Process an input value to produce an output within a ZIO effect
   *
   * @param input The input value
   * @return ZIO effect containing the output or error
   */
  def process(input: I): ZIO[Any, Throwable, O]
}

object Agent {
  /**
   * Create an Agent from a function that returns a ZIO effect
   *
   * @param f Function from input to ZIO effect
   * @return A new Agent
   */
  def apply[I, O](f: I => ZIO[Any, Throwable, O]): Agent[I, O] =
    new Agent[I, O] {
      def process(input: I): ZIO[Any, Throwable, O] = f(input)
    }
    
  /**
   * Create an Agent that returns a constant value
   *
   * @param value The constant value to return
   * @return A new Agent that ignores its input
   */
  def succeed[I, O](value: O): Agent[I, O] =
    apply(_ => ZIO.succeed(value))
}