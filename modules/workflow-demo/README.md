# Workflow Demo

## Overview

The Workflow Demo showcases the Agentic AI Framework's ability to connect specialized agents in a workflow to perform complex tasks. This demo illustrates how agents can be orchestrated to process text, summarize content, and execute build operations.

## 🚨 Important Notice: ZIO HTTP Compatibility Issue

There is currently a compatibility issue with ZIO HTTP 3.0.0-RC2. The server code may not compile due to API changes. As a workaround, you can access the UI directly without the backend server.

## Quick Start

### Option 1: Direct File Access (Recommended)

1. Open the local test HTML file directly in your browser:
   ```
   file:///Users/E74823/projects/agentic-ai-framework/modules/workflow-demo/src/main/resources/public/local-test.html
   ```

2. This version uses mock data to simulate workflow execution without requiring the backend server.

### Option 2: Run the Server (May Not Work)

1. Run the workflow demo server:
   ```
   ./modules/workflow-demo/run-workflow-demo.sh
   ```

2. Access the UI at: `http://localhost:8083/`

## Documentation

Comprehensive documentation is available in the `docs` directory:

- [Complete Documentation](docs/WorkflowDemo_Documentation.md)
- [Troubleshooting Guide](docs/WorkflowDemo_TroubleshootingGuide.md)

## Architecture

The Workflow Demo consists of:

1. **Frontend Interface**: A React application for creating and visualizing workflows
2. **Backend Server**: A Scala/ZIO application providing REST API endpoints
3. **Agent Components**: Specialized agents for text transformation, summarization, etc.
4. **Workflow Engine**: Coordinates agent execution according to defined workflows

## Available Agent Types

- **TextTransformerAgent**: Transforms text (capitalization, etc.)
- **TextSplitterAgent**: Splits text into manageable chunks
- **SummarizationAgent**: Creates summaries of text input
- **BuildAgent**: Simulates build processes

## REST API Endpoints (When Server is Running)

| Endpoint | Method | Description |
|----------|--------|-------------|
| `/api/workflow/execute` | POST | Execute a workflow |
| `/api/workflow/status/:id` | GET | Get status of a workflow execution |
| `/api/workflow/result/:id` | GET | Get the result of a completed workflow |
| `/api/workflow/progress/:id` | GET | Get the progress percentage of a workflow |
| `/api/workflow/cancel/:id` | POST | Cancel a running workflow |

## Troubleshooting

If you encounter the error "Error: Failed to fetch" when running a workflow, it typically means:

1. The workflow REST API server is not running
2. There's a CORS issue preventing the frontend from connecting to the backend
3. The frontend is using incorrect API endpoints
4. The ZIO HTTP server has compatibility issues (current scenario)

Refer to the [Troubleshooting Guide](docs/WorkflowDemo_TroubleshootingGuide.md) for detailed solutions.