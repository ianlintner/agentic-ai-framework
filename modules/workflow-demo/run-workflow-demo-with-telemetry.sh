#!/bin/bash

# Script to run the workflow demo with telemetry monitoring
# This script starts the monitoring stack (Prometheus, Jaeger, Grafana),
# runs the workflow demo, and provides links to access the telemetry dashboards.

set -e  # Exit on error

# Configuration
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
TELEMETRY_DIR="$PROJECT_ROOT/telemetry"
WORKFLOW_DEMO_DIR="$PROJECT_ROOT/workflow-demo"
DOCKER_COMPOSE_FILE="$TELEMETRY_DIR/docker-compose.yml"

# Color output helpers
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

print_header() {
  echo -e "\n${GREEN}$1${NC}\n"
}

print_info() {
  echo -e "${BLUE}$1${NC}"
}

print_warning() {
  echo -e "${YELLOW}$1${NC}"
}

# Check if Docker is running
if ! docker info > /dev/null 2>&1; then
  echo "Error: Docker is not running or not installed."
  echo "Please start Docker and try again."
  exit 1
fi

# Start the monitoring stack
print_header "Starting monitoring stack (Prometheus, Jaeger, Grafana)..."
cd "$TELEMETRY_DIR"
docker-compose -f "$DOCKER_COMPOSE_FILE" down --remove-orphans > /dev/null 2>&1 || true
docker-compose -f "$DOCKER_COMPOSE_FILE" up -d

# Wait for services to become available
print_info "Waiting for monitoring services to start..."
sleep 5

# Run the workflow demo with telemetry enabled
print_header "Starting the workflow demo with telemetry enabled..."
cd "$WORKFLOW_DEMO_DIR"

# Set environment variables to enable telemetry
export ENABLE_TELEMETRY=true
export OTEL_EXPORTER_OTLP_ENDPOINT="http://localhost:4317"
export OTEL_SERVICE_NAME="workflow-demo"

# Run the workflow demo
sbt "runMain com.agenticai.workflow.WorkflowDemoLauncher" &
WORKFLOW_PID=$!

# Display information about accessing dashboards
print_header "Telemetry Dashboards"
print_info "Workflow Demo:      http://localhost:8080"
print_info "Grafana Dashboard:  http://localhost:3000 (default login: admin/admin)"
print_info "Prometheus:         http://localhost:9090"
print_info "Jaeger UI:          http://localhost:16686"

print_warning "\nPress Ctrl+C to stop all services when done"

# Wait for Ctrl+C
trap 'kill $WORKFLOW_PID; cd "$TELEMETRY_DIR" && docker-compose -f "$DOCKER_COMPOSE_FILE" down; echo -e "\n${GREEN}All services stopped.${NC}"' EXIT
wait $WORKFLOW_PID