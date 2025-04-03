# Web Agent Dashboard Example

## Overview

The Web Agent Dashboard is a comprehensive example that demonstrates the capabilities of the Agentic AI Framework in a web-based environment. It provides a visual interface for interacting with multiple AI agents, inspecting their memory, and observing their internal state and processing.

## Features

- **Multi-Agent Orchestration**: Coordinate multiple specialized agents that collaborate on tasks
- **Memory Visualization**: Real-time visualization of agent memory cells and content
- **State Inspection**: Observe agent state transitions and decision-making processes
- **Debug Tools**: Interactive tools for debugging agent behavior
- **Documentation Integration**: Context-sensitive documentation available within the UI

## Architecture

The example consists of several key components:

### Backend Components

1. **Agent Ecosystem**
   - Task Agent: Manages high-level task coordination
   - Knowledge Agent: Retrieves and processes information
   - Memory Agent: Manages the persistent memory system
   - Planning Agent: Handles task decomposition and execution planning

2. **Memory System**
   - In-memory and persistent storage
   - Visual memory cell inspection
   - Memory compression and optimization statistics

3. **HTTP API Layer**
   - RESTful endpoints for agent interaction
   - WebSocket for real-time updates
   - SSE (Server-Sent Events) for state monitoring

### Frontend Components

1. **Dashboard UI**
   - Agent interaction panels
   - Memory visualization
   - State inspection tools
   - Documentation panel

2. **Visualization Tools**
   - Memory cell graph visualization
   - Agent state transition diagrams
   - Task execution flow charts

## Implementation Plan

### Step 1: Core Backend Services

```scala
// TaskCoordinatorAgent implementation
class TaskCoordinatorAgent extends BaseAgent[TaskRequest, TaskResponse] {
  private val memory = MemorySystem.make

  override protected def processMessage(request: TaskRequest): ZStream[Any, Throwable, TaskResponse] = {
    for {
      // Store request in memory
      _ <- ZIO.fromEffect(memory.createCellWithTags(request, Set("request", request.id)))
      
      // Task decomposition
      subtasks <- decomposeTask(request).flatMap(ZStream.fromIterable)
      
      // Process subtasks in parallel with the ParallelProcessor
      results <- processSubtasks(subtasks).flatMap(ZStream.fromIterable)
      
      // Consolidate results
      response = consolidateResults(results)
      
      // Store response in memory
      _ <- ZIO.fromEffect(memory.createCellWithTags(response, Set("response", request.id)))
    } yield response
  }
  
  // Helper methods for task processing
  private def decomposeTask(request: TaskRequest): ZIO[Any, Throwable, List[Subtask]] = ???
  private def processSubtasks(subtasks: List[Subtask]): ZIO[Any, Throwable, List[SubtaskResult]] = ???
  private def consolidateResults(results: List[SubtaskResult]): TaskResponse = ???
}
```

### Step 2: HTTP API Layer

```scala
// HTTP API for agent interaction using zio-http
object AgentApi extends ZIOAppDefault {
  def run = {
    val app = Routes(
      // Agent interaction endpoints
      Method.POST / "api" / "agents" / "task" -> handler { (req: Request) =>
        for {
          taskRequest <- req.body.asString.map(decode[TaskRequest])
          agent = new TaskCoordinatorAgent()
          response <- agent.process(taskRequest).runCollect.map(_.head)
        } yield Response.json(encode(response))
      },
      
      // Memory inspection endpoints
      Method.GET / "api" / "memory" / "cells" -> handler { (_: Request) =>
        for {
          memorySystem <- ZIO.service[MemorySystem]
          cells <- memorySystem.getAllCells
          cellData <- ZIO.foreach(cells.toList)(cell => 
            for {
              value <- cell.read
              meta <- cell.metadata
            } yield CellData(meta.id, value.toString, meta)
          )
        } yield Response.json(encode(cellData))
      }
    )
    
    // Configure and start the server
    Server.start(8080, app)
  }
}
```

### Step 3: Frontend Dashboard (Scala.js)

```scala
// Dashboard UI component using Scala.js
@JSExportTopLevel("AgentDashboard")
object AgentDashboard {
  def main(args: Array[String]): Unit = {
    val dashboard = new Dashboard()
    dashboard.render("#app-container")
  }
}

class Dashboard {
  // UI components
  private val agentPanel = new AgentPanel()
  private val memoryVisualizer = new MemoryVisualizer()
  private val stateInspector = new StateInspector()
  private val docsPanel = new DocumentationPanel()
  
  // WebSocket for real-time updates
  private val ws = new WebSocket("ws://localhost:8080/ws")
  
  def render(selector: String): Unit = {
    // Set up event listeners
    ws.onmessage = { event =>
      val update = decode[AgentUpdate](event.data.toString)
      updateUI(update)
    }
    
    // Render UI components
    val container = document.querySelector(selector)
    container.appendChild(agentPanel.render())
    container.appendChild(memoryVisualizer.render())
    container.appendChild(stateInspector.render())
    container.appendChild(docsPanel.render())
  }
  
  private def updateUI(update: AgentUpdate): Unit = {
    // Update UI components with new data
    agentPanel.update(update.agentState)
    memoryVisualizer.update(update.memoryCells)
    stateInspector.update(update.stateTransitions)
    docsPanel.update(update.context)
  }
}
```

## User Experience

The dashboard provides a rich, interactive experience:

1. **Task Creation**: Users can submit tasks through a form interface
2. **Real-time Monitoring**: Observe agents working in real-time with visual indicators of activity
3. **Memory Inspection**: Explore memory cells, their connections, and content
4. **State Debugging**: Analyze agent state transitions and decision points
5. **Documentation Access**: Access context-sensitive documentation for any aspect of the system

## Memory Visualization

The memory visualization component provides:

- Graph visualization of memory cells and their relationships
- Cell content inspection
- Memory usage statistics and optimization metrics
- Time-based memory evolution tracking

## Technical Requirements

- Scala 3.3.1 or higher
- ZIO 2.0.19 or higher
- ZIO HTTP for the API layer
- Scala.js for the frontend
- D3.js for interactive visualizations

## Running the Example

```bash
# Start the backend server
sbt "runMain com.agenticai.examples.webdashboard.AgentServer"

# Build the frontend
sbt webUI/fastOptJS

# Open in browser
open http://localhost:8080
```

## Extension Points

The example can be extended in several ways:

1. **New Agent Types**: Add specialized agents for different domains
2. **Alternative UIs**: Implement different visualization approaches
3. **Integration with External Services**: Connect agents to external APIs and data sources
4. **Custom Metrics**: Add domain-specific metrics and monitoring