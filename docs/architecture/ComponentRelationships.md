# ZIO Agentic AI Framework Component Relationships

**Version:** 1.0.0  
**Last Updated:** April 19, 2025  
**Author:** ZIO Agentic AI Framework Team  

## Table of Contents

1. [Overview](#overview)
2. [High-Level Architecture](#high-level-architecture)
3. [Module Dependencies](#module-dependencies)
4. [Data Flow](#data-flow)
5. [Communication Patterns](#communication-patterns)
6. [Extension Points](#extension-points)
7. [Runtime Interactions](#runtime-interactions)

## Overview

This document describes the relationships between components in the ZIO Agentic AI Framework, including dependencies, data flows, and communication patterns. Understanding these relationships is essential for both users and contributors to effectively work with the framework.

## High-Level Architecture

The ZIO Agentic AI Framework is organized into a layered architecture with clear separation of concerns:

```mermaid
graph TD
    classDef core fill:#f9f,stroke:#333,stroke-width:2px;
    classDef supporting fill:#bbf,stroke:#333,stroke-width:1px;
    classDef optional fill:#bfb,stroke:#333,stroke-width:1px;
    
    A[Applications]
    B[Examples Module]
    C[Dashboard Module]
    D[Workflow Demo]
    
    E[Agents Module]
    F[Mesh Module]
    G[HTTP Module]
    
    H[Core Module]
    I[Memory Module]
    J[Langchain4j Module]
    
    A --> B
    A --> C
    A --> D
    
    B --> E
    B --> F
    B --> G
    
    C --> E
    C --> G
    
    D --> E
    D --> F
    
    E --> H
    E --> I
    E --> J
    
    F --> H
    F --> I
    
    G --> H
    
    I --> H
    J --> H
    
    class H core;
    class I,J supporting;
    class E,F,G supporting;
    class B,C,D optional;
```

**Legend:**
- Core components (pink)
- Supporting infrastructure (blue)
- Optional modules (green)

## Module Dependencies

The following table shows the direct dependencies between modules:

| Module | Depends On | Purpose |
|--------|------------|---------|
| Core | None | Foundational data structures and utilities |
| Memory | Core | Persistent and temporary storage capabilities |
| Langchain4j | Core | LLM integration and API wrappers |
| Agents | Core, Memory, Langchain4j | Agent capabilities and lifecycle |
| Mesh | Core, Memory | Distributed agent communication |
| HTTP | Core | HTTP server and client capabilities |
| Examples | Agents, Mesh, HTTP | Demonstration of framework features |
| Dashboard | Agents, HTTP | Web UI for agent monitoring |
| Workflow Demo | Agents, Mesh | Working implementation of workflow system |

## Data Flow

The following diagram illustrates the primary data flows between components:

```mermaid
flowchart TD
    User([User]) <--> Dashboard
    User <--> HTTP
    External([External Systems]) <--> HTTP
    
    subgraph "Agent Ecosystem"
        Dashboard --> |"Monitoring\nData"| Agents
        HTTP --> |"Requests"| Agents
        Agents --> |"Responses"| HTTP
        Agents <--> |"Messages"| Mesh
        Agents --> |"Store"| Memory
        Agents --> |"Retrieve"| Memory
        Agents --> |"Prompts"| Langchain4j
        Langchain4j --> |"Responses"| Agents
    end
    
    subgraph "Foundation"
        Agents --> |"Use"| Core
        Mesh --> |"Use"| Core
        Memory --> |"Use"| Core
        Langchain4j --> |"Use"| Core
        HTTP --> |"Use"| Core
    end
    
    LLM[(LLM Providers)] <--> Langchain4j
```

## Communication Patterns

ZIO Agentic AI Framework components communicate using several patterns:

### 1. Direct Method Calls

For in-process communication, components use direct method calls wrapped in ZIO effects:

```scala
// Example: Agent using Memory
for {
  memory <- memoryService
  storedData <- memory.retrieve("key")
  result <- processData(storedData)
} yield result
```

### 2. Message Passing

For distributed communication, components use the Mesh module's message passing system:

```scala
// Example: Agent sending message to another agent
for {
  mesh <- meshService
  response <- mesh.sendMessage(destinationAgent, message)
} yield response
```

### 3. Event-Based Communication

For loose coupling, components can publish and subscribe to events:

```scala
// Example: Publishing an event
for {
  _ <- eventBus.publish(AgentStartedEvent(agentId))
} yield ()

// Example: Subscribing to events
val subscription = eventBus.subscribe(AgentEvents.Started).foreach { event =>
  logAgentStarted(event.agentId)
}
```

### 4. HTTP Communication

For external systems, components use the HTTP module:

```scala
// Example: HTTP endpoint receiving external request
val route = Routes.post("/agent/:id") { (req, params) =>
  for {
    agentId <- ZIO.succeed(params("id"))
    message <- req.body.asString
    response <- agentService.processMessage(agentId, message)
  } yield Response.json(response)
}
```

## Extension Points

ZIO Agentic AI Framework provides several extension points for customization:

### 1. Agent Capabilities

The capability system allows extending agent functionality:

```mermaid
graph TD
    A[Agent Core] --> B[Base Capability]
    B --> C[Memory Capability]
    B --> D[LLM Capability]
    B --> E[Tool Capability]
    B --> F[HTTP Capability]
    B --> G[Custom Capability]
    
    classDef custom fill:#bfb,stroke:#333,stroke-width:1px;
    class G custom;
```

### 2. Memory Storage Backends

The Memory module supports pluggable storage backends:

```mermaid
graph TD
    A[Memory Manager] --> B[Storage Interface]
    B --> C[In-Memory Storage]
    B --> D[File Storage]
    B --> E[Database Storage]
    B --> F[Custom Storage]
    
    classDef custom fill:#bfb,stroke:#333,stroke-width:1px;
    class F custom;
```

### 3. LLM Providers

The Langchain4j module supports multiple LLM providers:

```mermaid
graph TD
    A[LLM Service] --> B[Provider Interface]
    B --> C[OpenAI Provider]
    B --> D[Anthropic Provider]
    B --> E[VertexAI Provider]
    B --> F[Custom Provider]
    
    classDef custom fill:#bfb,stroke:#333,stroke-width:1px;
    class F custom;
```

## Runtime Interactions

The following sequence diagram illustrates a typical runtime interaction between components:

```mermaid
sequenceDiagram
    participant User
    participant HTTP as HTTP Server
    participant Agent as Agent System
    participant LLM as Langchain4j
    participant Mem as Memory
    participant Provider as LLM Provider
    
    User->>HTTP: HTTP Request
    HTTP->>Agent: Process Message
    
    Agent->>Mem: Retrieve Context
    Mem-->>Agent: Context Data
    
    Agent->>LLM: Generate Response
    LLM->>Provider: API Request
    Provider-->>LLM: API Response
    LLM-->>Agent: Generated Response
    
    Agent->>Mem: Store Interaction
    Mem-->>Agent: Confirmation
    
    Agent-->>HTTP: Agent Response
    HTTP-->>User: HTTP Response
```

This sequence shows:

1. A user sends a request to the HTTP server
2. The HTTP server forwards the message to the appropriate agent
3. The agent retrieves context from memory
4. The agent uses Langchain4j to generate a response via an LLM provider
5. The agent stores the interaction in memory
6. The response is sent back through the HTTP server to the user

These interactions demonstrate the modular nature of the ROO framework, with clear boundaries between components and well-defined communication patterns.