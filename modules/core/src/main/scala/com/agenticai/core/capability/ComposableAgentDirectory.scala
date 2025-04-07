package com.agenticai.core.capability

import zio._
import scala.collection.mutable
import com.agenticai.core.capability.CapabilityTaxonomy._

/**
 * A directory for composable agents that enables discovery and workflow creation.
 * 
 * ComposableAgentDirectory maintains a registry of agents and provides methods to:
 * - Find agents by capabilities
 * - Find agents by input/output types
 * - Create workflows by composing appropriate agents
 * 
 * @param capabilityRegistry The capability taxonomy registry to use for capability relationships
 */
class ComposableAgentDirectory(capabilityRegistry: CapabilityRegistry) {
  
  // Agent storage with unique IDs
  private val agents = mutable.Map.empty[String, ComposableAgent[_, _]]
  private var nextAgentId = 0
  
  /**
   * Register an agent in the directory
   * 
   * @param agent The agent to register
   * @return The unique ID assigned to the agent
   */
  def registerAgent[I, O](agent: ComposableAgent[I, O]): UIO[String] = ZIO.succeed {
    val id = s"agent-${nextAgentId}"
    nextAgentId += 1
    agents(id) = agent
    id
  }
  
  /**
   * Get an agent by its ID
   * 
   * @param id The agent ID
   * @return The agent if found, None otherwise
   */
  def getAgent(id: String): UIO[Option[ComposableAgent[_, _]]] = 
    ZIO.succeed(agents.get(id))
  
  /**
   * Find agents that have all the required capabilities
   *
   * @param requiredCapabilities The set of capabilities that agents must have
   * @return A list of agents that satisfy the capabilities
   */
  def findAgentsByCapabilities(requiredCapabilities: Set[String]): UIO[List[ComposableAgent[_, _]]] = {
    if (requiredCapabilities.isEmpty) {
      // If no capabilities required, return all agents
      ZIO.succeed(agents.values.toList)
    } else {
      // Simple implementation - directly filter agents that have the required capabilities
      // We'll just check direct capability matches for now
      ZIO.succeed {
        agents.values.filter { agent =>
          requiredCapabilities.forall { reqCap =>
            agent.capabilities.contains(reqCap)
          }
        }.toList
      }
    }
  }
  
  /**
   * Find agents by input and output types
   * 
   * @param inputType The required input type
   * @param outputType The required output type
   * @return A list of agents that match the types
   */
  def findAgentsByTypes(inputType: String, outputType: String): UIO[List[ComposableAgent[_, _]]] = {
    ZIO.succeed {
      agents.values.filter { agent =>
        agent.inputType == inputType && agent.outputType == outputType
      }.toList
    }
  }
  
  /**
   * Create a workflow by composing appropriate agents
   * 
   * @param inType The input type for the workflow
   * @param outType The output type for the workflow
   * @param requiredCapabilities Optional set of capabilities the workflow must provide
   * @return An optional composed agent forming the workflow
   */
  def createWorkflow[I, O](
    inType: String, 
    outType: String,
    requiredCapabilities: Set[String] = Set.empty
  ): UIO[Option[ComposableAgent[I, O]]] = {
    for {
      // First try to find direct matches (agents that can process from inType to outType)
      directMatches <- findAgentsByTypes(inType, outType)
      
      // Filter direct matches by capabilities if specified
      capableDirectMatches <- if (requiredCapabilities.isEmpty) {
        ZIO.succeed(directMatches)
      } else {
        for {
          matchingAgents <- findAgentsByCapabilities(requiredCapabilities)
          capableDirects = directMatches.filter(agent => 
            matchingAgents.exists(_.capabilities == agent.capabilities)
          )
        } yield capableDirects
      }
      
      workflow <- if (capableDirectMatches.nonEmpty) {
        // We found a direct match with required capabilities
        ZIO.succeed(Some(capableDirectMatches.head.asInstanceOf[ComposableAgent[I, O]]))
      } else {
        // Try to create a multi-step workflow
        createMultiStepWorkflow(inType, outType, requiredCapabilities)
      }
    } yield workflow
  }
  
  /**
   * Create a multi-step workflow by finding intermediate agents
   * 
   * @param inType The input type
   * @param outType The output type
   * @param requiredCapabilities Required capabilities
   * @return An optional composed agent forming the workflow
   */
  private def createMultiStepWorkflow[I, O](
    inType: String, 
    outType: String,
    requiredCapabilities: Set[String]
  ): UIO[Option[ComposableAgent[I, O]]] = {
    // Get all agents that match required capabilities
    findAgentsByCapabilities(requiredCapabilities).flatMap { capableAgents =>
      if (capableAgents.isEmpty) {
        // No agents found with the required capabilities
        ZIO.succeed(None)
      } else {
        // Find potential starting agents (that accept inType)
        val startAgents = capableAgents.filter(_.inputType == inType)
        
        if (startAgents.isEmpty) {
          // No starting point found
          ZIO.succeed(None)
        } else {
          // Try to build a path from inType to outType
          val paths = findPaths(startAgents, outType, Set(inType))
          
          if (paths.isEmpty) {
            ZIO.succeed(None)
          } else {
            // Take the shortest path
            val shortestPath = paths.minBy(_.length)
            
            // Compose the agents in the path
            val composedAgent = composeAgentsInPath(shortestPath).asInstanceOf[ComposableAgent[I, O]]
            ZIO.succeed(Some(composedAgent))
          }
        }
      }
    }
  }
  
  /**
   * Find all possible paths from the starting agents to the target output type
   * 
   * @param startAgents Agents to start from
   * @param targetOutputType The target output type
   * @param visitedTypes Types already visited (to prevent cycles)
   * @return List of paths (each path is a list of agents)
   */
  private def findPaths(
    startAgents: List[ComposableAgent[_, _]],
    targetOutputType: String,
    visitedTypes: Set[String]
  ): List[List[ComposableAgent[_, _]]] = {
    // Find direct matches to target
    val directMatches = startAgents.filter(_.outputType == targetOutputType)
    
    // Initialize with direct matches
    val directPaths = directMatches.map(agent => List(agent))
    
    // Find indirect paths through intermediate steps
    val intermediatePaths = startAgents.flatMap { agent =>
      val nextType = agent.outputType
      
      if (nextType == targetOutputType || visitedTypes.contains(nextType)) {
        // Already found a direct match or would create a cycle
        Nil
      } else {
        // Find next agents that can process the output of this agent
        val nextAgents = agents.values.filter(_.inputType == nextType).toList
        
        // Recursively find paths from these next agents
        val remainingPaths = findPaths(nextAgents, targetOutputType, visitedTypes + nextType)
        
        // Prepend current agent to each found path
        remainingPaths.map(path => agent :: path)
      }
    }
    
    // Combine direct and intermediate paths
    directPaths ++ intermediatePaths
  }
  
  /**
   * Compose a sequence of agents into a single agent
   * 
   * @param agents The sequence of agents to compose
   * @return A composed agent
   */
  private def composeAgentsInPath(agents: List[ComposableAgent[_, _]]): ComposableAgent[_, _] = {
    agents.reduceLeft { (acc, next) =>
      acc.asInstanceOf[ComposableAgent[Any, Any]].andThen(next.asInstanceOf[ComposableAgent[Any, Any]])
    }
  }
}

object ComposableAgentDirectory {
  /**
   * Create a new ComposableAgentDirectory
   * 
   * @param capabilityRegistry The capability taxonomy registry to use
   * @return A new ComposableAgentDirectory instance
   */
  def apply(capabilityRegistry: CapabilityRegistry): ComposableAgentDirectory = 
    new ComposableAgentDirectory(capabilityRegistry)
}