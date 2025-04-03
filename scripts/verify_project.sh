#!/bin/bash

# Agentic AI Framework - Project Verification Script
# This script verifies that the project follows the rules defined in .roorules

echo "=== Agentic AI Framework - Project Verification ==="
echo "Checking project according to Roo Rules..."

# Check that goals.md exists
if [ ! -f "goals.md" ]; then
  echo "❌ goals.md file not found! This file is required to define project goals."
  exit 1
else
  echo "✅ goals.md found"
fi

# Check if project compiles
echo "Running sbt compile..."
if ! sbt compile; then
  echo "❌ Project compilation failed! Fix compilation errors before proceeding."
  exit 1
else
  echo "✅ Project compiles successfully"
fi

# Run unit tests
echo "Running unit tests..."
if ! sbt test; then
  echo "❌ Unit tests failed! Fix test failures before proceeding."
  exit 1
else
  echo "✅ All unit tests pass"
fi

# Check code style and structure
echo "Checking code style..."
if [ -x "$(command -v scalafmt)" ]; then
  if ! scalafmt --check; then
    echo "❌ Code style check failed! Run 'scalafmt' to fix formatting issues."
    exit 1
  else
    echo "✅ Code style checks pass"
  fi
else
  echo "⚠️ scalafmt not found, skipping code style check"
fi

# Verify documentation is up to date
echo "Checking documentation..."
docs_count=$(find docs -type f -name "*.md" | wc -l)
if [ "$docs_count" -lt 5 ]; then
  echo "⚠️ Less than 5 documentation files found. Consider adding more documentation."
else
  echo "✅ Documentation looks good ($docs_count files found)"
fi

# Check for the presence of tests
echo "Checking for tests presence..."
test_count=$(find src/test -type f -name "*.scala" | wc -l)
if [ "$test_count" -lt 10 ]; then
  echo "⚠️ Less than 10 test files found. Consider adding more tests."
else
  echo "✅ Tests look good ($test_count files found)"
fi

# Optionally run integration tests if requested
if [ "$1" == "--with-integration" ]; then
  echo "Running integration tests..."
  if ! sbt integrationTest; then
    echo "❌ Integration tests failed!"
    exit 1
  else
    echo "✅ Integration tests pass"
  fi
fi

# Optionally check Vertex AI connection if requested
if [ "$1" == "--with-vertex" ]; then
  echo "Testing Vertex AI connection..."
  if ! sbt testVertexConnection; then
    echo "❌ Vertex AI connection test failed!"
    exit 1
  else
    echo "✅ Vertex AI connection verified"
  fi
fi

# Verify README exists and contains essential information
if [ ! -f "README.md" ]; then
  echo "❌ README.md file not found! This file is required for project documentation."
  exit 1
else
  readme_size=$(wc -l < README.md)
  if [ "$readme_size" -lt 20 ]; then
    echo "⚠️ README.md seems too short. Consider adding more information."
  else
    echo "✅ README.md found and has sufficient content"
  fi
fi

# Verify project structure follows conventions
if [ ! -d "src/main/scala/com/agenticai" ]; then
  echo "❌ Expected project structure not found! Check directory organization."
  exit 1
else
  echo "✅ Project structure follows conventions"
fi

# Final result
echo ""
echo "=== Verification Complete ==="
echo "Project appears to be in good standing according to Roo Rules."
echo "Make sure to check goals.md before starting any new task."
echo ""
echo "To run with integration tests: ./scripts/verify_project.sh --with-integration"
echo "To check Vertex AI connection: ./scripts/verify_project.sh --with-vertex"
echo "To run all checks: ./scripts/verify_project.sh --all"