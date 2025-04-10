# Agentic AI Framework - Modular Project Structure

This document outlines the modular structure of the Agentic AI Framework, designed to provide better composition and allow consumers to use only the parts they need.

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
6. **Examples** - Example applications

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
### Workflow Demo Module (`modules/workflow-demo`)

Contains a visual workflow UI builder demonstration.

**Implementation Status**: ✅ **Implemented**

### Langchain4j Module (`modules/langchain4j`)

Integration with Langchain4j for accessing LLM models.

**Implementation Status**: ✅ **Implemented**

### Mesh Module (`modules/mesh`)

Distributed agent mesh network implementation.

**Implementation Status**:
- ✅ **Implemented**: Agent discovery interfaces and in-memory implementation
- 🚧 **In Progress**: Distributed mesh communication
- 🔮 **Planned**: Advanced mesh features like agent migration

```

## File Migration Plan

### Current Files to Migrate

This section outlines how existing files will map to the new modular structure.

#### Core Files:
- `src/main/scala/com/agenticai/core/Agent.scala` → `modules/core/src/main/scala/com/agenticai/core/Agent.scala`
- `src/main/scala/com/agenticai/core/BaseAgent.scala` → `modules/core/src/main/scala/com/agenticai/core/BaseAgent.scala`

#### Memory Files:
- `src/main/scala/com/agenticai/core/memory/MemoryCell.scala` → `modules/memory/src/main/scala/com/agenticai/memory/MemoryCell.scala`
- `src/main/scala/com/agenticai/core/memory/MemorySystem.scala` → `modules/memory/src/main/scala/com/agenticai/memory/MemorySystem.scala`
- `src/main/scala/com/agenticai/core/memory/PersistentMemorySystem.scala` → `modules/memory/src/main/scala/com/agenticai/memory/PersistentMemorySystem.scala`
- `src/test/scala/com/agenticai/core/memory/MemoryCellSpec.scala` → `modules/memory/src/test/scala/com/agenticai/memory/MemoryCellSpec.scala`
- `src/test/scala/com/agenticai/core/memory/MemorySystemSpec.scala` → `modules/memory/src/test/scala/com/agenticai/memory/MemorySystemSpec.scala`
- `src/test/scala/com/agenticai/core/memory/PersistentMemorySystemSpec.scala` → `modules/memory/src/test/scala/com/agenticai/memory/PersistentMemorySystemSpec.scala`

#### Agent Files:
- `src/main/scala/com/agenticai/example/ChatAgent.scala` → `modules/agents/src/main/scala/com/agenticai/agents/chat/ChatAgent.scala`
- `src/main/scala/com/agenticai/examples/webdashboard/agents/TaskProcessorAgent.scala` → `modules/agents/src/main/scala/com/agenticai/agents/task/TaskProcessor.scala`
- `src/test/scala/com/agenticai/examples/webdashboard/agents/TaskProcessorAgentSpec.scala` → `modules/agents/src/test/scala/com/agenticai/agents/task/TaskProcessorSpec.scala`

#### HTTP Files:
- `src/main/scala/com/agenticai/examples/webdashboard/api/AgentAPI.scala` → `modules/http/src/main/scala/com/agenticai/http/api/AgentEndpoints.scala`

#### Dashboard Files:
- `src/main/resources/public/index.html` → `modules/dashboard/src/main/resources/public/index.html`
- `src/main/resources/public/assets/css/styles.css` → `modules/dashboard/src/main/resources/public/assets/css/styles.css`
- `src/main/resources/public/assets/js/dashboard.js` → `modules/dashboard/src/main/resources/public/assets/js/dashboard.js`

#### Example Files:
- `src/main/scala/com/agenticai/examples/ChatExample.scala` → `modules/examples/src/main/scala/com/agenticai/examples/chat/ChatExample.scala`
- `src/test/scala/com/agenticai/examples/ChatExampleSpec.scala` → `modules/examples/src/test/scala/com/agenticai/examples/chat/ChatExampleSpec.scala`
- `src/main/scala/com/agenticai/examples/webdashboard/models/Task.scala` → `modules/examples/src/main/scala/com/agenticai/examples/webdashboard/models/Task.scala`
- `src/main/scala/com/agenticai/examples/webdashboard/models/TaskModels.scala` → `modules/examples/src/main/scala/com/agenticai/examples/webdashboard/models/TaskModels.scala`

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
- **workflow-demo**: Depends on core and examples ✅
- **langchain4j**: Depends on core ✅
- **mesh**: Depends on core ✅
- **examples**: Depends on multiple modules ✅
- **examples**: Depends on all other modules

## Benefits of Modular Structure

1. **Reduced Dependency Footprint**: Consumers can include only the modules they need
2. **Clear Separation of Concerns**: Each module has a well-defined responsibility
3. **Independent Versioning**: Modules can be versioned separately if needed
4. **Better Testing**: Focused test suites for each module
5. **Independent Development**: Teams can work on different modules in parallel
6. **Simplified Maintenance**: Easier to understand and maintain smaller modules
7. **Flexible Deployment**: Deploy only the needed components

## Migration Steps

1. Create the module directory structure
2. Move files to their new locations, updating package declarations
3. Update import statements to reflect new package structure
4. Run comprehensive tests on each module
5. Update documentation to reflect the new structure