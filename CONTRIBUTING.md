# Contributing to Agentic AI Framework

Thank you for your interest in contributing to the Agentic AI Framework! This document provides guidelines and instructions for contributing to this project.

## Code of Conduct

Please be respectful and considerate of others when contributing to this project. We aim to foster an inclusive and welcoming community.

## Getting Started

1. Fork the repository on GitHub
2. Clone your fork locally
3. Set up the development environment
4. Run the tests to ensure everything is working properly

```bash
git clone git@github.com:your-username/agentic-ai-framework.git
cd agentic-ai-framework
sbt compile
sbt test
```

## Development Workflow

1. Create a new branch for your feature or bug fix
2. Make your changes
3. Write or modify tests as necessary
4. Ensure all tests pass
5. Commit your changes with clear, descriptive commit messages
6. Push your changes to your fork
7. Submit a pull request

```bash
git checkout -b feature/your-feature-name
# Make your changes
sbt test
git commit -m "Add feature X"
git push origin feature/your-feature-name
```

## Pull Request Process

1. Update the README.md or documentation with details of changes, if applicable
2. Include tests for new features or bug fixes
3. Ensure your code passes all tests
4. Update the CHANGELOG.md with details of changes (for significant contributions)
5. The pull request will be merged once it receives approval from maintainers

## Coding Standards

- Follow the Scala style guidelines
- Write clear, documented, and testable code
- Keep commits focused and atomic
- Use descriptive variable and function names
- Include comments for complex logic

### Scala Style Guidelines

- Use 2 spaces for indentation
- Limit line length to 100 characters
- Use camelCase for variables and methods
- Use PascalCase for classes and objects
- Use descriptive names that reflect the purpose of the variable or function
- Use immutable data structures where possible
- Avoid using `null`, prefer `Option` instead

## Testing

All new features and bug fixes should include tests. We use ZIO Test for unit testing.

```scala
import zio._
import zio.test._
import zio.test.Assertion._

object MySpec extends ZIOSpecDefault {
  def spec = suite("MySpec")(
    test("my test") {
      for {
        result <- MyClass.someFunction()
      } yield assert(result)(equalTo(expectedResult))
    }
  )
}
```

## Documentation

- Update documentation for any changed functionality
- Document public APIs with ScalaDoc comments
- Keep README.md and other documentation up to date with changes

## Questions

If you have any questions or need help with your contribution, please open an issue or reach out to the maintainers.

Thank you for contributing to the Agentic AI Framework!