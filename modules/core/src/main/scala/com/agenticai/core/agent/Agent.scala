package com.agenticai.core.agent

import zio.*

/** Core Agent trait representing a function-like entity that processes inputs to outputs within the
  * ZIO effect system.
  *
  * @tparam I
  *   Input type
  * @tparam O
  *   Output type
  */
trait Agent[-I, +O]:
  /** Process an input value to produce an output within a ZIO effect.
    *
    * @param input
    *   The input value
    * @return
    *   ZIO effect containing the output or error
    */
  def process(input: I): ZIO[Any, Throwable, O]

  /** Functorial map operation.
    *
    * @param f
    *   Function to apply to the output
    * @return
    *   A new Agent with mapped output
    */
  def map[O2](f: O => O2): Agent[I, O2] =
    Agent(input => process(input).map(f))

  /** Monadic flatMap operation.
    *
    * @param f
    *   Function from output to another Agent
    * @return
    *   A new Agent composing the operations
    */
  def flatMap[I2 <: I, O2](f: O => Agent[I2, O2]): Agent[I2, O2] =
    Agent(input => process(input).flatMap(o => f(o).process(input)))

  /** Kleisli composition (andThen).
    *
    * @param that
    *   Agent to compose with
    * @return
    *   A new Agent representing the composition
    */
  def andThen[O2](that: Agent[O, O2]): Agent[I, O2] =
    Agent(input => process(input).flatMap(that.process))

  /** Parallel composition with another Agent.
    *
    * @param that
    *   Agent to compose with
    * @return
    *   A new Agent that processes both in parallel
    */
  def zip[I2 <: I, O2](that: Agent[I2, O2]): Agent[I2, (O, O2)] =
    Agent(input => process(input).zip(that.process(input)))

  /** Parallel composition with discard of right result.
    */
  def zipLeft[I2 <: I, O2](that: Agent[I2, O2]): Agent[I2, O] =
    zip(that).map(_._1)

  /** Parallel composition with discard of left result.
    */
  def zipRight[I2 <: I, O2](that: Agent[I2, O2]): Agent[I2, O2] =
    zip(that).map(_._2)

object Agent:

  /** Create an Agent from a function that returns a ZIO effect.
    *
    * @param f
    *   Function from input to ZIO effect
    * @return
    *   A new Agent
    */
  def apply[I, O](f: I => ZIO[Any, Throwable, O]): Agent[I, O] =
    new DefaultAgent(f)

  /** Create an Agent that returns a constant value.
    *
    * @param value
    *   The constant value to return
    * @return
    *   A new Agent that ignores its input
    */
  def succeed[I, O](value: O): Agent[I, O] =
    apply(_ => ZIO.succeed(value))

  /** Create an Agent that fails with the given error.
    *
    * @param error
    *   The error to fail with
    * @return
    *   A new Agent that always fails
    */
  def fail[I, O](error: Throwable): Agent[I, O] =
    apply(_ => ZIO.fail(error))

  /** Create an Agent from a pure function.
    *
    * @param f
    *   Pure function from input to output
    * @return
    *   A new Agent
    */
  def fromFunction[I, O](f: I => O): Agent[I, O] =
    apply(i => ZIO.succeed(f(i)))

  /** Sequence a list of agents, executing them in order.
    *
    * @param agents
    *   The list of agents to sequence
    * @return
    *   A new Agent that returns a list of results
    */
  def sequence[I, O](agents: List[Agent[I, O]]): Agent[I, List[O]] =
    Agent(input => ZIO.foreach(agents)(_.process(input)))

  /** Execute agents in parallel.
    *
    * @param agents
    *   The list of agents to execute in parallel
    * @return
    *   A new Agent that returns a list of results
    */
  def parallel[I, O](agents: List[Agent[I, O]]): Agent[I, List[O]] =
    Agent(input => ZIO.foreachPar(agents)(_.process(input)))

/** Default implementation of the Agent trait.
  *
  * @param f
  *   Function from input to ZIO effect
  * @tparam I
  *   Input type
  * @tparam O
  *   Output type
  */
private final class DefaultAgent[-I, +O](f: I => ZIO[Any, Throwable, O]) extends Agent[I, O]:
  def process(input: I): ZIO[Any, Throwable, O] = f(input)
