# ZIO Agentic AI Framework Developer Onboarding Guide

**Version:** 1.0.0  
**Last Updated:** April 19, 2025  
**Author:** ZIO Agentic AI Framework Team  

## Table of Contents

1. [Introduction](#introduction)
2. [Development Environment Setup](#development-environment-setup)
3. [Project Structure](#project-structure)
4. [Building and Testing](#building-and-testing)
5. [Development Workflow](#development-workflow)
6. [Coding Standards](#coding-standards)
7. [Testing Guidelines](#testing-guidelines)
8. [Debugging](#debugging)
9. [Common Issues](#common-issues)
10. [Resources](#resources)

## Introduction

Welcome to the ZIO Agentic AI Framework development team! This guide will help you set up your development environment and understand the workflow and standards used in the project. ZIO Agentic AI Framework is a Scala backend framework for building distributed, agentic mesh systems with a focus on architecture, modularity, testing, and functional purity.

## Development Environment Setup

### Prerequisites

Before you begin, ensure you have the following tools installed:

- **JDK 11+**: Required for running Scala
- **SBT 1.8.0+**: The build tool used for the project
- **Git**: For version control
- **IDE**: IntelliJ IDEA with Scala plugin (recommended) or VS Code with Metals
- **Docker**: For running certain integration tests and examples

### Setup Steps

1. **Clone the repository**:
   ```bash
   git clone https://github.com/agenticai/zio-agentic-ai-framework.git
   cd zio-agentic-ai-framework
   ```

2. **Install dependencies**:
   ```bash
   sbt update
   ```

3. **Configure IDE**:
   - **IntelliJ IDEA**: Import as an SBT project
   - **VS Code**: Install the Metals extension and open the project folder

4. **Set up environment variables** (for certain features):
   ```bash
   # Create a .env file with your configuration
   cp .env.example .env
   # Edit the .env file with your API keys and configuration
   ```

5. **Verify your setup**:
   ```bash
   sbt compile
   sbt test
   ```

## Project Structure

ZIO Agentic AI Framework follows a modular architecture with clear separation of concerns:

```
zio-agentic-ai-framework/
├── build.sbt                 # Main build definition
├── project/                  # SBT configuration
│   ├── build.properties      # SBT version
│   ├── Dependencies.scala    # Dependency definitions
│   └── plugins.sbt           # SBT plugins
├── modules/                  # Core modules
│   ├── core/                 # Core functionality
│   ├── agents/               # Agent system
│   ├── memory/               # Memory subsystem
│   ├── mesh/                 # Distributed mesh
│   ├── http/                 # HTTP capabilities
│   ├── langchain4j/          # LLM integration
│   ├── examples/             # Example applications
│   └── integration-tests/    # Integration tests
├── docs/                     # Documentation
│   ├── architecture/         # Architecture docs
│   ├── guides/               # User and developer guides
│   ├── implementation/       # Implementation details
│   └── testing/              # Testing documentation
├── scripts/                  # Utility scripts
└── it/                       # Additional integration tests
```

Each module has a standard structure:

```
module/
├── README.md                 # Module documentation
├── src/
│   ├── main/
│   │   ├── scala/            # Main source code
│   │   └── resources/        # Resource files
│   └── test/
│       ├── scala/            # Test source code
│       └── resources/        # Test resources
└── target/                   # Build output (generated)
```

## Building and Testing

### Basic Commands

- **Compile the project**:
  ```bash
  sbt compile
  ```

- **Run tests**:
  ```bash
  sbt test
  ```

- **Run integration tests**:
  ```bash
  sbt it/test
  ```

- **Run specific module tests**:
  ```bash
  sbt "core/test"
  sbt "agents/test"
  ```

- **Generate documentation**:
  ```bash
  sbt doc
  ```

- **Clean build artifacts**:
  ```bash
  sbt clean
  ```

### Advanced Build Options

- **Continuous compilation**:
  ```bash
  sbt ~compile
  ```

- **Cross-compilation**:
  ```bash
  sbt +compile
  ```

- **Run with specific JVM options**:
  ```bash
  sbt -J-Xmx4G compile
  ```

## Development Workflow

### Feature Development Process

1. **Create an issue** in the issue tracker describing the feature
2. **Assign yourself** to the issue
3. **Create a branch** from `main` with a descriptive name:
   ```bash
   git checkout -b feature/your-feature-name
   ```
4. **Develop your feature** following the coding standards
5. **Write tests** for your feature
6. **Run all tests** to ensure nothing was broken
7. **Create a pull request**
8. **Address review feedback**
9. Once approved, **merge your changes**

### Code Review Process

- All changes must be reviewed by at least one team member
- Reviewers should check for:
  - Adherence to coding standards
  - Test coverage
  - Performance implications
  - Documentation
- Address all review comments before merging

### Branch Strategy

- `main`: Main development branch
- `feature/*`: Feature development branches
- `bugfix/*`: Bug fix branches
- `release/*`: Release preparation branches

## Coding Standards

ZIO Agentic AI Framework follows strict coding standards to ensure code quality and consistency:

### Scala Guidelines

- Follow the [Scala Style Guide](https://docs.scala-lang.org/style/)
- Use Scala 3 syntax and features
- Prefer immutable data structures
- Write pure functions where possible
- Properly handle errors with ZIO effects

### ZIO Guidelines

- Use ZIO for all effectful code
- Properly type effects with appropriate error and environment types
- Follow ZIO best practices for resource management
- Use ZIO layers for dependency injection

### Documentation

- Document all public APIs with ScalaDoc
- Include examples in documentation
- Update relevant documentation when making changes

### Examples

Good code example:

```scala
/** 
 * Processes a message and returns a response.
 *
 * @param message The input message to process
 * @return A ZIO effect that produces a response string or an error
 */
def processMessage(message: String): ZIO[Any, ProcessingError, String] = {
  for {
    _ <- ZIO.logInfo(s"Processing message: $message")
    result <- computeResponse(message)
      .tapError(err => ZIO.logError(s"Error processing message: ${err.message}"))
  } yield result
}
```

## Testing Guidelines

ZIO Agentic AI Framework requires comprehensive testing for all code:

### Test Types

- **Unit Tests**: Test individual components in isolation
- **Integration Tests**: Test interactions between components
- **Property-Based Tests**: Test properties that should hold for all inputs
- **Performance Tests**: Test performance characteristics

### Test Structure

- Use ZIO Test for all tests
- Write descriptive test names
- Follow the Arrange-Act-Assert pattern
- Isolate tests from each other

### Example Test

```scala
object MessageProcessorSpec extends ZIOSpecDefault {
  def spec = suite("MessageProcessor")(
    test("processMessage returns correct response for valid input") {
      for {
        processor <- MessageProcessor.make
        result <- processor.processMessage("Hello")
      } yield assertTrue(result.contains("processed"))
    },
    test("processMessage handles errors correctly") {
      for {
        processor <- MessageProcessor.make
        result <- processor.processMessage("").exit
      } yield assertTrue(result.isFailure)
    }
  )
}
```

## Debugging

### Logging

ROO uses ZIO Logging for structured logging:

```scala
import zio.logging._

val program = for {
  _ <- ZIO.logInfo("Starting application")
  _ <- ZIO.logDebug("Debug information")
  result <- computation
  _ <- ZIO.logInfo(s"Computation result: $result")
} yield result
```

Configure logging levels in `application.conf`:

```hocon
zio.logging {
  level = INFO
  format = simple
}
```

### Debugging Integration Tests

For debugging integration tests:

1. Add logging statements to identify issues
2. Run with increased logging level:
   ```bash
   sbt -Dzio.logging.level=DEBUG "integration-tests/testOnly com.agenticai.it.YourTest"
   ```
3. Use the ZIO Test Debug aspect for detailed execution tracing

### Common Debugging Scenarios

- **Test Timeout**: Tests that timeout often indicate deadlocks or long-running operations
- **Resource Leaks**: Check for unclosed resources or missing finalizers
- **Concurrency Issues**: Use ZIO Test's `TestClock` to control timing

## Common Issues

### Build Issues

- **Dependency Resolution**: If SBT fails to resolve dependencies, try:
  ```bash
  sbt clean update
  ```

- **Compilation Errors**: Check import statements and ensure you're using compatible versions

- **Test Failures**: Isolate failing tests with:
  ```bash
  sbt "testOnly *YourTestName"
  ```

### IDE Issues

- **IntelliJ Not Recognizing Imports**: Invalidate caches and restart
- **VS Code/Metals Indexing Problems**: Delete `.metals` and `.bloop` directories and restart

### Runtime Issues

- **StackOverflowError**: Check for infinite recursion or excessive call depth
- **OutOfMemoryError**: Increase JVM heap size or check for memory leaks
- **Performance Problems**: Use ZIO ZIO metrics or JVM profiling tools

## Resources

### Internal Documentation

- [Architecture Overview](../architecture/README.md)
- [Component Relationships](../architecture/ComponentRelationships.md)
- [Testing Summary](../testing/README.md)
- [LLM Integration](../implementation/Langchain4jIntegration.md)

### External Resources

- [Scala Documentation](https://docs.scala-lang.org/)
- [ZIO Documentation](https://zio.dev/documentation/)
- [ZIO Test Documentation](https://zio.dev/zio-test/)
- [Scala Style Guide](https://docs.scala-lang.org/style/)

### Community

- [ROO GitHub Discussions](https://github.com/agenticai/zio-agentic-ai-framework/discussions)
- [ROO Slack Channel](https://agenticai.slack.com)
- [ZIO Discord](https://discord.gg/zio)