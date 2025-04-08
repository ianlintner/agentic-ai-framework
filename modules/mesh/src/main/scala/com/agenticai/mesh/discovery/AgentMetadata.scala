package com.agenticai.mesh.discovery

import java.time.Instant

/**
 * Metadata about an agent's capabilities and characteristics.
 *
 * @param capabilities Set of capability identifiers that the agent provides
 * @param inputType Description of the agent's input type
 * @param outputType Description of the agent's output type
 * @param properties Additional properties of the agent
 * @param version Optional version information
 */
case class AgentMetadata(
  capabilities: Set[String],
  inputType: String,
  outputType: String,
  properties: Map[String, String] = Map.empty,
  version: Option[String] = None
)

/**
 * Query for discovering agents using type constraints.
 *
 * @param capabilities Optional set of required capabilities
 * @param inputType Optional required input type
 * @param outputType Optional required output type
 * @param properties Optional required properties
 * @param limit Maximum number of results to return
 */
case class TypedAgentQuery(
  capabilities: Set[String] = Set.empty,
  inputType: Option[String] = None,
  outputType: Option[String] = None,
  properties: Map[String, String] = Map.empty,
  limit: Int = 10,
  onlyActive: Boolean = true
) {
  /**
   * Check if an agent matches this query based on type constraints.
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
      properties.forall { case (k, v) => info.metadata.properties.get(k).contains(v) }
    
    // Check if the agent is active (if required)
    val isActiveMatch = !onlyActive || info.status == AgentStatus.Active
    
    hasCapabilities && matchesInput && matchesOutput && hasProperties && isActiveMatch
  }
}