# Testing Documentation

**Author:** ZIO Agentic AI Framework Team  
**Date:** April 19, 2025  
**Version:** 1.0.0

## Overview

This directory contains documentation related to testing the ZIO Agentic AI Framework. The framework uses a comprehensive testing approach that includes unit tests, property-based tests, integration tests, and application tests.

## Testing Philosophy

The ZIO Agentic AI Framework follows these testing principles:

1. **Test-Driven Development**: Tests are written before or alongside code to ensure functionality meets requirements.
2. **Property-Based Testing**: Complex logic is tested using property-based tests to verify behavior across a wide range of inputs.
3. **Integration Testing**: Components are tested together to ensure they work correctly as a system.
4. **Application Testing**: End-to-end tests verify that the framework works correctly in realistic scenarios.
5. **ZIO-Based Testing**: All effectful test code uses ZIO for consistency and reliability.

## Test Types

### Unit Tests

Unit tests verify that individual components work correctly in isolation. They are located in the `src/test/scala` directory of each module.

### Property-Based Tests

Property-based tests verify that components satisfy certain properties across a wide range of inputs. They use libraries like ScalaCheck and ZIO Test to generate test cases.

### Integration Tests

Integration tests verify that different modules work together correctly. They are located in the `it/` directory and the `modules/integration-tests/` directory.

### Application Tests

Application tests verify that the framework works correctly in realistic scenarios. They are located in various modules, particularly in the `examples` module.

## Test Documentation Index

| Document | Description |
|----------|-------------|
| [ApplicationAndIntegrationTests.md](ApplicationAndIntegrationTests.md) | Guide to application and integration testing |
| [CircuitPatternTests.md](CircuitPatternTests.md) | Guide to testing circuit patterns |
| [IntegrationTestsSetup.md](IntegrationTestsSetup.md) | Setup guide for integration tests |
| [LLMIntegrationTests.md](LLMIntegrationTests.md) | Guide to testing LLM integrations |
| [MemorySystemTests.md](MemorySystemTests.md) | Guide to testing the memory system |
| [TestingSummary.md](TestingSummary.md) | Summary of testing approach |
| [VertexAIIntegration.md](VertexAIIntegration.md) | Guide to testing Vertex AI integration |

## Test Setup

### Prerequisites

- Java 11 or later
- Scala 3.3.1 or later
- SBT (Scala Build Tool)

### Running Tests

To run all tests:

```bash
sbt test
```

To run tests for a specific module:

```bash
sbt "core/test"
```

To run integration tests:

```bash
sbt it/test
```

### Generating Test Reports

The project includes advanced test reporting tools:

```bash
# Generate test reports with coverage for all modules
./scripts/run-tests-with-reports.sh --all

# Generate test reports for specific modules
./scripts/run-tests-with-reports.sh --modules=core,mesh

# Skip coverage reports for faster execution
./scripts/run-tests-with-reports.sh --modules=core --skip-coverage
```

## Test Structure

Each test file follows a consistent structure:

1. **Imports**: Required imports for the test
2. **Test Suite**: A ZIO Test suite containing related tests
3. **Test Cases**: Individual test cases that verify specific behaviors
4. **Test Fixtures**: Shared setup and teardown code
5. **Test Utilities**: Helper functions for the tests

Example:

```scala
import zio.test._
import zio.test.Assertion._
import com.agenticai.core.Agent

object AgentSpec extends ZIOSpecDefault {
  def spec = suite("Agent")(
    test("should process messages") {
      // Test implementation
    },
    test("should handle errors") {
      // Test implementation
    }
  )
}
```

## Best Practices

### Writing Good Tests

1. **Test One Thing**: Each test should verify a single behavior
2. **Descriptive Names**: Test names should clearly describe what is being tested
3. **Arrange-Act-Assert**: Structure tests with setup, action, and verification
4. **Independent Tests**: Tests should not depend on each other
5. **Fast Tests**: Tests should run quickly to enable rapid feedback

### Testing Asynchronous Code

1. **Use ZIO**: Use ZIO for testing asynchronous code
2. **Timeouts**: Add timeouts to prevent tests from hanging
3. **Resource Management**: Use ZIO's resource management to ensure proper cleanup

### Testing External Services

1. **Mock External Services**: Use mocks for external services in unit tests
2. **Integration Tests**: Use real external services in integration tests
3. **Test Environments**: Use test environments for external services

## Troubleshooting

### Common Issues

1. **Flaky Tests**: Tests that sometimes pass and sometimes fail
2. **Slow Tests**: Tests that take too long to run
3. **Resource Leaks**: Tests that don't properly clean up resources

### Solutions

1. **Flaky Tests**: Identify and fix the source of non-determinism
2. **Slow Tests**: Optimize test setup and execution
3. **Resource Leaks**: Use ZIO's resource management to ensure proper cleanup

## Contributing

When contributing new code to the framework, please follow these guidelines:

1. **Write Tests**: All new code should have tests
2. **Run Tests**: Ensure all tests pass before submitting a PR
3. **Test Coverage**: Aim for high test coverage
4. **Test Quality**: Write high-quality tests that verify behavior, not implementation

## References

- [ZIO Test Documentation](https://zio.dev/reference/test/)
- [ScalaCheck Documentation](https://github.com/typelevel/scalacheck/blob/main/doc/UserGuide.md)
- [SBT Documentation](https://www.scala-sbt.org/1.x/docs/index.html)
