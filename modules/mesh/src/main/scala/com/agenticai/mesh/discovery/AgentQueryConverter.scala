package com.agenticai.mesh.discovery

/** Converter utility to help with the transition between AgentQuery and TypedAgentQuery
  */
object AgentQueryConverter:

  /** Convert AgentQuery to TypedAgentQuery
    */
  def toTypedQuery(query: AgentQuery): TypedAgentQuery =
    TypedAgentQuery(
      capabilities = query.capabilities,
      inputType = query.inputType,
      outputType = query.outputType,
      properties = query.properties,
      limit = query.limit,
      onlyActive = query.onlyActive
    )
