# Claude 3.7 on Vertex AI Integration: Implementation Plan

This document outlines a comprehensive plan to fix the dependency issues and properly integrate Claude 3.7 via Google's Vertex AI into the agentic-ai-framework.

## Current Issues

1. **Repository Configuration Issues**:
   - Custom Nexus repository unable to find required dependencies
   - Maven Central access appears restricted or incorrectly configured

2. **Scala Version Incompatibilities**:
   - Compiler bridge errors when attempting to compile Scala 2.12/2.13 code
   - Possible Java 23 compatibility issues with older Scala versions

3. **Missing Dependencies**:
   - Google Cloud libraries not properly resolved
   - ZIO libraries missing in the build

## Implementation Plan

### Phase 1: Fix Build Configuration

1. **Repository Configuration**:
   ```scala
   // Add to build.sbt
   ThisBuild / resolvers ++= Seq(
     Resolver.mavenLocal,
     Resolver.mavenCentral,
     "Sonatype OSS Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots",
     "Sonatype OSS Releases" at "https://oss.sonatype.org/content/repositories/releases",
     "Typesafe repository" at "https://repo.typesafe.com/typesafe/releases/"
   )
   
   // Override any custom repository settings
   ThisBuild / useCoursier := true
   ThisBuild / updateOptions := updateOptions.value.withGigahorse(true)
   ```

2. **SBT Configuration**:
   - Create `project/repositories` file with proper repository configuration
   - Update `project/build.properties` to use a compatible SBT version

3. **Scala Version**:
   - Use Scala 2.13.10 which is widely supported and compatible with most libraries

### Phase 2: Fix Dependencies

1. **Update project/Dependencies.scala**:
   ```scala
   object Dependencies {
     object Versions {
       val zio        = "2.0.15"
       val zioStreams = "2.0.15"
       val googleCloudVertexAI = "0.5.0"
       val googleCloudCore     = "2.22.0"
       val googleAuth          = "1.19.0"
     }
   
     val zio = Seq(
       "dev.zio" %% "zio"         % Versions.zio,
       "dev.zio" %% "zio-streams" % Versions.zioStreams
     )
   
     val googleCloud = Seq(
       "com.google.cloud" % "google-cloud-aiplatform" % Versions.googleCloudVertexAI,
       "com.google.cloud" % "google-cloud-core"       % Versions.googleCloudCore,
       "com.google.auth"  % "google-auth-library-oauth2-http" % Versions.googleAuth
     )
   }
   ```

2. **Update build.sbt to include these dependencies**:
   ```scala
   libraryDependencies ++= Dependencies.zio ++ Dependencies.googleCloud
   ```

### Phase 3: Fix Google Auth Integration

1. **Update VertexAIConfig.scala**:
   - Use the proper Google Auth library
   - Fix credentials handling

2. **Update VertexAIClient.scala**:
   - Update to use the correct Claude 3.7 model path
   - Fix authentication flow

### Phase 4: Implement Actual Integration

1. **Create Proper Claude 3.7 Request Format**:
   ```scala
   case class ClaudeMessage(role: String, content: String)
   
   case class ClaudeRequest(
     model: String,
     messages: List[ClaudeMessage],
     maxTokens: Int,
     temperature: Float,
     topP: Float,
     topK: Int,
     stopSequences: List[String]
   )
   ```

2. **Implement Response Handling**:
   ```scala
   case class ClaudeResponse(
     id: String,
     model: String,
     content: String,
     stopReason: String,
     usage: TokenUsage
   )
   
   case class TokenUsage(
     inputTokens: Int,
     outputTokens: Int
   )
   ```

3. **Implement ZIO-based Client**:
   ```scala
   def generateText(prompt: String): ZIO[Any, Throwable, String] = {
     for {
       client <- ZIO.succeed(VertexAiPredictionServiceClient.create())
       request <- createPredictionRequest(prompt)
       response <- ZIO.attemptBlocking(client.predict(request))
       result <- parseResponse(response)
     } yield result
   }
   ```

### Phase 5: Testing

1. **Unit Tests**:
   - Create tests with mocked responses
   - Test error handling

2. **Integration Tests**:
   - Create real (but limited) API calls
   - Test with actual Claude 3.7 model

3. **End-to-End Tests**:
   - Run full workflow examples

## Fallback Plan

If dependency issues persist, the fallback approach is to:

1. Keep a dedicated standalone demo module that doesn't depend on the main project
2. Use HTTP-based integration rather than the Google Cloud SDK directly
3. Use shell scripts as we've done in this solution to demonstrate functionality

## Completion Criteria

The integration will be considered complete when:
1. All builds pass without errors
2. Unit tests pass
3. Integration tests pass
4. We can successfully make API calls to Claude 3.7 via Vertex AI
5. The implementation aligns with our agentic framework design principles