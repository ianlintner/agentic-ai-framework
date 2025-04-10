package com.agenticai.core.task

import zio.*
import com.agenticai.core.agent.Agent

/** Task represents a discrete unit of work that can be composed functionally.
  *
  * @tparam I
  *   Input type
  * @tparam O
  *   Output type
  */
trait Task[-I, +O]:
  /** Execute the task with the given input.
    *
    * @param input
    *   The input value
    * @return
    *   ZIO effect containing the output or error
    */
  def execute(input: I): ZIO[Any, Throwable, O]

  /** Functorial map operation.
    *
    * @param f
    *   Function to apply to the output
    * @return
    *   A new Task with mapped output
    */
  def map[O2](f: O => O2): Task[I, O2] =
    Task(input => execute(input).map(f))

  /** Monadic flatMap operation.
    *
    * @param f
    *   Function from output to another Task
    * @return
    *   A new Task composing the operations
    */
  def flatMap[I2 <: I, O2](f: O => Task[I2, O2]): Task[I2, O2] =
    Task(input => execute(input).flatMap(o => f(o).execute(input)))

  /** Parallel composition with another task.
    *
    * @param that
    *   Task to compose with
    * @return
    *   A new Task that processes both in parallel
    */
  def zip[I2 <: I, O2](that: Task[I2, O2]): Task[I2, (O, O2)] =
    Task(input => execute(input).zip(that.execute(input)))

  /** Kleisli composition (andThen).
    *
    * @param that
    *   Task to compose with
    * @return
    *   A new Task representing the composition
    */
  def andThen[O2](that: Task[O, O2]): Task[I, O2] =
    Task(input => execute(input).flatMap(that.execute))

  /** Convert to an Agent.
    *
    * @return
    *   An Agent representation of this task
    */
  def asAgent: Agent[I, O] = Agent(execute)

object Task:

  /** Create a Task from a function.
    *
    * @param f
    *   Function from input to ZIO effect
    * @return
    *   A new Task
    */
  def apply[I, O](f: I => ZIO[Any, Throwable, O]): Task[I, O] =
    new DefaultTask(f)

  /** Create a Task that succeeds with a constant value.
    *
    * @param value
    *   The constant value to return
    * @return
    *   A new Task that ignores its input
    */
  def succeed[I, O](value: O): Task[I, O] =
    apply(_ => ZIO.succeed(value))

  /** Create a Task that fails with the given error.
    *
    * @param error
    *   The error to fail with
    * @return
    *   A new Task that always fails
    */
  def fail[I, O](error: Throwable): Task[I, O] =
    apply(_ => ZIO.fail(error))

  /** Sequence a list of tasks, executing them in order.
    *
    * @param tasks
    *   The list of tasks to sequence
    * @return
    *   A new Task that returns a list of results
    */
  def sequence[I, O](tasks: List[Task[I, O]]): Task[I, List[O]] =
    Task(input => ZIO.foreach(tasks)(_.execute(input)))

  /** Execute tasks in parallel.
    *
    * @param tasks
    *   The list of tasks to execute in parallel
    * @return
    *   A new Task that returns a list of results
    */
  def parallel[I, O](tasks: List[Task[I, O]]): Task[I, List[O]] =
    Task(input => ZIO.foreachPar(tasks)(_.execute(input)))

  /** Create a task from an agent.
    *
    * @param agent
    *   The agent to convert
    * @return
    *   A new Task wrapping the agent
    */
  def fromAgent[I, O](agent: Agent[I, O]): Task[I, O] =
    Task(agent.process)

  /** Combine a list of tasks with a combinator function.
    *
    * @param tasks
    *   The list of tasks to combine
    * @param f
    *   The function to combine results
    * @return
    *   A new Task applying the combinator function to the results
    */
  def combine[I, O, R](tasks: List[Task[I, O]])(f: List[O] => R): Task[I, R] =
    sequence(tasks).map(f)

/** Default implementation of Task.
  *
  * @param f
  *   Function from input to ZIO effect
  * @tparam I
  *   Input type
  * @tparam O
  *   Output type
  */
private final class DefaultTask[-I, +O](f: I => ZIO[Any, Throwable, O]) extends Task[I, O]:
  def execute(input: I): ZIO[Any, Throwable, O] = f(input)
