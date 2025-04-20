# Integration Tests Setup

**Author:** ZIO ZIO Agentic AI Framework Team  
**Date:** April 19, 2025  
**Version:** 1.0.0

## Overview

This document explains the integration testing setup for the ZIO Agentic AI Framework. Integration tests verify that different modules of the framework work together correctly and that the framework integrates properly with external services like LLM providers.

## Current Integration Test Structure

The framework currently has two separate integration test setups:

1. **Langchain4j Integration Tests** (`it/`): Focused on testing Langchain4j integration with real LLM providers.
2. **Framework Integration Tests** (`modules/integration-tests/`): Tests for integration between framework modules.

This dual structure is being consolidated to improve organization and test coverage.

## Langchain4j Integration Tests (`it/`)

### Purpose

The `it/` directory contains integration tests specifically for the Langchain4j module. These tests verify that the Langchain4j integration works correctly with actual LLM API calls.

### Structure

```
it/
├── src/
│   └── test/scala/com/agenticai/core/llm/
│       ├── ClaudeIntegrationSpec.scala
│       ├── VertexAIGeminiIntegrationSpec.scala
│       └── GoogleAIGeminiIntegrationSpec.scala
```

### Test Configuration

The tests use the `IntegrationTestConfig` object to manage environment variables and test configuration. If the required environment variables are not set, the tests will be skipped.

### Running the Tests

To run the integration tests, you need to set the appropriate environment variables for the LLM provider you want to test:

#### Claude Tests

```bash
export CLAUDE_API_KEY=your-claude-api-key
export CLAUDE_MODEL_NAME=claude-3-haiku-20240307  # Optional, defaults to claude-3-haiku-20240307
sbt "it/testOnly com.agenticai.core.llm.langchain.ClaudeIntegrationSpec"
```

#### Vertex AI Tests

```bash
export GOOGLE_CLOUD_PROJECT=your-gcp-project-id
export VERTEX_LOCATION=us-central1  # Optional, defaults to us-central1
export VERTEX_MODEL_NAME=gemini-1.0-pro  # Optional, defaults to gemini-1.0-pro
sbt "it/testOnly com.agenticai.core.llm.langchain.VertexAIGeminiIntegrationSpec"
```

Note: The tests use the local credentials from `gcloud auth login` by default. If you haven't authenticated with gcloud, run:

```bash
gcloud auth login
gcloud config set project your-gcp-project-id
```

Alternatively, you can explicitly set the credentials path:

```bash
export GOOGLE_APPLICATION_CREDENTIALS=/path/to/credentials.json
```

#### Google AI Tests

```bash
export GOOGLE_API_KEY=your-google-api-key
export GOOGLE_MODEL_NAME=gemini-1.0-pro  # Optional, defaults to gemini-1.0-pro
sbt "it/testOnly com.agenticai.core.llm.langchain.GoogleAIGeminiIntegrationSpec"
```

#### Running All Tests

To run all integration tests (only those with available credentials will execute):

```bash
sbt it/test
```

### Test Cases

Each integration test suite includes the following test cases:

1. **Basic Response**: Tests that the model can generate a basic response to a simple prompt.
2. **Streaming**: Tests that the model can stream responses.
3. **Multi-turn Conversations**: Tests that the model can maintain context across multiple turns.
4. **Reasoning**: Tests that the model can perform basic reasoning tasks.
5. **Error Handling**: Tests that the system handles errors gracefully.

## Framework Integration Tests (`modules/integration-tests/`)

### Purpose

The `modules/integration-tests/` directory contains tests that verify the integration between different modules of the framework. These tests ensure that the modules work together correctly in realistic scenarios.

### Structure

```
modules/integration-tests/
├── src/
│   └── test/scala/com/agenticai/core/
│       └── [Integration tests]
```

### Current Status

The framework integration tests are currently being developed and expanded. The module is not yet fully integrated into the build process, as indicated by the commented-out `it` module in the main `build.sbt` file.

### Test Cases

The framework integration tests focus on:

1. **Module Interaction**: Tests that different modules can interact correctly.
2. **End-to-End Workflows**: Tests complete workflows from start to finish.
3. **Error Handling**: Tests that errors are properly propagated and handled between modules.

## Integration Test Best Practices

When writing integration tests for the ZIO Agentic AI Framework, follow these best practices:

### 1. Use Real External Services Sparingly

- Use real external services (like LLM APIs) only when necessary to verify integration.
- Consider using smaller, faster models for testing to reduce costs and test execution time.
- Implement caching or recording/replay mechanisms for deterministic tests.

### 2. Manage Test Resources Properly

- Use ZIO's resource management to ensure proper cleanup of resources.
- Isolate test environments to prevent interference between tests.
- Use unique identifiers for test resources to avoid conflicts.

### 3. Handle Asynchronous Operations

- Use appropriate timeouts for external service calls.
- Handle retries and backoff for flaky external services.
- Use ZIO's concurrency features to test parallel operations.

### 4. Test Configuration

- Use environment variables for test configuration.
- Provide sensible defaults for optional configuration.
- Skip tests that require unavailable resources.

### 5. Test Coverage

- Test both happy paths and error cases.
- Test edge cases and boundary conditions.
- Test performance characteristics where relevant.

## Future Integration Test Improvements

The following improvements are planned for the integration test setup:

1. **Consolidation**: Merge the two integration test setups into a single, cohesive structure.
2. **Build Integration**: Fully integrate the integration tests into the build process.
3. **CI/CD Integration**: Add integration test runs to the CI/CD pipeline.
4. **Test Coverage**: Expand test coverage to include all modules and their interactions.
5. **Documentation**: Improve documentation of integration test setup and execution.

## Adding New Integration Tests

To add new integration tests:

1. Identify the integration points to test.
2. Determine whether the test belongs in `it/` or `modules/integration-tests/`.
3. Create a new test file in the appropriate directory.
4. Implement the test cases following the patterns in existing tests.
5. Update this documentation with instructions for running the new tests.

## Troubleshooting

### Common Issues

1. **Missing Environment Variables**: Ensure all required environment variables are set.
2. **Authentication Issues**: Verify that authentication credentials are valid and have the necessary permissions.
3. **Rate Limiting**: Be aware of rate limits for external services and implement appropriate backoff strategies.
4. **Test Isolation**: Ensure tests don't interfere with each other by using unique identifiers and proper cleanup.

### Debugging Tips

1. **Enable Verbose Logging**: Use the `-v` flag with sbt to enable verbose logging.
2. **Run Individual Tests**: Run specific tests to isolate issues.
3. **Check External Service Status**: Verify that external services are available and functioning correctly.
4. **Review Test Configuration**: Double-check that test configuration is correct.

## References

- [ZIO Test Documentation](https://zio.dev/reference/test/)
- [Langchain4j Documentation](https://docs.langchain4j.dev/)
- [SBT Multi-Project Builds](https://www.scala-sbt.org/1.x/docs/Multi-Project.html)