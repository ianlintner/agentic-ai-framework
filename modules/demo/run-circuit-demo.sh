#!/bin/bash

# Factorio Circuit Demo Runner Script
# This script runs the Factorio-inspired circuit pattern demos

# Set the current directory to the script's directory
cd "$(dirname "$0")"

# Colors for better output
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

echo -e "${GREEN}===================================================${NC}"
echo -e "${GREEN}   Factorio Circuit Patterns Demo Runner${NC}"
echo -e "${GREEN}===================================================${NC}"
echo ""

# Check if an argument was provided
if [ $# -eq 0 ]; then
  echo -e "${YELLOW}No specific demo selected. Running all demos.${NC}"
  echo ""
  DEMO_ARG=""
else
  DEMO_ARG="$1"
  
  case "$DEMO_ARG" in
    text|bit-packing|clock|memory|help)
      echo -e "${YELLOW}Running demo: $DEMO_ARG${NC}"
      echo ""
      ;;
    *)
      echo -e "${YELLOW}Unknown demo: $DEMO_ARG. Using 'help' instead.${NC}"
      DEMO_ARG="help"
      echo ""
      ;;
  esac
fi

# Run the demo using sbt
echo -e "${BLUE}Starting demo execution...${NC}"
echo ""

sbt "demo/runMain com.agenticai.demo.FactorioCircuitDemo $DEMO_ARG"

echo ""
echo -e "${GREEN}===================================================${NC}"
echo -e "${GREEN}   Demo Execution Completed${NC}"
echo -e "${GREEN}===================================================${NC}"