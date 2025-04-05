# Testing Documentation for Agentic AI Framework

This directory contains comprehensive documentation about the testing approach and test suites in the Agentic AI Framework.

## Overview

The Agentic AI Framework has a robust testing strategy that covers various components and integration points. The documentation is organized by functional area to provide clear guidance on testing approaches and existing test coverage.

## Documentation Structure

- [Testing Summary](TestingSummary.md) - High-level overview of all testing
- [Memory System Tests](MemorySystemTests.md) - Details on memory system test coverage
- [Circuit Pattern Tests](CircuitPatternTests.md) - Information on testing agent circuit components
- [LLM Integration Tests](LLMIntegrationTests.md) - Guide to testing LLM integrations
- [Application and Integration Tests](ApplicationAndIntegrationTests.md) - Guide to testing complete applications
- [VertexAI Integration](VertexAIIntegration.md) - Specific guide for testing Vertex AI integration

## Key Testing Principles

1. **Isolation**: Each component is tested in isolation with appropriate mocks
2. **Composition**: Complex components are tested through composition of simpler parts
3. **Coverage**: Tests aim to cover normal operation, edge cases, and error conditions
4. **Performance**: Critical paths include performance tests to ensure efficiency
5. **Resource Safety**: Tests verify resources are properly acquired and released

## Running Tests

### Basic Test Commands

```bash
# Run all tests
sbt test

# Run specific test suite
sbt "testOnly com.agenticai.core.memory.MemorySystemSpec"

# Run tests in a specific package
sbt "testOnly com.agenticai.core.memory.*"

# Run integration tests
sbt it:test
```

### Test Configuration

Some tests can be configured through environment variables:

```bash
# Configure logging level for tests
export AGENTIC_LOG_LEVEL=DEBUG

# Configure Vertex AI testing
export GOOGLE_CLOUD_PROJECT="your-gcp-project-id"
export VERTEX_LOCATION="us-central1"
```

## Test Coverage Areas

The framework has tests covering these major areas:

1. **Memory System**: Storage, retrieval, cleanup, and monitoring
2. **Circuit Patterns**: Functional combinators and agent composition
3. **LLM Integration**: Communication with language models
4. **Applications**: End-to-end testing of complete applications

## Adding New Tests

When adding new components to the framework, follow these guidelines:

1. Create tests that verify component behavior in isolation
2. Create tests that verify integration with other components
3. Include tests for normal operation, edge cases, and error scenarios
4. Use appropriate patterns (ZIO, mocks, etc.) consistent with existing tests
5. Update documentation to describe the new tests

## Test Tagging

Tests use ZIO's test tags to categorize tests for selective execution:

```scala
test("example")(
  // Test body
) @@ TestAspect.tagged(Tags.Integration)
```

Common tags include:
- `Unit`: Basic unit tests
- `Integration`: Tests requiring component integration
- `Slow`: Tests that take longer to execute
- `Resource`: Tests that require external resources

## Continuous Integration

Tests are run as part of CI/CD pipelines to ensure ongoing quality. The pipeline:

1. Runs unit tests on every pull request
2. Runs integration tests before merging to main branches
3. Generates test coverage reports
4. Ensures no regressions are introduced

## Performance Testing

Performance-critical components include performance tests that:

1. Measure throughput and latency
2. Compare against baseline performance
3. Verify behavior under load
4. Test resource usage efficiency

## Test Mocks and Utilities

The framework includes several testing utilities:

- Mock LLM clients for testing without API calls
- TestClock utilities for time-dependent testing
- Resource management helpers for tests using external resources
- Mock implementations of core interfaces

## Contributing to Tests

When contributing to the test suite:

1. Follow the existing patterns and conventions
2. Ensure tests are deterministic and not flaky
3. Document test purpose and approach
4. Consider edge cases and error conditions
5. Update relevant documentation files
