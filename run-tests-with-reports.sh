#!/bin/bash
# Script to run tests with detailed reports locally

# Set default values
ALL_MODULES=false
MODULES=""
SKIP_COVERAGE=false
HELP=false

# Parse command line arguments
while [[ $# -gt 0 ]]; do
  case $1 in
    --all)
      ALL_MODULES=true
      shift
      ;;
    --modules=*)
      MODULES="${1#*=}"
      shift
      ;;
    --skip-coverage)
      SKIP_COVERAGE=true
      shift
      ;;
    --help)
      HELP=true
      shift
      ;;
    *)
      echo "Unknown parameter: $1"
      HELP=true
      shift
      ;;
  esac
done

# Display help if requested or if no modules specified
if [ "$HELP" = true ] || ([ "$ALL_MODULES" = false ] && [ -z "$MODULES" ]); then
  echo "Usage: ./run-tests-with-reports.sh [OPTIONS]"
  echo
  echo "Options:"
  echo "  --all                Run tests for all modules"
  echo "  --modules=LIST       Run tests for specific modules (comma-separated)"
  echo "                       Example: --modules=core,mesh"
  echo "  --skip-coverage      Skip coverage reports (faster)"
  echo "  --help               Display this help message"
  echo
  echo "Example:"
  echo "  ./run-tests-with-reports.sh --modules=core,mesh"
  exit 0
fi

# Create reports directory
REPORTS_DIR="test-reports-$(date +%Y%m%d-%H%M%S)"
mkdir -p "$REPORTS_DIR"
mkdir -p "$REPORTS_DIR/test-html"
mkdir -p "$REPORTS_DIR/coverage"

echo "⚙️ Running tests with detailed reports..."
echo "📁 Reports will be saved to: $REPORTS_DIR"

# Determine which modules to test
if [ "$ALL_MODULES" = true ]; then
  echo "🧪 Testing all modules"
  TEST_COMMAND=""
else
  echo "🧪 Testing modules: $MODULES"
  IFS=',' read -ra MODULE_ARRAY <<< "$MODULES"
  TEST_COMMAND=""
  for module in "${MODULE_ARRAY[@]}"; do
    trimmed=$(echo "$module" | xargs)
    TEST_COMMAND="$TEST_COMMAND $trimmed/test"
  done
fi

# Run tests with or without coverage
if [ "$SKIP_COVERAGE" = true ]; then
  echo "⏩ Skipping coverage reports"
  if [ -z "$TEST_COMMAND" ]; then
    sbt "testOnly -- -h $REPORTS_DIR/test-html"
  else
    sbt "$TEST_COMMAND"
  fi
else
  echo "📊 Including coverage reports"
  if [ -z "$TEST_COMMAND" ]; then
    sbt coverage "testOnly -- -h $REPORTS_DIR/test-html" coverageReport
  else
    sbt coverage "$TEST_COMMAND" coverageReport
  fi
  
  # Copy coverage reports
  echo "📋 Collecting coverage reports..."
  find . -path "*/target/scala*/scoverage-report" -type d | while read -r dir; do
    module=$(echo "$dir" | sed -E 's/\.\/(.+)\/target.+/\1/')
    echo "  - Copying coverage report for $module"
    mkdir -p "$REPORTS_DIR/coverage/$module"
    cp -r "$dir"/* "$REPORTS_DIR/coverage/$module"/
  done
  
  # Generate coverage summary
  echo "📝 Creating coverage summary..."
  COVERAGE_SUMMARY="$REPORTS_DIR/coverage-summary.md"
  echo "# Coverage by Module" > "$COVERAGE_SUMMARY"
  echo "" >> "$COVERAGE_SUMMARY"
  
  find . -name "scoverage.coverage" | while read -r file; do
    module=$(echo "$file" | sed -E 's/\.\/(.+)\/target.+/\1/')
    
    # Extract coverage metrics
    if [ -f "$file" ]; then
      stmt_coverage=$(grep "statement coverage:" "$file" | awk '{print $3}')
      branch_coverage=$(grep "branch coverage:" "$file" | awk '{print $3}')
      
      echo "## $module" >> "$COVERAGE_SUMMARY"
      echo "- Statement Coverage: $stmt_coverage" >> "$COVERAGE_SUMMARY"
      echo "- Branch Coverage: $branch_coverage" >> "$COVERAGE_SUMMARY"
      echo "" >> "$COVERAGE_SUMMARY"
    fi
  done
fi

# Copy test reports
echo "📋 Collecting test reports..."
find . -path "*/target/test-reports" -type d | while read -r dir; do
  module=$(echo "$dir" | sed -E 's/\.\/(.+)\/target.+/\1/')
  echo "  - Copying test reports for $module"
  mkdir -p "$REPORTS_DIR/test-reports/$module"
  cp -r "$dir"/* "$REPORTS_DIR/test-reports/$module"/
done

# Create test summary
echo "📝 Creating test summary..."
TEST_SUMMARY="$REPORTS_DIR/test-summary.md"
echo "# Test Results Summary" > "$TEST_SUMMARY"
echo "" >> "$TEST_SUMMARY"

find . -name "TEST-*.xml" | while read -r file; do
  module=$(echo "$file" | sed -E 's/\.\/(.+)\/target.+/\1/')
  test_name=$(basename "$file" .xml | sed 's/TEST-//')
  
  # Extract pass/fail counts
  total=$(grep -o 'tests="[0-9]*"' "$file" | sed 's/tests="//' | sed 's/"//')
  failures=$(grep -o 'failures="[0-9]*"' "$file" | sed 's/failures="//' | sed 's/"//')
  errors=$(grep -o 'errors="[0-9]*"' "$file" | sed 's/errors="//' | sed 's/"//')
  skipped=$(grep -o 'skipped="[0-9]*"' "$file" | sed 's/skipped="//' | sed 's/"//')
  
  # Calculate pass count
  pass=$((total - failures - errors - skipped))
  
  # Append to summary
  echo "## Module: $module" >> "$TEST_SUMMARY"
  echo "- Test: $test_name" >> "$TEST_SUMMARY"
  echo "- Total: $total tests" >> "$TEST_SUMMARY"
  echo "- Passed: $pass tests" >> "$TEST_SUMMARY"
  if [ "$failures" -gt 0 ]; then
    echo "- Failed: $failures tests ❌" >> "$TEST_SUMMARY"
  fi
  if [ "$errors" -gt 0 ]; then
    echo "- Errors: $errors tests ❌" >> "$TEST_SUMMARY"
  fi
  if [ "$skipped" -gt 0 ]; then
    echo "- Skipped: $skipped tests ⚠️" >> "$TEST_SUMMARY"
  fi
  echo "" >> "$TEST_SUMMARY"
done

# Print final summary
echo "✅ Testing complete!"
echo "📂 Reports available in: $REPORTS_DIR"
echo "  - Test Summary: $TEST_SUMMARY"

if [ "$SKIP_COVERAGE" = false ]; then
  echo "  - Coverage Summary: $COVERAGE_SUMMARY"
  echo "  - Coverage Reports: $REPORTS_DIR/coverage/"
fi

echo "  - Test Reports: $REPORTS_DIR/test-reports/"
echo
echo "You can open the HTML reports in your browser to view detailed results"