package com.agenticai.core.capability

import zio._
import scala.collection.immutable.Set

/**
 * A type-safe composable agent that can be chained with other agents.
 * ComposableAgent encapsulates the processing logic along with metadata
 * about capabilities, input/output types, and agent properties.
 *
 * @tparam I Input type
 * @tparam O Output type
 */
trait ComposableAgent[-I, +O] {
  /** Process an input value and produce an output */
  def process(input: I): Task[O]
  
  /** Agent's capabilities */
  def capabilities: Set[String]
  
  /** Input type name (for workflow composition) */
  def inputType: String
  
  /** Output type name (for workflow composition) */
  def outputType: String
  
  /** Custom properties for this agent */
  def properties: Map[String, String]
  
  /**
   * Chain this agent with another agent, creating a new agent that
   * applies the second agent to the output of this agent.
   *
   * @param that The agent to chain with this one
   * @return A new agent that represents the composition
   */
  def andThen[O2](that: ComposableAgent[O, O2]): ComposableAgent[I, O2] = {
    ComposableAgent(
      processImpl = input => process(input).flatMap(that.process),
      agentCapabilities = capabilities ++ that.capabilities,
      inType = inputType,
      outType = that.outputType,
      agentProperties = properties.view.filterKeys(!that.properties.contains(_)).toMap ++ that.properties
    )
  }
}

object ComposableAgent {
  
  /**
   * Create a simple ComposableAgent with the given properties
   *
   * @param processImpl Function that implements the processing logic
   * @param agentCapabilities Set of capabilities that this agent provides
   * @param inType Name of the input type
   * @param outType Name of the output type
   * @param agentProperties Optional custom properties for this agent
   * @return A new ComposableAgent instance
   */
  def apply[I, O](
    processImpl: I => Task[O],
    agentCapabilities: Set[String],
    inType: String,
    outType: String,
    agentProperties: Map[String, String] = Map.empty
  ): ComposableAgent[I, O] = {
    new ComposableAgent[I, O] {
      def process(input: I): Task[O] = processImpl(input)
      val capabilities: Set[String] = agentCapabilities
      val inputType: String = inType
      val outputType: String = outType
      val properties: Map[String, String] = agentProperties
    }
  }
  
  /**
   * Create a parallel composable agent that applies multiple agents to the same input
   * and combines their results.
   *
   * @param agents List of agents to apply in parallel
   * @param combiner Function to combine the results of all agents
   * @return A new ComposableAgent that applies agents in parallel
   */
  def parallel[I, O, R](
    agents: List[ComposableAgent[I, O]], 
    combiner: List[O] => R
  ): ComposableAgent[I, R] = {
    
    require(agents.nonEmpty, "Cannot create a parallel agent with no agents")
    
    val allCapabilities = agents.flatMap(_.capabilities).toSet
    val combinedProperties = agents.foldLeft(Map.empty[String, String]) { 
      (acc, agent) => acc ++ agent.properties
    }
    
    // All agents must have the same input type for parallel composition
    val inputTypeName = agents.head.inputType
    require(
      agents.forall(_.inputType == inputTypeName),
      s"All agents must have the same input type for parallel composition, but found: ${agents.map(_.inputType).distinct.mkString(", ")}"
    )
    
    ComposableAgent(
      processImpl = input => {
        for {
          // Process the input in parallel with all agents
          results <- ZIO.foreachPar(agents)(_.process(input))
          // Combine the results
          combined = combiner(results)
        } yield combined
      },
      agentCapabilities = allCapabilities,
      inType = inputTypeName,
      outType = "combined", // This is a special case; could be refined in future
      agentProperties = combinedProperties
    )
  }
}