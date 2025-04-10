# Workflow Example

## Overview

This example demonstrates how to orchestrate specialized agents in a workflow to perform complex tasks. It showcases the core concepts of the Agentic AI Framework's agent composition capabilities.

## Key Features

- **Agent Composition**: Specialized agents connected in a directed workflow
- **Data Transformation Pipeline**: Sequential processing of text through multiple agent types
- **Flexible Workflow Definition**: Modular workflow configuration with explicit connections

## Agent Types

This example includes several specialized agent types:

- **TextTransformerAgent**: Transforms text (capitalization, formatting)
- **TextSplitterAgent**: Splits text into manageable chunks
- **SummarizationAgent**: Summarizes text content
- **BuildAgent**: Simulates a build process (example of an action agent)

## Architecture

The workflow example follows a simple but powerful architecture:

1. **Agent Interface**: Common interface for all agents, represented by the `Agent[-I, +O]` trait
2. **Agent Implementations**: Specialized agent implementations for specific tasks
3. **Workflow Model**: Data structures representing workflow nodes and connections
4. **Workflow Engine**: Runtime for executing workflows and managing data flow
5. **HTTP Server**: Simple server for exposing the workflow via REST API

## Implementation Status

This is a functional example with simulated agent behavior. In a production implementation:

- Summarization would use a real LLM like Claude or GPT
- Build processes would connect to real CI/CD systems
- HTTP server would use a production-ready library (current ZIO HTTP compatibility issue)

## Running the Example

From the project root directory:

```bash
sbt "examples/runMain com.agenticai.examples.workflow.WorkflowExampleRunner"
```

The example provides two modes:
1. Console mode: Runs the workflow and shows output in the terminal
2. HTTP Server mode: Runs a simulated HTTP server

## Code Organization

- `agent/` - Agent implementations
- `model/` - Data models for workflow components
- `engine/` - Workflow execution engine
- `server/` - Simple HTTP server (simulated)
- `WorkflowExampleRunner.scala` - Main entry point

## Implementation Notes

- This example uses a simplified version of ZIO HTTP due to compatibility issues
- The agents demonstrate the pattern for creating specialized, composable behaviors
- The workflow engine shows how to manage execution flow through a directed graph

## Relationship to Agent Mesh

This example demonstrates a workflow within a single process. For distributed workflows across multiple processes or machines, see the Mesh examples which extend these concepts to networked environments.