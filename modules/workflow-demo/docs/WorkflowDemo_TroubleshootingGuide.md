# Workflow Demo Troubleshooting Guide

## Overview

The Workflow Demo is a React application that connects to a REST API backend. The demo showcases how specialized agents can be connected in a workflow to perform complex tasks like text processing, summarization, and build operations.

## Common Issues

### "Error: Failed to fetch"

This error typically occurs when:

1. The workflow REST API server is not running
2. There's a CORS issue preventing the frontend from connecting to the backend
3. The frontend is using incorrect API endpoints
4. The ZIO HTTP server has compatibility issues (current scenario)

## Running the Demo

### Option 1: Direct File Access (Current Workaround)

Due to compatibility issues with ZIO HTTP 3.0.0-RC2, you can access the demo UI directly:

1. Open `modules/workflow-demo/src/main/resources/public/local-test.html` directly in your browser:
   ```
   file:///Users/E74823/projects/agentic-ai-framework/modules/workflow-demo/src/main/resources/public/local-test.html
   ```

2. This version uses mock data to simulate the workflow execution without requiring the backend server.

### Option 2: When Server is Fixed

Once the ZIO HTTP compatibility issues are resolved:

1. Run the workflow demo server:
   ```
   ./modules/workflow-demo/run-workflow-demo.sh
   ```

2. Open your browser to http://localhost:8083/

3. The UI should connect to the API server running on port 8080.

## API Endpoints (For Reference)

The workflow demo REST API should provide these endpoints:

- `POST /api/workflow/execute` - Execute a new workflow
- `GET /api/workflow/status/:id` - Get workflow status
- `GET /api/workflow/result/:id` - Get workflow result
- `GET /api/workflow/progress/:id` - Get workflow progress
- `POST /api/workflow/cancel/:id` - Cancel workflow

## Resolving "Failed to fetch" Errors

1. **Check if the server is running:**
   - Look for "Starting Workflow Demo API Server on port 8080..." message
   - Verify there are no compilation errors when starting the server

2. **Check CORS configuration:**
   - The server should have appropriate CORS headers:
     ```
     Access-Control-Allow-Origin: *
     Access-Control-Allow-Methods: GET, POST, PUT, DELETE, OPTIONS
     Access-Control-Allow-Headers: Content-Type, Authorization
     ```

3. **Verify API endpoints in the frontend:**
   - Open `modules/workflow-demo/src/main/resources/public/workflow.js`
   - Ensure API calls use the correct base URL (e.g., `http://localhost:8080/api/workflow/...`)

4. **Use the local test version:**
   - When server issues persist, use the local-test.html version which works without a backend server

## Current Status and Known Issues

- The ZIO HTTP 3.0.0-RC2 has compatibility issues that prevent the server from compiling and running
- As a workaround, we've created a placeholder server implementation that explains how to access the UI directly
- Future work will include updating the HTTP server to be compatible with the latest ZIO HTTP version

## Additional Debugging Tips

1. Check the browser console for detailed error messages
2. Use browser network tools to inspect API requests and responses
3. Compare server logs with client-side errors to identify mismatches
4. For local testing without backend, use the local-test.html file which includes mock data