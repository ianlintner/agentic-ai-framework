# Dependency Fix Plan

After running `sbt test`, we've identified several critical issues that need to be addressed to align with our .roorules requirement of having "Always be buildable and testable" code. This document outlines the issues and provides an implementation plan to fix them.

## Current Issues

1. **Missing ZIO Dependencies**:
   - Error: `Not found: zio` in multiple files
   - This is a critical issue as our entire framework is built on ZIO

2. **Missing Google Cloud Dependencies**:
   - Error: `Error downloading com.google.cloud:google-cloud-vertexai:3.7.0`
   - Error: `value google is not a member of com`

3. **Java/Scala API Compatibility Issues**:
   - Error in `VertexAIClaudeDemo`: `value getOrDefault is not a member of Map[String, String]`

## Implementation Plan

### Phase 1: Fix Dependencies in project/Dependencies.scala

```scala
// Current Dependencies.scala needs to be updated with:

object Dependencies {
  object Versions {
    val zio = "2.0.15" // Latest stable ZIO version
    val zioStreams = "2.0.15"
    val zioTest = "2.0.15"
    val googleCloud = "3.5.0" // Use a verified available version
    val googleAuth = "1.19.0"
  }

  // Core dependencies
  val coreDependencies = Seq(
    "dev.zio" %% "zio" % Versions.zio,
    "dev.zio" %% "zio-streams" % Versions.zioStreams
  )

  // Agent dependencies including Google Cloud
  val agentDependencies = Seq(
    "com.google.cloud" % "google-cloud-aiplatform" % Versions.googleCloud,
    "com.google.auth" % "google-auth-library-oauth2-http" % Versions.googleAuth
  )

  // Add appropriate test dependencies
  val testDependencies = Seq(
    "dev.zio" %% "zio-test" % Versions.zioTest % Test,
    "dev.zio" %% "zio-test-sbt" % Versions.zioTest % Test
  )
}
```

### Phase 2: Fix Java/Scala Compatibility in VertexAIClaudeDemo

```scala
// Add import for Java conversions
import scala.jdk.CollectionConverters._

// Change the sys.env.getOrDefault to:
projectId: String = sys.env.getOrElse("GOOGLE_CLOUD_PROJECT", ""),
```

### Phase 3: Update build.sbt for Proper Dependency Management

Ensure the build.sbt file properly includes all necessary dependencies for each module and has appropriate cross-version settings for Scala 3.

### Phase 4: Test Incrementally

1. Fix base dependencies first
2. Compile only core module: `sbt core/compile`
3. Add dependencies for each module progressively
4. Test each module as it becomes compilable

## Success Criteria

1. **Compile Success**: `sbt compile` completes without errors
2. **Test Success**: `sbt test` runs and passes tests
3. **Demo Runs**: `sbt "runMain com.agenticai.examples.VertexAIClaudeDemo"` executes successfully

## Timeline

- Day 1: Fix ZIO dependencies and verify core module compiles
- Day 2: Fix Google Cloud dependencies and verify integration modules compile
- Day 3: Fix all remaining issues and ensure all tests pass

## Measuring Alignment with Goals and Rules

Our .roorules document states:
- "Always be buildable and testable"
- "Every change must compile successfully with `sbt compile`"
- "All tests must pass with `sbt test`"

Our goals.md includes:
- "Integration with common ai clients with zio wrappers"
- "Always be buildable and testable"

These fixes directly address these requirements by ensuring:
1. The ZIO foundation works correctly
2. The Google Vertex AI integration can be properly implemented
3. The project compiles and tests can run

After completing this plan, we'll be back on track with our core principles and can continue implementing the ambitious goals outlined in our enhanced goals document.