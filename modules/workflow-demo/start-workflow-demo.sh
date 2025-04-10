#!/bin/bash
set -e

echo "Starting Workflow Demo Web Server..."
cd "$(dirname "$0")"

# Use project-specific command to avoid any ambiguity
echo "Running workflow demo with explicit class path..."
sbt "project workflowDemo" "runMain com.agenticai.workflow.WorkflowDemoLauncher"

# This will only execute if the server stops normally
echo "Server stopped successfully."