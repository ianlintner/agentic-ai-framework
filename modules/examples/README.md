# ZIO Agentic AI Framework Examples Module

**Version:** 1.0.0  
**Last Updated:** April 19, 2025  
**Author:** ZIO Agentic AI Framework Team  

## Overview

The Examples module provides practical demonstrations of the ZIO Agentic AI Framework's capabilities through working code examples. These examples serve as both documentation and reference implementations to help developers understand how to use the framework effectively.

## Examples

### Basic Examples

| Example | Description | Key Concepts |
|---------|-------------|--------------|
| [Hello World](src/main/scala/com/agenticai/examples/basic/HelloWorld.scala) | Simple agent that responds to greetings | Agent creation, basic interaction |
| [Tool Usage](src/main/scala/com/agenticai/examples/basic/ToolUsage.scala) | Agent using tools to perform tasks | Tool integration, capability system |
| [Multi-turn Conversation](src/main/scala/com/agenticai/examples/basic/MultiTurnConversation.scala) | Agent maintaining context over multiple interactions | State management, conversation context |

### Advanced Examples

| Example | Description | Key Concepts |
|---------|-------------|--------------|
| [Memory Usage](src/main/scala/com/agenticai/examples/advanced/MemoryUsage.scala) | Agent with persistent memory | Memory integration, information retrieval |
| [Multi-agent Coordination](src/main/scala/com/agenticai/examples/advanced/MultiAgentCoordination.scala) | Multiple agents working together | Agent communication, coordination |
| [Custom Capabilities](src/main/scala/com/agenticai/examples/advanced/CustomCapabilities.scala) | Implementing custom agent capabilities | Capability extension, agent customization |

### Domain-Specific Examples

| Example | Description | Key Concepts |
|---------|-------------|--------------|
| [Question Answering](src/main/scala/com/agenticai/examples/domains/QuestionAnswering.scala) | Agent that answers questions from documents | Document processing, information extraction |
| [Task Planning](src/main/scala/com/agenticai/examples/domains/TaskPlanning.scala) | Agent that plans and executes complex tasks | Planning, sequential processing |
| [Customer Support](src/main/scala/com/agenticai/examples/domains/CustomerSupport.scala) | Customer support workflow simulation | Workflow integration, domain adaptation |

## Running Examples

Each example can be run as a standalone application. Use the provided shell script or run directly with SBT:

```bash
# Using the provided script
./run-example.sh HelloWorld

# Or using SBT directly
sbt "examples/runMain com.agenticai.examples.basic.HelloWorld"
```

## Example Structure

All examples follow a consistent structure:

```
src/main/scala/com/agenticai/examples/
├── basic/          # Basic examples for beginners
├── advanced/       # Advanced usage patterns
├── domains/        # Domain-specific implementations
└── utils/          # Shared utilities for examples
```

## Learning Path

For new users, we recommend following this sequence:

1. Start with the Basic examples to understand core concepts
2. Explore the Advanced examples to learn about more complex features
3. Review the Domain-Specific examples to see practical applications
4. Try modifying examples to experiment with different approaches

## Integration with Documentation

These examples are referenced throughout the ZIO Agentic AI Framework documentation:

- [Getting Started Guide](../../docs/guides/GettingStarted.md) references the Hello World example
- [Agent Development Guide](../../docs/guides/CreatingCustomAgents.md) builds upon the Custom Capabilities example
- [Multi-agent Systems](../../docs/mesh/DistributedAgentMesh.md) incorporates concepts from the Multi-agent Coordination example

## Contributing New Examples

We welcome contributions of new examples! To contribute:

1. Follow the existing structure and naming conventions
2. Include comprehensive comments explaining key concepts
3. Ensure your example is self-contained and runnable
4. Add appropriate test cases
5. Update this README with your new example

See the [CONTRIBUTING.md](../../CONTRIBUTING.md) file for more details on the contribution process.

## Troubleshooting

If you encounter issues running the examples:

- Check the [Troubleshooting Guide](../../docs/troubleshooting/TroubleshootingGuide.md)
- Ensure you're using the correct Scala and SBT versions
- Verify all dependencies are resolved correctly

For specific example-related issues, please file a GitHub issue with the "example" tag.