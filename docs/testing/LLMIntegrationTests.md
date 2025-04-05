# LLM Integration Testing Guide

This document explains how Large Language Model (LLM) integrations are tested in the Agentic AI Framework.

## Overview

The Agentic AI Framework integrates with various LLM providers to enable AI-powered agent behaviors. These integrations need to be thoroughly tested to ensure:

- Reliable communication with LLM APIs
- Proper handling of prompts and responses
- Graceful error handling
- Efficient resource usage
- Integration with the framework's memory and agent components

## Test Structure

LLM integration tests are located in `modules/core/src/test/scala/com/agenticai/core/llm/` and include:

1. `ClaudeAgentSpec.scala`: Tests for the Anthropic Claude integration
2. `VertexAIClientSpec.scala`: Tests for the Google Vertex AI integration
3. `VertexAIConfigSpec.scala`: Tests for configuration handling
4. `VertexAIClientMock.scala`: Mock implementations for testing

## Claude Integration Tests

The `ClaudeAgentSpec` tests verify that the framework can properly interact with Anthropic's Claude models.

### Basic Prompt Handling

Tests that the Claude agent can process basic prompts:

```scala
test("should handle basic prompts") {
  for {
    agent <- ClaudeAgent.make(VertexAIConfig.claudeDefault)
    result <- agent.process("test input").runHead.someOrFail(new RuntimeException("No result"))
  } yield assertTrue(result.nonEmpty)
} @@ TestAspect.withLiveClock
```

### Error Handling

Tests that errors from the LLM API are properly handled:

```scala
test("handles errors") {
  val testError = new RuntimeException("test")
  val failingClient = new VertexAIClient {
    def generateText(prompt: String) = ZIO.fail(testError)
    def complete(prompt: String) = ZIO.fail(testError)
    def streamCompletion(prompt: String) = ZStream.fail(testError)
  }
  
  for {
    memory <- ZIO.service[MemorySystem]
    agent = new ClaudeAgent("test", failingClient, memory)
    result <- agent.process("test").runHead.exit
  } yield assertTrue(result.isFailure)
}
```

## Vertex AI Integration

The Vertex AI integration tests ensure proper communication with Google's Vertex AI platform, which is used to access Claude and other models.

### Client Configuration

Tests that the Vertex AI client can be properly configured:

```scala
test("should configure with correct values") {
  val config = VertexAIConfig(
    projectId = "test-project",
    location = "us-central1",
    modelId = "claude-3-7-haiku-20240307"
  )
  
  assertTrue(
    config.projectId == "test-project" &&
    config.location == "us-central1" &&
    config.modelId == "claude-3-7-haiku-20240307"
  )
}
```

### API Communication

Tests that the client can communicate with the Vertex AI API:

```scala
test("should handle API requests") {
  for {
    client <- VertexAIClient.make(VertexAIConfig.claudeDefault)
    response <- client.complete("Hello, world!")
  } yield assertTrue(response.nonEmpty)
} @@ TestAspect.withLiveClock
```

### Response Streaming

Tests streaming responses, which is important for realtime agent interactions:

```scala
test("streamCompletion should stream responses") {
  for {
    client <- VertexAIClient.make(VertexAIConfig.claudeDefault)
    responses <- client.streamCompletion("Hello, world!").take(5).runCollect
  } yield assertTrue(responses.nonEmpty)
} @@ TestAspect.withLiveClock
```

## Mock Client Implementation

For testing without requiring actual API calls, a mock client implementation is provided:

```scala
class VertexAIClientMock extends VertexAIClient {
  def generateText(prompt: String): Task[String] = 
    ZIO.succeed(s"Mock response to: $prompt")
    
  def complete(prompt: String): Task[String] = 
    ZIO.succeed(s"Mock completion for: $prompt")
    
  def streamCompletion(prompt: String): ZStream[Any, Throwable, String] = 
    ZStream.fromIterable(Seq("Mock ", "streaming ", "response ", "for: ", prompt))
}
```

This mock is used in tests that need to verify behavior without making actual API calls:

```scala
test("agent should use memory with mock LLM") {
  for {
    memory <- ZIO.service[MemorySystem]
    mockClient = new VertexAIClientMock()
    agent = new ClaudeAgent("test-agent", mockClient, memory)
    result <- agent.process("test prompt").runHead
  } yield assertTrue(result.nonEmpty)
}
```

## Integration With Memory System

Tests verify that the LLM agents properly integrate with the memory system:

```scala
test("agent should store results in memory") {
  for {
    memory <- ZIO.service[MemorySystem]
    mockClient = new VertexAIClientMock()
    agent = new ClaudeAgent("memory-test", mockClient, memory)
    _ <- agent.process("first prompt").runDrain
    memoryCell <- memory.getCellsByTag("agent:memory-test").map(_.headOption)
    cellContent <- ZIO.fromOption(memoryCell).flatMap(_.read)
  } yield assertTrue(cellContent.isDefined && cellContent.get.contains("first prompt"))
}
```

## Test Design Philosophy

The LLM integration tests follow these principles:

1. **Isolated Testing**: Where possible, tests use mocks to avoid external dependencies
2. **Live Testing**: Critical integration points are tested with actual API calls
3. **Error Handling**: Tests verify proper handling of various error conditions
4. **Resource Management**: Tests ensure resources are properly acquired and released
5. **Integration Verification**: Tests confirm LLM components integrate with other parts of the framework

## Running LLM Tests

Execute the LLM integration tests using SBT:

```bash
# Run all LLM tests
sbt "testOnly com.agenticai.core.llm.*"

# Run a specific test suite
sbt "testOnly com.agenticai.core.llm.ClaudeAgentSpec"

# Run with live API access for integration testing
export VERTEX_AI_TEST_MODE=live
sbt "testOnly com.agenticai.core.llm.*"
```

## Test Environment Configuration

For running tests that interact with actual LLM APIs, you'll need to configure the test environment:

```bash
# Required for live testing
export GOOGLE_CLOUD_PROJECT="your-gcp-project-id"
export GOOGLE_APPLICATION_CREDENTIALS="/path/to/credentials.json"

# Optional configuration
export VERTEX_LOCATION="us-central1"
export CLAUDE_MODEL_ID="claude-3-7-haiku-20240307"
```

## Vertex AI Integration Testing

For detailed instructions on testing the Vertex AI integration specifically, refer to:

- [docs/testing/VertexAIIntegration.md](VertexAIIntegration.md)


## Creating New LLM Integration Tests

When adding new LLM integrations, follow these guidelines:

1. Create both mock and live client implementations
2. Test basic prompt handling
3. Test error scenarios and edge cases
4. Test integration with the memory system
5. Test resource management (connections, tokens, etc.)
6. Consider rate limiting and quota issues in tests
7. Document configuration requirements

## Best Practices for LLM Testing

1. **Avoid Rate Limits**: Space out tests that hit actual APIs
2. **Use Deterministic Prompts**: For consistent testing results
3. **Validate Response Structure**: Ensure responses meet expected formats
4. **Mock Expensive Operations**: Use mocks for most tests to reduce costs
5. **Test Timeouts**: Ensure proper handling of slow API responses
6. **Test Long Responses**: Verify handling of responses that approach token limits
7. **Version Control Prompts**: Keep test prompts in version control for consistency

## Common LLM Testing Issues

1. **API Credentials**: Ensure proper authentication is configured
2. **Rate Limiting**: Be aware of API rate limits during test runs
3. **Response Variability**: LLM responses are non-deterministic
4. **Cost Management**: Live API tests incur costs
5. **Long-Running Tests**: LLM API calls may cause timeouts

## LLM Component Architecture in Tests

The LLM integration tests reflect the architecture of the LLM components:

1. **Configuration Layer**: Tests for parsing and validation of config parameters
2. **Client Layer**: Tests for API communication and response handling
3. **Agent Layer**: Tests for integration with agent behaviors and memory
4. **Error Handling Layer**: Tests for proper handling of failures

This layered approach allows for isolated testing of each responsibility within the LLM integration.
