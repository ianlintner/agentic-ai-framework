#!/bin/bash

# run-dashboard.sh - Launcher script for the Agentic AI Dashboard
# 
# This script:
# - Compiles the dashboard module
# - Starts the ZIO HTTP server
# - Provides clear error reporting and exit codes

# Exit on any error
set -e

# Script location
SCRIPT_DIR=$(dirname "$(realpath "$0")")
cd "$SCRIPT_DIR"

echo "===== Agentic AI Dashboard Launcher ====="
echo "Starting compilation process..."

# Compile the project
if ! sbt compile; then
  echo "Error: Compilation failed!"
  exit 1
fi

echo "Compilation successful!"
echo

echo "Starting the Dashboard HTTP server..."
echo "The web interface will be available at http://localhost:8081"
echo "Press Ctrl+C to stop the server"
echo

# Run the application with explicit main class
if ! sbt "runMain com.agenticai.dashboard.DashboardLauncher"; then
  echo "Error: Failed to start the server!"
  exit 2
fi

# This part will only execute if the server is stopped normally
echo "Server stopped."