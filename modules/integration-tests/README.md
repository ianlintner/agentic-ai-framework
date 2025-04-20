# ZIO Agentic AI Framework Integration Tests Module

**Version:** 1.0.0  
**Last Updated:** April 19, 2025  
**Author:** ZIO Agentic AI Framework Team  

## Overview

The Integration Tests module contains comprehensive end-to-end tests for the ZIO Agentic AI Framework. These tests verify that different modules work together correctly in real-world scenarios, ensuring the framework functions as a cohesive system rather than just as individual components.

## Testing Approach

Integration tests in this module focus on:

- Cross-module interactions
- End-to-end workflows
- Real-world usage patterns
- Performance under realistic conditions
- System stability and reliability

## Dual Testing Structure

ZIO Agentic AI Framework currently has a dual integration testing structure:

1. **This Module (`modules/integration-tests/`)**: Contains integration tests focused on core framework functionality.
2. **Root Level IT Directory (`it/`)**: Contains integration tests specifically for Langchain4j and LLM provider integrations.

> **Note**: As outlined in the [DependencyFixPlan.md](../../docs/implementation/DependencyFixPlan.md), these two test structures will be consolidated in the future. This module will eventually contain all integration tests.

## Integration Tests vs. Unit Tests

| Aspect | Unit Tests | Integration Tests |
|--------|------------|------------------|
| Location | Within each module | This dedicated module |
| Focus | Individual components | Cross-module interactions |
| Dependencies | Mocked/stubbed | Real implementations |
| Runtime | Fast | May be slower |
| External Services | Mocked | Real or containerized |

## Test Categories

### Core System Tests

Tests that verify the fundamental interactions between core ZIO Agentic AI Framework modules:

- Core + Memory interactions
- Agent lifecycle and state management
- Capability system integration

### Mesh System Tests

Tests for distributed agent mesh functionality:

- Agent discovery and registration
- Message routing and delivery
- Distributed state synchronization
- Coordination patterns

### LLM Integration Tests

Tests for Language Model integrations:

- Multi-provider support
- Prompt handling
- Response processing
- Error handling and retry logic

### End-to-End Workflow Tests

Tests for complete workflows using multiple ZIO Agentic AI Framework components:

- Multi-agent conversations
- Complex task execution
- External tool integration

## Running Integration Tests

### Prerequisites

Before running the tests:

1. Ensure all required dependencies are installed
2. Configure any necessary environment variables for external services
3. Make sure you have sufficient resources (memory, CPU) for the tests

### Basic Test Execution

Run all integration tests with:

```bash
sbt "integration-tests/test"
```

### Running Specific Tests

Run a specific test category:

```bash
sbt "integration-tests/testOnly com.agenticai.it.mesh.*"
```

### Test Configuration

The tests use the standard ZIO Agentic AI Framework configuration system with some test-specific overrides:

```hocon
integration-tests {
  timeouts {
    default = 30 seconds
    mesh-discovery = 60 seconds
  }
  
  external-services {
    use-containers = true
    container-startup-timeout = 120 seconds
  }
}
```

## Writing New Integration Tests

### Guidelines

When adding new integration tests:

1. **Focus on integration**: Test the interactions between components, not the components themselves
2. **Be realistic**: Tests should reflect real-world usage scenarios
3. **Be comprehensive**: Cover both happy paths and error scenarios
4. **Be efficient**: Avoid unnecessary duplication of setup code
5. **Be stable**: Tests should not be flaky or dependent on external factors

### Test Structure

Follow this structure for new tests:

```scala
package com.agenticai.it

import zio._
import zio.test._
import com.agenticai.it.utils.IntegrationTestUtils

object MyIntegrationSpec extends ZIOSpecDefault {
  def spec = suite("Integration Test Suite")(
    test("should correctly integrate component A with component B") {
      for {
        // Setup
        resources <- IntegrationTestUtils.setupResourcesFor("test-case-1")
        
        // Execute the test scenario
        result <- testLogic(resources)
        
        // Assert the outcome
        _ <- assertResults(result)
      } yield assertCompleted
    }
  )
  
  private def testLogic(resources: TestResources): ZIO[Any, Throwable, TestResult] = ???
  private def assertResults(result: Any): ZIO[Any, Nothing, TestResult] = ???
}
```

### Shared Test Utilities

The module provides several utilities for test authors:

- `IntegrationTestUtils`: Common setup and teardown functionality
- `TestResources`: Managed resources for tests
- `TestAgents`: Pre-configured agent instances for testing
- `TestEnvironment`: Simulated environment for repeatable tests

## Troubleshooting

### Common Issues

- **Test timeouts**: Integration tests may need longer timeouts than unit tests
- **Resource conflicts**: Multiple tests using the same resources may interfere with each other
- **External dependencies**: Tests dependent on external services may fail if those services are unavailable

### Debugging Failed Tests

1. Run with increased logging:
   ```bash
   sbt -Dlog.level=DEBUG "integration-tests/testOnly com.agenticai.it.FailingSpec"
   ```

2. Use the test explorer in your IDE for step-by-step debugging

3. Check the test logs in `target/integration-test-logs/`

## Contributing

Contributions to the integration test suite are welcome! Please follow these steps:

1. Identify a gap in the current test coverage
2. Create a new test file in the appropriate package
3. Implement tests according to the guidelines above
4. Submit a PR with your changes

For more details, see the [CONTRIBUTING.md](../../CONTRIBUTING.md) file.