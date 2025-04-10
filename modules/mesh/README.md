# Mesh Module

## Implementation Status

This module includes implementation status markers to clearly indicate the current state of each component:

- ✅ **Implemented**: Features that are fully implemented and tested
- 🚧 **In Progress**: Features that are partially implemented
- 🔮 **Planned**: Features planned for future development

## Overview

The Mesh module provides a distributed agent mesh network, enabling agents to discover and communicate with each other across different machines and processes. It includes agent discovery, remote communication protocols, and the infrastructure for building distributed AI agent systems.

## Current Status

Overall status: 🚧 **In Progress**

### Features

- ✅ **Agent Discovery**: Agent directory for registering and discovering agents by capabilities and metadata
- ✅ **Protocol Definitions**: Core protocol interfaces and message envelope definitions
- ✅ **Agent References**: Remote agent references for location transparency
- 🚧 **HTTP Transport**: HTTP server implementation for the mesh protocol
- 🚧 **Remote Agent Communication**: Call agents on remote nodes
- 🔮 **Agent Deployment**: Deploy agents to remote nodes in the mesh
- 🔮 **Fault Tolerance**: Graceful handling of node failures
- 🔮 **Security Features**: Authentication and encryption of agent communication

## Dependencies

This module depends on:

- `core`: Required - Uses the core agent interfaces and ZIO integration
- External HTTP libraries for implementing the transport layer

## Usage Examples

Basic agent discovery (in-memory):

```scala
import com.agenticai.mesh.discovery.*
import com.agenticai.mesh.protocol.*
import zio.*

// Create an in-memory agent directory
val directory = new InMemoryAgentDirectory()

// Register an agent with capabilities
val program = for {
  agentRef = RemoteAgentRef[String, String](java.util.UUID.randomUUID(), "http://localhost:8080/agent")
  metadata = AgentMetadata(
    capabilities = Set("translation", "text-processing"),
    inputType = "String",
    outputType = "String",
    properties = Map("language" -> "English")
  )
  _ <- directory.registerAgent(agentRef, metadata)
  
  // Discover agents by capabilities
  query = TypedAgentQuery(capabilities = Set("translation"))
  agents <- directory.discoverAgents(query)
} yield agents
```

### Common Patterns

```scala
// Subscribing to directory events
val eventProgram = ZIO.scoped {
  for {
    // Get event stream
    events <- ZIO.succeed(directory.subscribeToEvents())
    
    // Process events
    _ <- events.forEach { event =>
      event match {
        case DirectoryEvent.AgentRegistered(id, info) =>
          ZIO.debug(s"Agent registered: $id with capabilities ${info.metadata.capabilities}")
        case DirectoryEvent.AgentStatusChanged(id, oldStatus, newStatus) =>
          ZIO.debug(s"Agent $id changed status from $oldStatus to $newStatus")
        case _ => ZIO.unit
      }
    }.fork
  } yield ()
}
```

## Architecture

The Mesh module is organized into several key packages:

- `com.agenticai.mesh.discovery`: Agent discovery and directory services
- `com.agenticai.mesh.protocol`: Communication protocol definitions
- `com.agenticai.mesh.server`: HTTP server for mesh communication

The architecture follows a decentralized design where each node can act as both a client and a server. The discovery system enables agents to find each other by capabilities, while the protocol layer enables communication between them.

## Known Limitations

- 🚧 The distributed mesh example is currently disabled due to ongoing implementation
- 🚧 Remote agent communication is partially implemented but not fully tested
- 🔮 Security features are planned but not yet implemented
- 🔮 Advanced deployment capabilities are still under development

## Future Development

Planned enhancements:

- 🔮 Full mesh deployment functionality
- 🔮 Advanced security with authentication and encryption
- 🔮 Cross-language support via standard protocol
- 🔮 Federated learning across distributed agents
- 🔮 Self-healing mesh with automatic failover

## Testing

The module has tests for implemented components, particularly:

- Agent directory functionality
- Discovery mechanisms
- Protocol message handling

Run tests for this module with:
```bash
sbt "mesh/test"
```

For test coverage:
```bash
./scripts/run-tests-with-reports.sh --modules=mesh
```

## See Also

- [Agent Discovery Documentation](../../docs/mesh/AgentDiscovery.md)
- [Distributed Agent Mesh Documentation](../../docs/mesh/DistributedAgentMesh.md)
- [Mesh Implementation Plan](../../docs/mesh/MeshImplementationPlan.md)