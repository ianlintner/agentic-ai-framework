# Web Agent Dashboard Example

## Overview

The Web Agent Dashboard example demonstrates how to build a web-based monitoring and interaction interface for AI agents built with the Agentic AI Framework. It showcases how to visualize agent activity, memory, and task processing in real-time.

## Key Features

- **Task Management System**: Submit, track, and visualize tasks processed by agents
- **Memory Visualization**: Inspect the contents and relationships of agent memory cells
- **Agent State Monitoring**: Observe agent status and processing in real-time
- **Interactive Dashboard**: User-friendly web interface for interacting with the agent system
- **Debug Data Visualization**: Tools for debugging and understanding agent behavior

## Architecture

The example is organized into several components:

### Backend Components
1. **`TaskProcessorAgent`**: Agent that processes task requests by breaking them into subtasks
2. **`AgentAPI`**: API layer that exposes agent functionality to the web interface
3. **`TaskModels`**: Data models representing tasks, subtasks, and results

### Frontend Components
1. **HTML/CSS Interface**: Clean, responsive dashboard layout
2. **Interactive Visualizations**: D3.js-based visualizations of memory and tasks
3. **Real-time Updates**: WebSocket-based live updates of agent activity

## Getting Started

### Prerequisites
- Scala 3.3.1+
- SBT
- Modern web browser

### Running the Example

1. Start the backend:
```bash
sbt "runMain com.agenticai.examples.webdashboard.api.AgentAPI"
```

2. Open your browser and navigate to:
```
http://localhost:8080
```

3. Use the dashboard to submit and monitor tasks

## Exploring the Codebase

### Key Files

- **`models/TaskModels.scala`**: Core data models
- **`models/Task.scala`**: Extended task model with utility methods
- **`agents/TaskProcessorAgent.scala`**: Agent implementation
- **`api/AgentAPI.scala`**: API layer and mock HTTP server
- **`resources/public/index.html`**: Dashboard HTML structure
- **`resources/public/assets/css/styles.css`**: Dashboard styling
- **`resources/public/assets/js/dashboard.js`**: Dashboard functionality

## Key Concepts Demonstrated

### Agent Composition
The example shows how to build a task processing agent that:
- Breaks down complex tasks into manageable subtasks
- Processes subtasks efficiently
- Aggregates and summarizes results

### Memory System Integration
The dashboard visualizes the agent's memory system:
- Memory cells displayed as an interactive graph
- Memory contents and metadata inspection
- Relationships between memory cells

### Streaming and Real-time Updates
The example leverages ZIO streams for:
- Processing tasks as streams of data
- Providing real-time updates through WebSockets
- Reactive user interface updates

## Debugging Features

The dashboard includes several features to aid debugging:

### Memory Inspector
- View the contents of memory cells
- Filter memory cells by tags
- Visualize relationships between cells

### Task Debugging
- Observe the decomposition of tasks into subtasks
- Track the progress and status of each subtask
- Review the final aggregated results

### Log Viewer
- Real-time log display
- Filtering by log level and source
- Chronological event tracking

## Extending the Example

### Adding New Agent Types

Create new agent classes that extend `BaseAgent`:

```scala
class SpecializedAgent extends BaseAgent[SpecializedRequest, SpecializedResponse] {
  override protected def processMessage(request: SpecializedRequest): ZStream[Any, Throwable, SpecializedResponse] = {
    // Specialized processing logic
    ZStream.succeed(...)
  }
}
```

### Enhancing Visualizations

The D3.js-based visualizations can be extended:

1. Add new visualization components in `dashboard.js`
2. Provide additional data endpoints in `AgentAPI.scala`
3. Update the dashboard layout in `index.html`

### Implementing Persistent Storage

Replace the in-memory storage with database integration:

1. Add database dependencies to `build.sbt`
2. Create database models and repositories
3. Update the API layer to use the repositories

## Debugging Data Ideas

Here are some ideas for enhancing the debugging data visualizations:

### Agent Decision Tree Visualization
- Visualize the decision-making process of agents
- Show branching paths and alternatives considered
- Highlight critical decision points

### Memory Evolution Timeline
- Track changes to memory cells over time
- Show when cells are created, updated, and accessed
- Visualize memory compression and optimization events

### Agent Communication Network
- Visualize communication between multiple agents
- Show message passing and coordination
- Track dependencies and bottlenecks

### Performance Metrics Dashboard
- Monitor processing time per task/subtask
- Track memory usage and efficiency
- Identify performance bottlenecks

## Best Practices

This example demonstrates several best practices:

1. **Separation of Concerns**: Clear separation between models, agents, and API
2. **Type-safe Design**: Leveraging Scala's type system for safety
3. **Functional Programming**: Pure functions and immutable data where possible
4. **Reactive Design**: Real-time updates and event-driven architecture
5. **Testability**: Components designed for easy testing

## Further Resources

- Check the main project README for framework documentation
- See the memory system documentation for details on memory cell functionality
- Review the agent system documentation for agent implementation guidelines