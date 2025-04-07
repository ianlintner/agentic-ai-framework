#!/bin/bash

# Set default paths for GENDEV and SGDK if not already set
if [ -z "$GENDEV" ]; then
  export GENDEV="/opt/gendev"  # Default path, change as needed
fi

if [ -z "$SGDK" ]; then
  export SGDK="$GENDEV/sgdk"  # Default path based on GENDEV
fi

echo "Using GENDEV: $GENDEV"
echo "Using SGDK: $SGDK"

# Run the original command/script with all arguments
# Replace this with the actual command you're trying to run
"$@"
