# ZIO Agentic AI Framework API Documentation

**Version:** 1.0.0  
**Last Updated:** April 19, 2025  
**Author:** ZIO Agentic AI Framework Team  

## Overview

This directory contains comprehensive API documentation for the ZIO Agentic AI Framework's public interfaces. Each module's API is documented separately, with details on available types, functions, classes, and how they should be used.

## Module APIs

| Module | Description | Link |
|--------|-------------|------|
| Core | Foundational data structures and utilities | [Core API](Core.md) |
| Agents | Agent capabilities and lifecycle | [Agents API](Agents.md) |
| Memory | Persistent and temporary storage | [Memory API](Memory.md) |
| Mesh | Distributed agent communication | [Mesh API](Mesh.md) |
| HTTP | HTTP server and client capabilities | [HTTP API](HTTP.md) |
| Langchain4j | LLM integration and API wrappers | [Langchain4j API](Langchain4j.md) |

## Using the API Documentation

Each module's API documentation follows a consistent structure:

1. **Overview**: A brief description of the module's purpose and features
2. **Main Types**: The primary types and classes exposed by the module
3. **Core Functions**: The most commonly used functions and their behaviors
4. **Examples**: Code examples showing how to use the API
5. **Extension Points**: How to extend the module's functionality
6. **Error Handling**: Common errors and how to handle them
7. **Best Practices**: Recommendations for effective use of the API
8. **Version History**: API changes across versions

## API Stability

The ZIO Agentic AI Framework follows Semantic Versioning:

- **Stable APIs**: Interfaces marked as `@stable` are safe to use and will maintain backward compatibility within the same major version.
- **Experimental APIs**: Interfaces marked as `@experimental` may change in minor versions.
- **Internal APIs**: Packages with `.internal` in their name are not part of the public API and may change at any time.

## Generating Updated API Documentation

The API documentation can be regenerated using:

```bash
sbt doc
```

This will generate Scaladoc for all modules. The generated documentation will be available in each module's `target/scala-3.3.1/api` directory.

## Contributing to API Documentation

When contributing to the API documentation:

1. Follow the established format and style
2. Include examples for all public interfaces
3. Document parameters, return types, and exceptions
4. Explain complex concepts with diagrams where appropriate

See the [CONTRIBUTING.md](../../CONTRIBUTING.md) file for more details on the contribution process.