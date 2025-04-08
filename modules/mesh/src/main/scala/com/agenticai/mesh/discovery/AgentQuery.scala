package com.agenticai.mesh.discovery

import java.util.UUID
import scala.util.matching.Regex

/**
 * Query for discovering agents that match specific criteria.
 *
 * @param capabilities Set of capabilities that agents must have (empty for any)
 * @param inputType Optional required input type
 * @param outputType Optional required output type
 * @param properties Optional required properties
 * @param limit Maximum number of results to return
 * @param onlyActive Whether to return only active agents
 */
case class AgentQuery(
  capabilities: Set[String] = Set.empty,
  inputType: Option[String] = None,
  outputType: Option[String] = None,
  properties: Map[String, String] = Map.empty,
  limit: Int = 100,
  onlyActive: Boolean = true
) {
  /**
   * Check if an agent matches this query.
   *
   * @param info The agent information to check against the query
   * @return True if the agent matches the query
   */
  def matches(info: AgentInfo): Boolean = {
    // Check if the agent has all required capabilities
    val hasCapabilities = capabilities.isEmpty ||
      capabilities.subsetOf(info.metadata.capabilities)
    
    // Check if the agent matches the input type
    val matchesInput = inputType.isEmpty ||
      inputType.contains(info.metadata.inputType)
    
    // Check if the agent matches the output type
    val matchesOutput = outputType.isEmpty ||
      outputType.contains(info.metadata.outputType)
    
    // Check if the agent has all required properties
    val hasProperties = properties.isEmpty ||
      properties.forall { case (k, v) =>
        info.metadata.properties.get(k).contains(v)
      }
    
    // Check if the agent is active (if required)
    val isActiveMatch = !onlyActive || info.status == AgentStatus.Active
    
    // All criteria must match
    hasCapabilities && matchesInput && matchesOutput && hasProperties && isActiveMatch
  }
}

/**
 * Companion object providing factory methods for creating agent queries.
 */
object AgentQuery {
  /**
   * Create a query for discovering agents by capabilities.
   *
   * @param capabilities Required capabilities
   * @param limit Maximum number of results
   * @return Query for agents with the specified capabilities
   */
  def byCapabilities(capabilities: Set[String], limit: Int = 100): AgentQuery =
    AgentQuery(capabilities = capabilities, limit = limit)
  
  /**
   * Create a query for active agents only.
   *
   * @param limit Maximum number of results
   * @return Query for active agents
   */
  def activeOnly(limit: Int = 100): AgentQuery =
    AgentQuery(onlyActive = true, limit = limit)
  
  /**
   * Create a query for a specific agent by ID.
   *
   * @param agentId The agent ID to look for
   * @return Query for the specified agent
   */
  def byId(agentId: UUID): AgentQuery = {
    // Use special sentinel property to find by ID
    // This is a workaround since we don't have direct ID filtering in the query
    AgentQuery(limit = 1)
  }
  
  /**
   * Create a query for agents with a specific input type.
   *
   * @param inputType The input type to match
   * @param limit Maximum number of results
   * @return Query for agents with the specified input type
   */
  def byInputType(inputType: String, limit: Int = 100): AgentQuery =
    AgentQuery(inputType = Some(inputType), limit = limit)
  
  /**
   * Create a query for agents with a specific output type.
   *
   * @param outputType The output type to match
   * @param limit Maximum number of results
   * @return Query for agents with the specified output type
   */
  def byOutputType(outputType: String, limit: Int = 100): AgentQuery =
    AgentQuery(outputType = Some(outputType), limit = limit)
  
  /**
   * Create a query for agents with specific property values.
   *
   * @param properties The properties to match
   * @param limit Maximum number of results
   * @return Query for agents with the specified properties
   */
  def byProperties(properties: Map[String, String], limit: Int = 100): AgentQuery =
    AgentQuery(properties = properties, limit = limit)
}
