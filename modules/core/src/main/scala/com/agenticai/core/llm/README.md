# Claude 3.7 on Google Vertex AI Integration

This directory contains implementations for integrating with Claude 3.7 via Google Cloud's Vertex AI platform. The integration allows you to use Claude 3.7 models within your Agentic AI Framework applications.

## Components

The integration consists of three main components:

1. **VertexAIConfig**: Configuration for the Vertex AI client, including Google Cloud project settings and Claude model parameters.
2. **VertexAIClient**: Client for communicating with the Vertex AI API, handling authentication and request formatting.
3. **ClaudeAgent**: Implementation of the Agent interface that uses Claude via Vertex AI.

## Setup Instructions

### 1. Google Cloud Setup

Before using this integration, you need to set up your Google Cloud environment:

1. Create a Google Cloud project or use an existing one
2. Enable the Vertex AI API
3. Set up authentication (either application default credentials or a service account)

### 2. Required Dependencies

Add the following dependencies to your project:

```scala
// In build.sbt or Dependencies.scala
libraryDependencies ++= Seq(
  "com.google.cloud" % "google-cloud-vertexai" % "3.7.0",
  "com.google.auth" % "google-auth-library-oauth2-http" % "1.19.0"
)
```

### 3. Authentication

There are two ways to authenticate with Google Cloud:

#### Application Default Credentials

Run the following command to set up application default credentials:

```bash
gcloud auth application-default login
```

#### Service Account

1. Create a service account with the necessary permissions
2. Download the JSON key file
3. Provide the path to the key file in your configuration:

```scala
val config = VertexAIConfig.claudeDefault.copy(
  projectId = "your-project-id",
  credentialsPath = Some("/path/to/credentials.json")
)
```

## Usage Examples

### Basic Example

```scala
import com.agenticai.core.llm._
import com.agenticai.core.memory._
import zio._

object ClaudeExample extends ZIOAppDefault {
  def run = {
    val config = VertexAIConfig.claudeDefault.copy(
      projectId = "your-project-id"
    )
    
    val memorySystem = new InMemorySystem()
    
    for {
      agent <- ClaudeAgent.create(config, memorySystem)
      response <- agent.process("Hello, Claude! How are you today?").runHead
        .someOrFail(new RuntimeException("No response received"))
      _ <- Console.printLine(s"Claude says: $response")
    } yield ()
  }
}
```

### Streaming Example

For real-time streaming of responses:

```scala
import com.agenticai.core.llm._
import com.agenticai.core.memory._
import zio._

object ClaudeStreamingExample extends ZIOAppDefault {
  def run = {
    val config = VertexAIConfig.claudeDefault.copy(
      projectId = "your-project-id"
    )
    
    val memorySystem = new InMemorySystem()
    
    for {
      agent <- ClaudeAgent.create(config, memorySystem)
      _ <- Console.print("Claude is thinking: ")
      _ <- agent.generateStream("Explain quantum computing in simple terms")
        .foreach(chunk => Console.print(chunk))
      _ <- Console.printLine("")
    } yield ()
  }
}
```

## Integration Testing

To run integration tests, make sure your environment is properly set up with authentication and the necessary permissions:

```bash
# Run unit tests (no API calls)
sbt test

# Run integration tests (makes actual API calls)
sbt integrationTest

# Test connectivity only 
sbt testVertexConnection
```

## Performance Considerations

- Claude 3.7 Haiku is designed for efficiency and speed
- For longer conversation context, consider using Claude 3.7 Sonnet or Opus
- To reduce costs, minimize the number of API calls by batching requests when possible

## Troubleshooting

Common issues:

1. **Authentication errors**: Make sure your credentials are properly set up
2. **Project not found**: Verify your Google Cloud project ID
3. **Region not available**: Check that Claude is available in your chosen region
4. **Quota exceeded**: You may need to request an increase in your quota for Vertex AI

For detailed error messages, enable debug logging:

```scala
val config = VertexAIConfig.claudeDefault.copy(
  projectId = "your-project-id",
  // Other settings...
)