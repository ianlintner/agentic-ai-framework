# Workflow Demo Documentation

## Introduction

The Workflow Demo showcases the Agentic AI Framework's ability to connect specialized agents in a workflow to perform complex tasks. This demo illustrates how different agent types can be orchestrated to process text, summarize content, and execute build operations in a pipeline fashion.

## Architecture

The Workflow Demo consists of:

1. **Frontend Interface**: A React application that allows users to:
   - Create and visualize workflows
   - Execute workflows and monitor progress
   - View results of workflow execution

2. **Backend Server**: A Scala/ZIO application that:
   - Provides REST API endpoints for workflow operations
   - Manages workflow execution and status tracking
   - Orchestrates agent communication

3. **Agent Components**:
   - **TextTransformerAgent**: Transforms text (e.g., capitalization, sentence case)
   - **TextSplitterAgent**: Splits text into chunks
   - **SummarizationAgent**: Creates summaries of text input
   - **BuildAgent**: Simulates build processes

4. **Workflow Engine**: Coordinates the execution of agents according to defined workflows

## Component Interactions

```
┌─────────────┐     HTTP     ┌───────────────┐
│   Frontend  │ ────────────►│ Backend Server│
│  React App  │◄────────────┐│   (ZIO HTTP)  │
└─────────────┘     JSON    └───────┬────────┘
                               ┌────▼────┐
                               │ Workflow │
                               │  Engine  │
                               └────┬────┘
                                    │
                   ┌────────────────┼────────────────┐
                   │                │                │
              ┌────▼────┐      ┌────▼────┐      ┌────▼────┐
              │  Text   │      │  Text   │      │  Build  │
              │Transformer◄─────┤ Splitter◄──────┤  Agent  │
              │  Agent  │      │  Agent  │      │         │
              └─────────┘      └─────────┘      └─────────┘
```

## Workflow Model

The workflow is modeled as a directed graph:

- **Nodes**: Represent agent instances with specific configurations
- **Connections**: Define data flow between agents
- **Workflow**: Contains metadata and the complete node/connection graph

Example workflow definition:
```scala
Workflow(
  id = UUID.randomUUID().toString,
  name = "Text Processing and Build Demo",
  description = "A demo workflow that transforms, summarizes text, and performs build operations",
  nodes = List(
    WorkflowNode(
      id = "node-1",
      nodeType = "text-transformer",
      label = "Capitalize Text",
      configuration = Map("transform" -> "capitalize"),
      position = NodePosition(150, 100)
    ),
    // Additional nodes...
  ),
  connections = List(
    NodeConnection(
      id = "conn-1",
      sourceNodeId = "node-1",
      targetNodeId = "node-2"
    ),
    // Additional connections...
  )
)
```

## Using the Workflow Demo

### Running the Demo

1. Start the workflow demo server:
   ```
   ./modules/workflow-demo/run-workflow-demo.sh
   ```

2. Access the UI:
   - **If server is running**: Open http://localhost:8083/ in your browser
   - **If server has issues**: Open the file directly:
     ```
     file:///Users/E74823/projects/agentic-ai-framework/modules/workflow-demo/src/main/resources/public/local-test.html
     ```

### Creating Workflows

The UI allows you to:

1. Add agent nodes by selecting from available agent types
2. Configure each node with specific parameters
3. Connect nodes by drawing connections between them
4. Save workflows for later execution

### Executing Workflows

1. Click "Execute" on a workflow
2. Monitor progress in real-time through the UI
3. View the final output when execution completes

## API Endpoints

The workflow server provides these REST API endpoints:

| Endpoint | Method | Description |
|----------|--------|-------------|
| `/api/workflow/execute` | POST | Execute a workflow |
| `/api/workflow/status/:id` | GET | Get status of a workflow execution |
| `/api/workflow/result/:id` | GET | Get the result of a completed workflow |
| `/api/workflow/progress/:id` | GET | Get the progress percentage of a workflow |
| `/api/workflow/cancel/:id` | POST | Cancel a running workflow |

## Agent Types

### TextTransformerAgent

Transforms text according to configured rules:
- **capitalize**: Converts text to UPPERCASE
- **lowercase**: Converts text to lowercase
- **sentencecase**: Capitalizes the first letter of each sentence

### TextSplitterAgent

Splits text into chunks with configurable parameters:
- **chunkSize**: Number of characters per chunk
- **overlap**: Number of overlapping characters between chunks

### SummarizationAgent

Creates summaries of input text:
- **ratio**: The ratio of the summary length to the original text (e.g., 0.2 for 20%)
- **maxLength**: Maximum summary length in characters

### BuildAgent

Simulates build operations:
- **target**: The build target (e.g., "debug", "release")
- **platform**: The target platform (e.g., "linux", "macos", "windows")

## Extending the Demo

The Workflow Demo can be extended in several ways:

1. **Add new agent types**:
   - Create a new class implementing the `Agent` trait
   - Add the agent factory to the `WorkflowEngine`

2. **Enhance the UI**:
   - Modify the React components in `modules/workflow-demo/src/main/resources/public/`
   - Add visualizations for workflow execution

3. **Improve the backend**:
   - Add persistence for workflows
   - Implement authentication and multi-user support
   - Add WebSocket support for real-time updates

## Technical Notes

- The frontend uses vanilla JavaScript with minimal dependencies
- The backend is built on ZIO and ZIO HTTP
- Agent implementations are currently simplified for demo purposes
- In a production system, agents could be distributed across multiple machines

## Troubleshooting

For issues and solutions, refer to the [Troubleshooting Guide](WorkflowDemo_TroubleshootingGuide.md).