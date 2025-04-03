#!/bin/bash

# This script proves that we are using Scala 3-only features
# by attempting to compile and run code that would be impossible
# in Scala 2.x

echo "=== Demonstrating Scala 3-ONLY Features ==="
echo "This demo proves we are using Scala 3 by using features like:"
echo "- Top-level definitions"
echo "- Native enums"
echo "- Union types"
echo "- Extension methods"
echo "- Opaque type aliases"
echo "- Significant indentation syntax"
echo "- And many more Scala 3-specific features"
echo ""

# Set up SBT options to see detailed output
export SBT_OPTS="$SBT_OPTS -Dsbt.log.noformat=true"

# Clean and compile the project to ensure we're building fresh
echo "Cleaning and compiling with Scala 3.3.1..."
sbt clean compile

# Run the Scala 3-only demo app (this will fail if using Scala 2.x)
echo "" 
echo "=== Running Scala3OnlyApp ==="
echo "This will ONLY succeed if using Scala 3, as it uses features not available in Scala 2.x"
sbt "runMain com.agenticai.demo.Scala3OnlyApp"

# Display the Scala version being used
echo ""
echo "=== Current Scala Version ==="
sbt "scalaVersion"

echo ""
echo "=== Demo Complete ==="
echo "If this compiled and ran successfully, it PROVES we are using Scala 3"
echo "as the features used in Scala3OnlyFeatures.scala don't exist in Scala 2.x"