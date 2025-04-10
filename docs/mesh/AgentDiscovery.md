# Agent Discovery in Distributed Mesh

## Implementation Status

This document includes implementation status markers to clearly indicate the current state of each component:

- ✅ **Implemented**: Features that are fully implemented and tested
- 🚧 **In Progress**: Features that are partially implemented
- 🔮 **Planned**: Features planned for future development

## Overview

Agent Discovery is a crucial component of the Distributed Agent Mesh that enables agents to find and collaborate with each other based on their capabilities. It provides a registry system for agents to advertise their capabilities and a discovery mechanism for finding appropriate agents to solve specific problems.

## Core Concepts

### Agent Metadata ✅

Each agent in the mesh can register metadata that describes its capabilities:

- ✅ **Capabilities**: Set of strings representing the agent's abilities (e.g., "translation", "sentiment-analysis")
- ✅ **Input/Output Types**: The types of data the agent can process and produce
- ✅ **Properties**: Additional key-value pairs describing agent characteristics
- 🚧 **Version**: Optional version information

### Agent Status ✅

Agents have a status that indicates their availability:

- ✅ **Active**: Ready to process requests
- ✅ **Unavailable**: Temporarily not accepting requests
- ✅ **Overloaded**: Running at capacity, only critical operations should be sent
- ✅ **Initializing**: Starting up, not yet ready
- ✅ **ShuttingDown**: In the process of terminating
- ✅ **Deregistered**: Permanently removed from the mesh

### Directory Events ✅

The discovery system publishes events when changes occur:

- ✅ **AgentRegistered**: New agent added to the directory
- ✅ **AgentUnregistered**: Agent removed from the directory
- ✅ **AgentStatusChanged**: Agent's status updated
- ✅ **AgentMetadataUpdated**: Agent's capabilities or properties changed
- ✅ **AgentLoadChanged**: Agent's load factor changed

## Using Agent Discovery

**Implementation Status**:
- ✅ **Implemented**: In-memory agent directory
- ✅ **Implemented**: Basic agent discovery by capabilities and properties
- 🚧 **In Progress**: Remote agent discovery
- 🔮 **Planned**: Smart agent selection and routing

### Registering an Agent with Capabilities

```scala
// Create an agent
val translationAgent = new TranslationAgent()

// Register with capabilities
mesh.registerWithCapabilities(
  translationAgent,
  AgentMetadata(
    capabilities = Set("translation", "natural-language-processing"),
    inputType = "TranslationRequest",
    outputType = "String",
    properties = Map("supported-languages" -> "English,Spanish,French,German")
  )
)
```

### Discovering Agents

#### By Capability

```scala
// Find agents that can perform translation
val translationAgents = mesh.findAgentsByCapabilities(Set("translation"))
```

#### By Input/Output Types

```scala
// Find agents that take String input and produce SentimentResult output
val sentimentAgents = mesh.findAgentsByTypes(
  inputType = "String",
  outputType = "SentimentResult"
)
```

#### Advanced Queries

```scala
// Custom query combining multiple criteria
val customQuery = AgentQuery(
  capabilities = Set("natural-language-processing"),
  inputType = Some("String"),
  properties = Map("supported-languages" -> "Spanish"),
  limit = 5,
  onlyActive = true
)

val matchingAgents = mesh.discoverAgents(customQuery)
```

### Using Discovered Agents

Once you've found suitable agents, you can get a reference to them and use them:

```scala
for {
  // Find agents with specific capabilities
  agents <- mesh.findAgentsByCapabilities(Set("translation"))
  
  // Get the first matching agent
  translationAgent <- ZIO.fromOption(agents.headOption)
    .orElseFail(new Exception("No translation agent found"))
    
  // Get a usable agent reference
  agent <- mesh.getAgent(translationAgent)
  
  // Use the agent
  request = TranslationRequest("Hello world", "English", "Spanish")
  result <- agent.asInstanceOf[Agent[TranslationRequest, String]].process(request)
} yield result
```

### Subscribing to Directory Events

You can subscribe to events to be notified when agents are added, removed, or changed:

```scala
val eventStream = mesh.subscribeToDirectoryEvents()

// Process events
eventStream.foreach { event =>
  event match {
    case DirectoryEvent.AgentRegistered(id, info, _) =>
      println(s"New agent registered: $id with capabilities ${info.metadata.capabilities}")
      
    case DirectoryEvent.AgentStatusChanged(id, oldStatus, newStatus, _) =>
      println(s"Agent $id changed status from $oldStatus to $newStatus")
      
    case _ => // Handle other events
  }
}
```

## Advanced Features

**Implementation Status**:
- 🚧 **In Progress**: Agent health monitoring
- 🔮 **Planned**: Smart agent selection
- 🔮 **Planned**: Integration with memory systems

### Agent Health Monitoring

Agents can update their status to reflect their current health:

```scala
// Update an agent's status
mesh.updateAgentStatus(agentId, AgentStatus.Overloaded)
```

### Smart Agent Selection

Beyond basic discovery, you can implement strategies for selecting the best agent:

```scala
// Find the least loaded translation agent
def findBestTranslationAgent(): Task[Option[Agent[TranslationRequest, String]]] = {
  for {
    agents <- mesh.findAgentsByCapabilities(Set("translation"))
    
    // Sort by load factor (lowest first) and get the first one
    bestAgentInfo <- ZIO.succeed(agents.sortBy(_.loadFactor.getOrElse(1.0)).headOption)
    
    // Convert to usable agent
    bestAgent <- ZIO.foreach(bestAgentInfo)(mesh.getAgent)
  } yield bestAgent.map(_.asInstanceOf[Agent[TranslationRequest, String]])
}
```

## Integration with Memory Systems 🔮

**Implementation Status**: 🔮 **Planned**

The agent discovery mechanism can be integrated with the framework's memory systems to enable agents to remember successful collaborations:

```scala
// Example of remembering successful collaborations
def rememberSuccessfulCollaboration(agentInfo: AgentInfo, task: String, success: Boolean): Task[Unit] = {
  memorySystem.getMemoryCell[Map[String, Set[AgentInfo]]](s"collaborations/$task")
    .flatMap { cell =>
      // Update memory with successful collaborations
      cell.update { existingMap =>
        val agentSet = existingMap.getOrElse(task, Set.empty)
        val updatedSet = if (success) agentSet + agentInfo else agentSet - agentInfo
        existingMap.updated(task, updatedSet)
      }
    }
}
```

## Implementing Custom Discovery Logic 🔮

**Implementation Status**: 🔮 **Planned**

You can extend the discovery system with custom logic by implementing your own `AgentDirectory` or wrapping the existing one:

```scala
class CustomAgentDirectory(wrapped: AgentDirectory) extends AgentDirectory {
  // Delegate most methods to the wrapped directory
  
  // Customize the discovery logic
  override def discoverAgents(query: AgentQuery): Task[List[AgentInfo]] = {
    for {
      // Get basic results from the wrapped directory
      baseResults <- wrapped.discoverAgents(query)
      
      // Apply custom ranking or filtering
      customResults = baseResults
        .filter(customFilterLogic)
        .sortBy(customRankingLogic)
    } yield customResults
  }
  
  // Helper methods
  private def customFilterLogic(info: AgentInfo): Boolean = {
    // Custom filtering logic
    true
  }
  
  private def customRankingLogic(info: AgentInfo): Double = {
    // Custom ranking logic
    0.0
  }
}
```

## Best Practices

**Implementation Status**: 🚧 **In Progress**

1. **Specific Capabilities**: Define granular capabilities that precisely describe what an agent can do
2. **Consistent Naming**: Use a consistent naming scheme for capabilities across your application
3. **Regular Status Updates**: Keep agent status and load information up-to-date
4. **Fallback Logic**: Always have fallback logic when no suitable agent is found
5. **Event Handling**: Process directory events to maintain an up-to-date view of the mesh
6. **Capability Composition**: Compose complex workflows from multiple specialized agents
7. **Versioning**: Include version information in agent metadata to manage upgrades

## Conclusion

The Agent Discovery mechanism forms a crucial part of the distributed agent mesh, enabling dynamic composition of agent capabilities and intelligent routing of tasks to the most appropriate agents. By leveraging this system, you can build flexible, scalable, and resilient agent networks that adapt to changing requirements and resource availability.

## Current Development Status

The agent discovery system is currently partially implemented with:
- ✅ In-memory agent directory with basic discovery functionality
- ✅ Support for capabilities, metadata, and status management
- ✅ Event publication for directory changes
- 🚧 Remote agent discovery is under active development
- 🔮 Advanced features like smart selection and memory integration are planned for future releases

Note that the distributed mesh example is currently disabled in the codebase and will be restored in a future update as the implementation progresses.