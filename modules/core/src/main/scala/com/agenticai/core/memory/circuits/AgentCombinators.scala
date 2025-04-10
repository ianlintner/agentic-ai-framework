package com.agenticai.core.memory.circuits

/** Monadic combinators for agent composition inspired by Factorio's circuit networks.
  *
  * These combinators allow for functional composition of agents, enabling complex processing
  * pipelines and feedback mechanisms similar to Factorio's combinators.
  */
object AgentCombinators:

  /** Basic agent trait representing a processing unit that transforms inputs to outputs
    *
    * @tparam I
    *   Input type
    * @tparam O
    *   Output type
    */
  trait Agent[I, O]:
    /** Process an input and produce an output
      *
      * @param input
      *   The input to process
      * @return
      *   The processed output
      */
    def process(input: I): O

  /** Create an agent from a function
    *
    * @param f
    *   The function to convert to an agent
    * @return
    *   An agent that applies the function to its input
    */
  def Agent[I, O](f: I => O): Agent[I, O] = new Agent[I, O]:
    def process(input: I): O = f(input)

  /** Transform an agent's output using a function (analogous to Arithmetic Combinator)
    *
    * @param agent
    *   The agent to transform
    * @param f
    *   The transformation function
    * @return
    *   A new agent that transforms the output of the original agent
    */
  def transform[I, A, B](agent: Agent[I, A])(f: A => B): Agent[I, B] = new Agent[I, B]:
    def process(input: I): B = f(agent.process(input))

  /** Filter an agent's output based on a predicate (analogous to Decider Combinator)
    *
    * @param agent
    *   The agent whose output will be filtered
    * @param predicate
    *   The condition to determine if the output should pass through
    * @return
    *   A new agent that conditionally passes the output
    */
  def filter[I, O](agent: Agent[I, O])(predicate: O => Boolean): Agent[I, Option[O]] =
    new Agent[I, Option[O]]:

      def process(input: I): Option[O] =
        val result = agent.process(input)
        if predicate(result) then Some(result) else None

  /** Connect agents in sequence (pipeline pattern)
    *
    * @param first
    *   The first agent in the pipeline
    * @param second
    *   The second agent in the pipeline
    * @return
    *   A new agent that processes input through both agents in sequence
    */
  def pipeline[I, A, B](first: Agent[I, A], second: Agent[A, B]): Agent[I, B] = new Agent[I, B]:
    def process(input: I): B = second.process(first.process(input))

  /** Process input through two agents in parallel and combine their results
    *
    * @param agentA
    *   The first agent
    * @param agentB
    *   The second agent
    * @param combine
    *   The function to combine results from both agents
    * @return
    *   A new agent that processes through both paths and combines the results
    */
  def parallel[I, A, B, C](agentA: Agent[I, A], agentB: Agent[I, B])(
      combine: (A, B) => C
  ): Agent[I, C] = new Agent[I, C]:
    def process(input: I): C = combine(agentA.process(input), agentB.process(input))

  /** Apply multiple transformations in sequence (shift register pattern)
    *
    * @param stages
    *   The list of transformation stages
    * @param initial
    *   The initial agent that processes the input
    * @return
    *   A new agent that applies all transformations in sequence
    */
  def shiftRegister[I, O](stages: List[Agent[O, O]])(initial: Agent[I, O]): Agent[I, O] =
    new Agent[I, O]:

      def process(input: I): O =
        var result = initial.process(input)
        stages.foreach { stage =>
          result = stage.process(result)
        }
        result

  /** Apply an agent repeatedly to its own output (feedback loop pattern)
    *
    * @param agent
    *   The agent to apply repeatedly
    * @param iterations
    *   The number of times to apply the agent
    * @return
    *   A new agent that applies the original agent multiple times
    */
  def feedback[O](agent: Agent[O, O])(iterations: Int): Agent[O, O] = new Agent[O, O]:

    def process(input: O): O =
      var result = input
      for _ <- 0 until iterations do result = agent.process(result)
      result

  /** A memory cell that maintains state between operations, inspired by Factorio's memory cells
    * used in circuit networks.
    *
    * @param initialValue
    *   The initial value
    * @tparam T
    *   The type of value stored in the cell
    */
  class MemoryCell[T](initialValue: T):
    private var value: T = initialValue

    /** Get the current value
      * @return
      *   The current value stored in the cell
      */
    def get: T = value

    /** Set a new value
      * @param newValue
      *   The value to store in the cell
      */
    def set(newValue: T): Unit = value = newValue

    /** Update the value using a transformation function
      * @param f
      *   The transformation function to apply to the current value
      */
    def update(f: T => T): Unit = value = f(value)

    /** Create an agent interface to the memory cell This allows the memory cell to be used as an
      * agent in a circuit
      * @return
      *   An agent that applies a function to the memory cell's value and returns the result
      */
    def asAgent: Agent[T => T, T] = new Agent[T => T, T]:

      def process(input: T => T): T =
        val newValue = input(value)
        set(newValue)
        newValue
