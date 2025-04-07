#!/bin/bash

# This is a test script that simulates the original script that was checking for GENDEV and SGDK

echo "========================================"
echo "    Building Carvana Outrun MVP"
echo "========================================"

# Check if GENDEV and SGDK are set
if [ -z "$GENDEV" ] || [ -z "$SGDK" ]; then
  echo "GENDEV and/or SGDK environment variables not set."
  echo "Please set them before running this script:"
  echo "export GENDEV=/path/to/gendev"
  echo "export SGDK=\$GENDEV/sgdk"
  exit 1
fi

echo "GENDEV and SGDK environment variables are set correctly."
echo "GENDEV: $GENDEV"
echo "SGDK: $SGDK"
echo "Continuing with build process..."
echo "Build completed successfully!"
