package com.agenticai.core.capability

import zio._
import com.agenticai.core.agent.Agent
import com.agenticai.core.task.Task

/**
 * Capability represents what an agent can do.
 *
 * @tparam I Input type
 * @tparam O Output type
 */
trait Capability[-I, +O] {
  /**
   * Apply the capability to the input.
   *
   * @param input The input value
   * @return ZIO effect containing the output or error
   */
  def apply(input: I): ZIO[Any, Throwable, O]
  
  /**
   * Functorial map operation.
   *
   * @param f Function to apply to the output
   * @return A new Capability with mapped output
   */
  def map[O2](f: O => O2): Capability[I, O2] =
    Capability(input => apply(input).map(f))
    
  /**
   * Monadic flatMap operation.
   *
   * @param f Function from output to another Capability
   * @return A new Capability composing the operations
   */
  def flatMap[I2 <: I, O2](f: O => Capability[I2, O2]): Capability[I2, O2] =
    Capability(input => apply(input).flatMap(o => f(o)(input)))
    
  /**
   * Compose with another capability.
   *
   * @param that Capability to compose with
   * @return A new Capability representing the composition
   */
  def andThen[O2](that: Capability[O, O2]): Capability[I, O2] =
    Capability(input => apply(input).flatMap(that.apply))
    
  /**
   * Parallel composition with another capability.
   *
   * @param that Capability to compose with
   * @return A new Capability that processes both in parallel
   */
  def zip[I2 <: I, O2](that: Capability[I2, O2]): Capability[I2, (O, O2)] =
    Capability(input => apply(input).zip(that(input)))
    
  /**
   * Convert to a task.
   *
   * @return A Task representation of this capability
   */
  def asTask: Task[I, O] = Task(apply)
  
  /**
   * Convert to an agent.
   *
   * @return An Agent representation of this capability
   */
  def asAgent: Agent[I, O] = Agent(apply)
  
  /**
   * Apply a condition to this capability.
   *
   * @param condition Condition to check on the input
   * @return A capability that returns an option based on the condition
   */
    
  /**
   * Chain capabilities conditionally.
   *
   * @param that Alternative capability
   * @return A capability that tries this first, then that if this fails
   */
  /**
   * Chain capabilities conditionally.
   */
  def orElse[I2 <: I, O2 >: O](that: Capability[I2, O2]): Capability[I2, O2] =
    new Capability[I2, O2] {
      def apply(input: I2): zio.ZIO[Any, Throwable, O2] =
        Capability.this.apply(input).orElse(that(input))
    }
}

object Capability {
  /**
   * Create a capability from a function.
   *
   * @param f Function from input to ZIO effect
   * @return A new Capability
   */
  def apply[I, O](f: I => ZIO[Any, Throwable, O]): Capability[I, O] =
    new DefaultCapability(f)
    
  /**
   * Create a capability that succeeds with a constant value.
   *
   * @param value The constant value to return
   * @return A new Capability that ignores its input
   */
  def succeed[I, O](value: O): Capability[I, O] =
    apply(_ => ZIO.succeed(value))
    
  /**
   * Create a capability that fails with the given error.
   *
   * @param error The error to fail with
   * @return A new Capability that always fails
   */
  def fail[I, O](error: Throwable): Capability[I, O] =
    apply(_ => ZIO.fail(error))
    
  /**
   * Combine multiple capabilities into one.
   *
   * @param capabilities The list of capabilities to combine
   * @return A new Capability that returns a list of results
   */
  def combine[I, O](capabilities: List[Capability[I, O]]): Capability[I, List[O]] =
    Capability(input => ZIO.foreach(capabilities)(_.apply(input)))
    
  /**
   * Execute capabilities in parallel.
   *
   * @param capabilities The list of capabilities to execute in parallel
   * @return A new Capability that returns a list of results
   */
  def parallel[I, O](capabilities: List[Capability[I, O]]): Capability[I, List[O]] =
    Capability(input => ZIO.foreachPar(capabilities)(_.apply(input)))
    
  /**
   * Create a capability from a predicate.
   *
   * @param predicate Predicate to check on the input
   * @param error Error message if predicate fails
   * @return A capability that verifies the predicate
   */
  def fromPredicate[I](predicate: I => Boolean, error: String): Capability[I, I] =
    Capability(input => 
      if (predicate(input)) ZIO.succeed(input)
      else ZIO.fail(new IllegalArgumentException(error))
    )
}

/**
 * Default implementation of Capability.
 *
 * @param f Function from input to ZIO effect
 * @tparam I Input type
 * @tparam O Output type
 */
private final class DefaultCapability[-I, +O](f: I => ZIO[Any, Throwable, O]) extends Capability[I, O] {
  def apply(input: I): ZIO[Any, Throwable, O] = f(input)
}