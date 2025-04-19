# Workflow Demo

## Overview

The Workflow Demo showcases the Agentic AI Framework's ability to connect specialized agents in a workflow to perform complex tasks. This demo illustrates how agents can be orchestrated to process text, summarize content, and execute build operations.

## 🚨 Important Notice: ZIO HTTP Compatibility

This demo uses ZIO HTTP 3.0.0-RC4, which should resolve previous compatibility issues with RC2. If you encounter any issues with the server, you can still access the UI directly without the backend server as a workaround.

## Quick Start

### Option 1: Direct File Access (Recommended)

1. Run the workflow demo server first to see the correct file path:
   ```
   ./modules/workflow-demo/run-workflow-demo.sh
   ```

2. Open the local test HTML file directly in your browser using the path shown in the server startup message.

3. This version uses mock data to simulate workflow execution without requiring the backend server.

### Option 2: Access Through Server Web Interface (May Not Work)

1. Run the workflow demo server (if not already running):
   ```
   ./modules/workflow-demo/run-workflow-demo.sh
   ```

2. Instead of opening the HTML file directly, access the UI through the server at: `http://localhost:8083/`

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
3. The frontend is using incorrect API endpoints or port (make sure it's using port 8083)
4. The ZIO HTTP server has compatibility issues (should be resolved with RC4, but might still occur)

Refer to the [Troubleshooting Guide](docs/WorkflowDemo_TroubleshootingGuide.md) for detailed solutions.
