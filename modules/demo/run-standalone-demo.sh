#!/bin/bash

# Run the Scala 3.3.1 ZIO-based Claude 3.7 Vertex AI demo
# This script compiles and runs the standalone demo directly
# without requiring the rest of the project to compile

echo "=== Building and Running Standalone Claude 3.7 Demo ==="

# Change to the demo module directory
cd "$(dirname "$0")"

# Set environment variables if needed
export GOOGLE_CLOUD_PROJECT=${GOOGLE_CLOUD_PROJECT:-"your-gcp-project-id"}

# Make sure we're using Maven Central
export SBT_OPTS="-Xms512m -Xmx2g -Dsbt.override.build.repos=true"
export JAVA_OPTS="$JAVA_OPTS -Dmaven.wagon.http.ssl.insecure=true -Dmaven.wagon.http.ssl.allowall=true -Dsbt.repository.config=project/repositories"

# Compile and run the simplified demo with verbose output to show what's happening
echo "Running Scala 3.3.1 ZIO-based Claude 3.7 demo..."

# Display dependency resolution details to help debugging
export SBT_OPTS="$SBT_OPTS -Dsbt.log.noformat=true -Dsbt.boot.directory=./target/boot"

# Clean, update dependencies explicitly, then compile
sbt clean update
sbt compile
sbt "runMain com.agenticai.demo.VertexAIClaudeDemo"

echo "=== Demo Complete ==="