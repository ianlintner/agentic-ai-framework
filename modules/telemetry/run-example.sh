#!/bin/bash

# Script to run the telemetry exporter example

cd "$(dirname "$0")"

# Check if monitoring stack is running
if ! docker ps | grep -q prometheus; then
  echo "Monitoring stack is not running. Starting it now..."
  ./run-monitoring.sh
  
  # Give the stack a moment to start up
  echo "Waiting for monitoring stack to initialize..."
  sleep 10
fi

# Run the telemetry example
echo "Running the telemetry exporter example..."
cd ../..
sbt "project agentic-telemetry" "runMain com.agenticai.telemetry.examples.TelemetryExporterExample"

echo ""
echo "You can view metrics and traces at:"
echo "- Grafana: http://localhost:3000 (admin/admin)"
echo "- Jaeger: http://localhost:16686"
echo "- Prometheus: http://localhost:9090"