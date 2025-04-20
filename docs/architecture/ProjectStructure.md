# ZIO Agentic AI Framework - Modular Project Structure

This document outlines the modular structure of the ZIO Agentic AI Framework, designed to provide better composition and allow consumers to use only the parts they need.

## Implementation Status

This document includes implementation status markers to clearly indicate the current state of each component:

- ✅ **Implemented**: Features that are fully implemented and tested
- 🚧 **In Progress**: Features that are partially implemented
- 🔮 **Planned**: Features planned for future development

## Module Overview

The framework is divided into the following modules:
1. ✅ **Core** - Essential interfaces and abstractions
2. ✅ **Memory** - Memory system implementation (basic functionality)
3. 🚧 **Agents** - Agent implementations
4. 🚧 **HTTP** - Web API implementation
5. 🚧 **Dashboard** - Web UI and visualizations
6. ✅ **Examples** - Example applications
7. ✅ **Workflow Demo** - Visual workflow UI builder demo
8. ✅ **Langchain4j** - Integration with Langchain4j for LLM access
9. 🚧 **Mesh** - Distributed agent mesh implementation
10. 🚧 **Integration Tests** - Integration tests for the framework

## Module Contents

### Core Module (`modules/core`)

Contains the essential interfaces, abstractions, and base types.

**Implementation Status**: ✅ **Implemented**

```
modules/core/
├── src/
│   ├── main/scala/com/agenticai/core/
│   │   ├── Agent.scala             # Agent interface
│   │   ├── BaseAgent.scala         # Base agent implementation
│   │   ├── errors/                 # Core error types
│   │   │   └── CoreError.scala
│   │   ├── models/                 # Common data models
│   │   │   └── AgentState.scala
│   │   └── util/                   # Utility functions
│   │       └── TypedId.scala
│   └── test/scala/com/agenticai/core/
│       └── [Core tests]
```

### Memory Module (`modules/memory`)

Contains the memory system implementation, with cells and persistence.

**Implementation Status**:
- ✅ **Implemented**: Basic MemoryCell and MemorySystem interfaces
- ✅ **Implemented**: InMemorySystem implementation
- ✅ **Implemented**: PersistentMemorySystem implementation
- 🚧 **In Progress**: Advanced memory monitoring and cleanup strategies

```
modules/memory/
├── src/
│   ├── main/scala/com/agenticai/memory/
│   │   ├── MemoryCell.scala             # Memory cell interface
│   │   ├── MemorySystem.scala           # Memory system interface
│   │   ├── PersistentMemorySystem.scala # Persistent memory implementation
│   │   ├── InMemorySystem.scala         # In-memory implementation
│   │   ├── errors/                      # Memory-specific errors
│   │   │   └── MemoryError.scala
│   │   ├── models/                      # Memory models
│   │   │   └── MemoryCellMetadata.scala
│   │   └── persistence/                 # Persistence implementations
│   │       ├── FilePersistence.scala
│   │       └── DatabasePersistence.scala
│   └── test/scala/com/agenticai/memory/
│       ├── MemoryCellSpec.scala
│       ├── MemorySystemSpec.scala
│       └── PersistentMemorySystemSpec.scala
```

### Agents Module (`modules/agents`)

Contains implementations of various agent types and agent-related utilities.

**Implementation Status**: 🚧 **In Progress**

```
modules/agents/
├── src/
│   ├── main/scala/com/agenticai/agents/
│   │   ├── chat/                        # Chat-related agents
│   │   │   ├── ChatAgent.scala
│   │   │   └── models/
│   │   │       └── Message.scala
│   │   ├── task/                        # Task-related agents
│   │   │   ├── TaskAgent.scala
│   │   │   └── models/
│   │   │       ├── Task.scala
│   │   │       └── TaskModels.scala
│   │   ├── util/                        # Agent utilities
│   │   │   ├── AgentMetrics.scala
│   │   │   └── AgentLogging.scala
│   │   └── errors/                      # Agent-specific errors
│   │       └── AgentError.scala
│   └── test/scala/com/agenticai/agents/
│       ├── chat/
│       │   └── ChatAgentSpec.scala
│       └── task/
│           └── TaskAgentSpec.scala
```

### HTTP Module (`modules/http`)

Contains the HTTP API implementation.

**Implementation Status**: 🚧 **In Progress**

```
modules/http/
├── src/
│   ├── main/scala/com/agenticai/http/
│   │   ├── server/                      # Server implementation
│   │   │   ├── HttpServer.scala
│   │   │   └── WebSocketServer.scala
│   │   ├── api/                         # API endpoints
│   │   │   ├── AgentEndpoints.scala
│   │   │   ├── MemoryEndpoints.scala
│   │   │   └── models/
│   │   │       └── ApiModels.scala
│   │   ├── json/                        # JSON serialization
│   │   │   └── JsonCodecs.scala
│   │   └── errors/                      # HTTP-specific errors
│   │       └── ApiError.scala
│   └── test/scala/com/agenticai/http/
│       └── [HTTP tests]
```

### Dashboard Module (`modules/dashboard`)

Contains the web dashboard for visualization and debugging.

**Implementation Status**: 🚧 **In Progress**

```
modules/dashboard/
├── src/
│   ├── main/
│   │   ├── resources/
│   │   │   └── public/                  # Static assets
│   │   │       ├── index.html
│   │   │       └── assets/
│   │   │           ├── css/
│   │   │           │   └── styles.css
│   │   │           └── js/
│   │   │               └── dashboard.js
│   │   └── scala/com/agenticai/dashboard/
│   │       ├── server/                  # Dashboard server
│   │       │   └── DashboardServer.scala
│   │       ├── visualizations/          # Visualization components
│   │       │   ├── MemoryVisualizer.scala
│   │       │   ├── AgentStateVisualizer.scala
│   │       │   └── TaskVisualizer.scala
│   │       └── models/                  # Dashboard-specific models
│   │           └── DashboardModels.scala
│   └── test/scala/com/agenticai/dashboard/
│       └── [Dashboard tests]
```

### Examples Module (`modules/examples`)

Contains example applications demonstrating framework usage.

**Implementation Status**: ✅ **Implemented**

```
modules/examples/
├── src/
│   ├── main/
│   │   ├── resources/                   # Example-specific resources
│   │   │   └── [example resources]
│   │   └── scala/com/agenticai/examples/
│   │       ├── chat/                    # Chat examples
│   │       │   ├── ChatExample.scala
│   │       │   └── ChatBotApp.scala
│   │       ├── webdashboard/            # Web dashboard example
│   │       │   ├── WebDashboardApp.scala
│   │       │   ├── models/
│   │       │   │   ├── Task.scala
│   │       │   │   └── TaskModels.scala
│   │       │   ├── agents/
│   │       │   │   └── TaskProcessorAgent.scala
│   │       │   └── api/
│   │       │       └── AgentAPI.scala
│   │       └── dataprocessing/          # Data processing examples
│   │           └── DataProcessingExample.scala
│   └── test/scala/com/agenticai/examples/
│       ├── chat/
│       │   └── ChatExampleSpec.scala
│       └── webdashboard/
│           └── agents/
│               └── TaskProcessorAgentSpec.scala
```

### Workflow Demo Module (`modules/workflow-demo`)

Contains a visual workflow UI builder demonstration.

**Implementation Status**: ✅ **Implemented**

**Note**: This module currently uses ZIO HTTP 3.0.0-RC4, which differs from the version used in other modules (3.0.0-RC2). The module has been temporarily configured to work independently of core and memory dependencies to ensure functionality.

```
modules/workflow-demo/
├── src/
│   ├── main/
│   │   ├── resources/                   # Workflow demo resources
│   │   │   └── public/                  # Static assets
│   │   │       ├── index.html
│   │   │       └── assets/
│   │   └── scala/com/agenticai/workflow/
│   │       ├── WorkflowDemoLauncher.scala
│   │       ├── server/                  # Server implementation
│   │       ├── agents/                  # Demo agents
│   │       └── models/                  # Workflow models
│   └── test/scala/com/agenticai/workflow/
│       └── [Workflow tests]
├── docs/
│   ├── WorkflowDemo_Documentation.md
│   └── WorkflowDemo_TroubleshootingGuide.md
└── run-workflow-demo.sh                 # Startup script
```

### Langchain4j Module (`modules/langchain4j`)

Integration with Langchain4j for accessing LLM models.

**Implementation Status**: ✅ **Implemented** (with some features in progress)

**Note**: While the core integration is implemented, some advanced features are still in development:
- 🚧 **In Progress**: Tool support
- 🚧 **In Progress**: Some advanced Langchain4j features

```
modules/langchain4j/
├── src/
│   ├── main/scala/com/agenticai/core/llm/langchain/
│   │   ├── ZIOChatModelFactory.scala
│   │   ├── VertexAIModelSupport.scala
│   │   └── [Other integration files]
│   └── test/scala/com/agenticai/core/llm/langchain/
│       └── [Langchain4j tests]
```

### Mesh Module (`modules/mesh`)

Distributed agent mesh network implementation.

**Implementation Status**:
- ✅ **Implemented**: Agent discovery interfaces and in-memory implementation
- 🚧 **In Progress**: Distributed mesh communication
- 🔮 **Planned**: Advanced mesh features like agent migration

```
modules/mesh/
├── src/
│   ├── main/scala/com/agenticai/mesh/
│   │   ├── discovery/                   # Agent discovery
│   │   ├── communication/               # Agent communication
│   │   └── topology/                    # Mesh topology
│   └── test/scala/com/agenticai/mesh/
│       └── [Mesh tests]
```

### Integration Tests (`it` and `modules/integration-tests`)

The framework currently has two separate integration test setups:

1. **Langchain4j Integration Tests** (`it/`): Focused on testing Langchain4j integration with real LLM providers.

**Implementation Status**: ✅ **Implemented**

```
it/
├── src/
│   └── test/scala/com/agenticai/core/llm/
│       ├── ClaudeIntegrationSpec.scala
│       ├── VertexAIGeminiIntegrationSpec.scala
│       └── GoogleAIGeminiIntegrationSpec.scala
```

2. **Framework Integration Tests** (`modules/integration-tests/`): Tests for integration between framework modules.

**Implementation Status**: 🚧 **In Progress**

```
modules/integration-tests/
├── src/
│   └── test/scala/com/agenticai/core/
│       └── [Integration tests]
```

**Note**: The integration test setup is currently being consolidated to improve organization and test coverage.

## Module Dependencies

The following diagram shows the dependencies between modules:

```
core <---- memory <---- agents <---- http <---- dashboard
  ^          ^            ^           ^            ^
  |          |            |           |            |
  +----------+------------+-----------+------------+
                          |
                        examples
```
- **core**: No dependencies ✅
- **memory**: Depends on core ✅
- **agents**: Depends on core and memory 🚧
- **http**: Depends on core, memory, and agents 🚧
- **dashboard**: Depends on core, memory, agents, and http 🚧
- **workflow-demo**: Depends on core ✅
- **langchain4j**: Depends on core ✅
- **mesh**: Depends on core ✅
- **examples**: Depends on multiple modules ✅
- **integration-tests**: Depends on all modules 🚧
- **it**: Depends on core and langchain4j ✅

## Benefits of Modular Structure

The modular structure provides several benefits:

1. **Selective Dependency**: Consumers can depend only on the modules they need
2. **Parallel Development**: Teams can work on different modules simultaneously
3. **Focused Testing**: Each module has its own test suite
4. **Clear Boundaries**: Well-defined interfaces between modules
5. **Versioning Flexibility**: Modules can be versioned independently
6. **Reduced Compilation Time**: Changes in one module don't require recompiling the entire codebase

## Migration Steps

The project is transitioning from a monolithic structure to a modular one. The migration involves:

1. ✅ Creating module directories
2. ✅ Setting up build.sbt for each module
3. ✅ Moving files to appropriate modules
4. ✅ Updating imports and dependencies
5. 🚧 Refactoring code to respect module boundaries
6. 🚧 Updating documentation to reflect the new structure