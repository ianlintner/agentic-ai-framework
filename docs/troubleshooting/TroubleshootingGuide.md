# ZIO Agentic AI Framework Troubleshooting Guide

**Version:** 1.0.0  
**Last Updated:** April 19, 2025  
**Author:** ZIO Agentic AI Framework Team  

## Table of Contents

1. [Introduction](#introduction)
2. [General Troubleshooting](#general-troubleshooting)
3. [Build and Compilation Issues](#build-and-compilation-issues)
4. [Core Module Issues](#core-module-issues)
5. [Agent Module Issues](#agent-module-issues)
6. [Memory Module Issues](#memory-module-issues)
7. [Mesh Module Issues](#mesh-module-issues)
8. [HTTP Module Issues](#http-module-issues)
9. [Langchain4j Module Issues](#langchain4j-module-issues)
10. [Test Failures](#test-failures)
11. [Performance Issues](#performance-issues)
12. [Integration Issues](#integration-issues)
13. [Deployment Issues](#deployment-issues)
14. [Getting Help](#getting-help)

## Introduction

This guide provides solutions for common issues encountered when working with the ZIO Agentic AI Framework. It covers problems ranging from build failures to runtime issues and is organized by module to help you quickly find relevant information.

## General Troubleshooting

### Logging

Enable detailed logging to diagnose issues:

```scala
// In your application
import zio.logging._

val program = for {
  _ <- ZIO.logDebug("Detailed information for debugging")
  result <- yourOperation
} yield result

// Run with appropriate log level
val runtimeWithLogging = Runtime.default.configure(
  _.copy(logLevel = LogLevel.Debug)
)
```

Configure logging in `application.conf`:

```hocon
zio.logging {
  level = DEBUG  # Options: TRACE, DEBUG, INFO, WARNING, ERROR, OFF
  format = colored
}
```

### Diagnostic Information

Collect diagnostic information to help with troubleshooting:

```bash
# System information
java -version
sbt sbtVersion
scala -version

# Project dependencies
sbt "show allDependencies"

# Configuration
sbt "show configuration"
```

### Common ZIO Issues

1. **Fiber Leaks**: Use the ZIO Trace aspect for debugging

   ```scala
   import zio.test.TestAspect

   test("potentially leaking test") {
     yourProgram
   } @@ TestAspect.diagnose
   ```

2. **Deadlocks**: Ensure you're not blocking in ZIO effects

   ```scala
   // Bad - can cause deadlocks
   ZIO.succeed {
     someBlockingOperation()  // Blocks the fiber
   }

   // Good - use blocking for blocking operations
   ZIO.blocking {
     someBlockingOperation()
   }
   ```

## Build and Compilation Issues

### Dependency Resolution Problems

**Symptom**: SBT fails to resolve dependencies with errors like "unresolved dependency".

**Solution**:

1. Clean and update dependencies:
   ```bash
   sbt clean update
   ```

2. Check for conflicting versions:
   ```bash
   sbt "show evicted"
   ```

3. If specific libraries are causing issues, try forcing a version:
   ```scala
   // In build.sbt
   libraryDependencies += "com.example" %% "library" % "1.2.3" force()
   ```

### Compilation Errors

**Symptom**: Compilation fails with errors.

**Solutions**:

1. **Type errors**: Check for type mismatches and ensure all types align, especially in ZIO effects.

2. **Missing imports**: Ensure all needed types are imported.

3. **Incompatible Scala versions**: Verify that libraries are compatible with your Scala version:
   ```bash
   sbt "show scalaVersion"
   ```

4. **IDE-related issues**: Invalidate caches or restart the IDE.

### Missing Classes at Runtime

**Symptom**: `ClassNotFoundException` or `NoClassDefFoundError` at runtime.

**Solutions**:

1. Ensure the class is in your classpath.

2. Check for version conflicts between runtime and compile-time dependencies.

3. Verify dependency scopes are correct (e.g., "provided" vs "compile").

## Core Module Issues

### ZIO Effect Composition Problems

**Symptom**: Issues with combining ZIO effects or unexpected behavior in effect composition.

**Solutions**:

1. Check effect types for compatibility:
   ```scala
   // Example of properly typed effects
   val effect1: ZIO[Any, IOException, String] = ZIO.succeed("hello")
   val effect2: ZIO[Any, NumberFormatException, Int] = ZIO.succeed(42)
   
   // Combining effects with different error types
   val combined = effect1.zipWith(effect2.mapError(e => e: Exception))((s, i) => s"$s $i")
   ```

2. For environment type issues, use proper combination strategies:
   ```scala
   // Effects with different environment requirements
   val effect1: ZIO[Logging, Nothing, Unit] = ZIO.serviceWithZIO(_.log("Message"))
   val effect2: ZIO[Database, Nothing, User] = ZIO.serviceWithZIO(_.getUser(1))
   
   // Combine environments
   val combined: ZIO[Logging with Database, Nothing, User] = 
     for {
       _ <- effect1
       user <- effect2
     } yield user
   ```

### Concurrency Issues

**Symptom**: Race conditions, deadlocks, or unexpected behavior in concurrent code.

**Solutions**:

1. Use proper concurrency primitives:
   ```scala
   // For shared state, use Ref
   for {
     ref <- Ref.make(0)
     _ <- ZIO.foreachPar(1 to 100)(_ => ref.update(_ + 1))
     result <- ref.get
   } yield result
   
   // For coordination, use Promise
   for {
     promise <- Promise.make[Nothing, Int]
     _ <- promise.succeed(42).fork
     result <- promise.await
   } yield result
   ```

2. For deadlocks, ensure resources are acquired and released properly:
   ```scala
   // Use ZIO.acquireReleaseWith for safe resource management
   ZIO.acquireReleaseWith(
     acquire = openResource,
     release = closeResource,
     use = useResource
   )
   ```

## Agent Module Issues

### Agent Initialization Failures

**Symptom**: Agents fail to initialize with errors like "Capability not found" or "Initialization timeout".

**Solutions**:

1. Ensure all required capabilities are added before initialization:
   ```scala
   for {
     agent <- Agent.create("agent-id")
     
     // Add all required capabilities
     _ <- agent.addCapability(MemoryCapability.default)
     _ <- agent.addCapability(LLMCapability.default)
     
     // Then initialize
     _ <- agent.initialize
   } yield agent
   ```

2. Check capability dependencies are available in the environment:
   ```scala
   // Provide all necessary dependencies
   val program = for {
     agent <- createAndInitializeAgent
     result <- agent.run(input)
   } yield result
   
   program.provide(
     MemoryLive.layer,
     LLMServiceLive.layer,
     ToolRegistryLive.layer
   )
   ```

### Agent Communication Failures

**Symptom**: Agents cannot communicate or messages are lost.

**Solutions**:

1. Verify mesh configuration and connectivity:
   ```scala
   // Check mesh status
   for {
     mesh <- ZIO.service[MeshService]
     status <- mesh.status
     _ <- ZIO.logInfo(s"Mesh status: $status")
   } yield ()
   ```

2. Ensure message format is correct:
   ```scala
   // Properly format messages
   val message = AgentMessage(
     source = "agent-1",
     destination = "agent-2",
     content = "Hello",
     metadata = Map("type" -> "greeting")
   )
   ```

3. Implement retry logic for reliable delivery:
   ```scala
   // Retry sending message with backoff
   mesh.sendMessage(message)
     .retry(Schedule.exponential(100.milliseconds) && Schedule.recurs(5))
   ```

## Memory Module Issues

### Storage Backend Connection Failures

**Symptom**: Memory module fails to connect to storage backend.

**Solutions**:

1. Check connection settings:
   ```hocon
   # In application.conf
   memory {
     backend = "database"
     database {
       url = "jdbc:postgresql://localhost:5432/roo"
       username = "roo"
       password = "password"
     }
   }
   ```

2. Verify backend service is running:
   ```bash
   # For database backends
   docker ps | grep postgres
   
   # For Redis
   redis-cli ping
   ```

3. Test connection with diagnostic tools:
   ```scala
   for {
     memory <- ZIO.service[MemoryService]
     health <- memory.healthCheck
     _ <- ZIO.logInfo(s"Memory health: $health")
   } yield ()
   ```

### Data Persistence Issues

**Symptom**: Data not saved or retrieved correctly.

**Solutions**:

1. Check for serialization issues:
   ```scala
   // Ensure data is serializable
   case class MyData(field: String) derives JsonCodec
   
   // Check serialization/deserialization works
   val data = MyData("test")
   val json = data.toJson
   val decoded = json.fromJson[MyData]
   ```

2. Verify key structure and namespacing:
   ```scala
   // Use structured keys
   val key = MemoryKey(
     scope = "agent-1",
     category = "conversations",
     id = "12345"
   )
   ```

3. Implement transaction safety:
   ```scala
   // Use transactions for related operations
   memory.transaction {
     for {
       _ <- memory.store(key1, value1)
       _ <- memory.store(key2, value2)
     } yield ()
   }
   ```

## Mesh Module Issues

### Node Discovery Problems

**Symptom**: Nodes cannot discover each other in the mesh.

**Solutions**:

1. Check network configuration:
   ```hocon
   # In application.conf
   mesh {
     binding {
       host = "0.0.0.0"  # Listen on all interfaces
       port = 8080
     }
     discovery {
       mechanism = "multicast"
       multicast {
         group = "224.0.0.1"
         port = 8081
       }
     }
   }
   ```

2. Verify firewall settings allow discovery traffic:
   ```bash
   # Check if ports are open
   sudo lsof -i :8080
   sudo lsof -i :8081
   ```

3. Test discovery directly:
   ```scala
   for {
     mesh <- ZIO.service[MeshService]
     nodes <- mesh.discoverNodes
     _ <- ZIO.foreach(nodes)(n => ZIO.logInfo(s"Found node: $n"))
   } yield ()
   ```

### Message Routing Issues

**Symptom**: Messages not reaching intended recipients.

**Solutions**:

1. Check recipient is registered in the mesh:
   ```scala
   for {
     mesh <- ZIO.service[MeshService]
     isRegistered <- mesh.isRegistered("agent-id")
     _ <- ZIO.logInfo(s"Agent registered: $isRegistered")
   } yield ()
   ```

2. Verify message format and addressing:
   ```scala
   // Ensure correct destination format
   val message = MeshMessage(
     destination = NodeAddress("node-1", "agent-1"),
     payload = messageContent,
     options = MessageOptions(priority = Priority.Normal)
   )
   ```

3. Monitor message flow with tracing:
   ```scala
   // Enable message tracing
   mesh.sendMessage(message, TracingOptions(enabled = true))
   ```

## HTTP Module Issues

### Server Binding Failures

**Symptom**: HTTP server fails to bind to port.

**Solutions**:

1. Check if the port is already in use:
   ```bash
   # Check port usage
   sudo lsof -i :8080
   
   # Find and kill the process
   kill -9 <PID>
   ```

2. Try binding to a different port:
   ```scala
   // Specify a different port
   val server = HttpServer.builder
     .port(8081)  // Changed from 8080
     .routes(routes)
     .build
   ```

3. Use privileged ports safely:
   ```scala
   // For ports below 1024
   HttpServer.builder
     .port(80)
     .withPrivilegedPortHandler
     .routes(routes)
     .build
   ```

### Request/Response Handling Issues

**Symptom**: HTTP requests fail or return unexpected responses.

**Solutions**:

1. Debug request/response cycle:
   ```scala
   // Add logging middleware
   val loggingMiddleware = Middleware { handler =>
     Handler { request =>
       for {
         _ <- ZIO.logInfo(s"Received request: ${request.method} ${request.path}")
         response <- handler(request)
         _ <- ZIO.logInfo(s"Sending response: ${response.status}")
       } yield response
     }
   }
   
   val server = HttpServer.builder
     .middleware(loggingMiddleware)
     .routes(routes)
     .build
   ```

2. Check for content type mismatches:
   ```scala
   // Ensure content type is set correctly
   Response.json(json).withHeader("Content-Type", "application/json")
   ```

3. Verify serialization works correctly:
   ```scala
   // Test serialization
   case class User(name: String, age: Int) derives JsonCodec
   val user = User("John", 30)
   val json = user.toJson
   ```

## Langchain4j Module Issues

### LLM Provider Connection Issues

**Symptom**: Cannot connect to LLM providers like OpenAI or Anthropic.

**Solutions**:

1. Check API keys and environment variables:
   ```bash
   # Verify environment variables are set
   echo $OPENAI_API_KEY
   echo $ANTHROPIC_API_KEY
   ```

2. Test provider connection directly:
   ```scala
   for {
     llm <- ZIO.service[LLMService]
     health <- llm.healthCheck
     _ <- ZIO.logInfo(s"LLM health: $health")
   } yield ()
   ```

3. Verify network connectivity:
   ```bash
   # Check connectivity to provider
   curl -I https://api.openai.com
   curl -I https://api.anthropic.com
   ```

### Response Parsing Issues

**Symptom**: LLM responses cannot be parsed or structured output is incorrect.

**Solutions**:

1. Check output parser configuration:
   ```scala
   // Specify expected output structure
   val parser = OutputParser.structuredJson[List[String]]
   
   // Use parser with LLM
   llm.complete(prompt, OutputOptions(parser = Some(parser)))
   ```

2. Verify prompt structure for structured output:
   ```scala
   // Add clear output instructions
   val prompt = """
     |Please list three fruits.
     |
     |Output format:
     |```json
     |["fruit1", "fruit2", "fruit3"]
     |```
   """.stripMargin
   ```

3. Implement error handling for malformed responses:
   ```scala
   llm.complete(prompt)
     .map(parser.parse)
     .catchAll { error =>
       ZIO.logError(s"Failed to parse: $error") *>
       ZIO.succeed(fallbackValue)
     }
   ```

## Test Failures

### ZIO Test Timeouts

**Symptom**: Tests fail with timeout errors.

**Solutions**:

1. Increase test timeout:
   ```scala
   test("slow test") {
     slowOperation
   } @@ TestAspect.timeout(60.seconds)
   ```

2. For tests using the test clock, ensure fibers are suspending:
   ```scala
   test("test with clock") {
     for {
       fiber <- ZIO.sleep(1.second).fork
       _ <- TestClock.adjust(1.second)
       result <- fiber.join
     } yield result
   }
   ```

3. Use the diagnose aspect to identify non-suspending fibers:
   ```scala
   test("potentially problematic test") {
     potentiallyNonSuspendingCode
   } @@ TestAspect.diagnose
   ```

### Flaky Tests

**Symptom**: Tests pass sometimes but fail other times.

**Solutions**:

1. Identify race conditions:
   ```scala
   // Use test retry to prove flakiness
   test("flaky test") {
     flakyOperation
   } @@ TestAspect.flaky
   ```

2. Control randomness with fixed seeds:
   ```scala
   test("test with randomness") {
     randomizedOperation
   } @@ TestAspect.withSeed(123L)
   ```

3. Isolate tests for interactions:
   ```scala
   // Run test in isolation
   test("isolated test") {
     sensitiveToPreviousTests
   } @@ TestAspect.sequential
   ```

## Performance Issues

### High Latency

**Symptom**: Operations take longer than expected.

**Solutions**:

1. Enable performance tracing:
   ```scala
   // Add performance tracing
   val tracedOperation = for {
     _ <- ZIO.logDebug("Starting operation")
     start <- Clock.nanoTime
     result <- yourOperation
     end <- Clock.nanoTime
     _ <- ZIO.logDebug(s"Operation took ${(end - start) / 1000000}ms")
   } yield result
   ```

2. Analyze bottlenecks:
   ```scala
   // Benchmark different parts
   for {
     t1 <- ZIO.withClock(Clock.ClockLive)(yourOperation).timed
     (d1, _) = t1
     
     t2 <- ZIO.withClock(Clock.ClockLive)(alternativeImplementation).timed
     (d2, _) = t2
     
     _ <- ZIO.logInfo(s"Original: ${d1.toMillis}ms, Alternative: ${d2.toMillis}ms")
   } yield ()
   ```

3. Check for resource leaks:
   ```scala
   // Monitor resource usage
   for {
     before <- ZIO.succeed(Runtime.getRuntime.freeMemory())
     _ <- yourOperation
     after <- ZIO.succeed(Runtime.getRuntime.freeMemory())
     _ <- ZIO.logInfo(s"Memory used: ${(before - after) / 1024 / 1024}MB")
   } yield ()
   ```

### High Memory Usage

**Symptom**: Application consumes more memory than expected.

**Solutions**:

1. Review data structures for efficiency:
   ```scala
   // Use efficient data structures
   // Instead of:
   val inefficient = List.tabulate(1000000)(i => (s"key-$i", i))
   
   // Consider:
   val efficient = Map.from(List.tabulate(1000000)(i => (s"key-$i", i)))
   ```

2. Implement streaming for large data:
   ```scala
   // Use ZIO Stream for large data
   ZStream.fromIterator(largeIterator)
     .map(processItem)
     .runCollect
   ```

3. Tune JVM memory settings:
   ```bash
   # Run with appropriate memory settings
   sbt -J-Xmx4G -J-Xms2G
   ```

## Integration Issues

### Cross-Module Integration Problems

**Symptom**: Modules don't work together as expected.

**Solutions**:

1. Check module versioning:
   ```bash
   # Verify all modules are on compatible versions
   sbt "show version"
   ```

2. Inspect module initialization order:
   ```scala
   // Ensure correct initialization order
   val program = for {
     _ <- ZIO.logInfo("Starting application")
     _ <- CoreModule.initialize
     _ <- MemoryModule.initialize
     _ <- AgentModule.initialize
   } yield ()
   ```

3. Test integrations incrementally:
   ```scala
   // Test modules in isolation first
   test("module A works") { moduleATest }
   test("module B works") { moduleBTest }
   test("modules A and B work together") { integrationTest }
   ```

### External System Integration Issues

**Symptom**: Integration with external systems fails.

**Solutions**:

1. Verify API compatibility:
   ```scala
   // Check external system version
   for {
     client <- ZIO.service[ExternalClient]
     version <- client.getVersion
     _ <- ZIO.logInfo(s"External system version: $version")
   } yield ()
   ```

2. Implement circuit breakers for resilience:
   ```scala
   // Add circuit breaker
   val cbSettings = CircuitBreakerSettings(
     maxFailures = 5,
     resetTimeout = 1.minute
   )
   val protectedCall = CircuitBreaker.make(cbSettings).use(
     externalSystemCall
   )
   ```

3. Mock external systems for testing:
   ```scala
   // Test with mocked external system
   val mockedSystem = new ExternalSystem {
     def call(input: String): Task[String] = 
       ZIO.succeed("mocked response")
   }
   
   val testProgram = yourProgram.provide(ZLayer.succeed(mockedSystem))
   ```

## Deployment Issues

### Container Deployment Problems

**Symptom**: Application fails to run properly in containers.

**Solutions**:

1. Check container resource limits:
   ```yaml
   # In docker-compose.yml or Kubernetes manifest
   resources:
     limits:
       memory: "2Gi"
       cpu: "1"
     requests:
       memory: "1Gi"
       cpu: "0.5"
   ```

2. Verify environment variables:
   ```bash
   # List environment variables in container
   docker exec -it container-name env
   ```

3. Inspect logs for container-specific issues:
   ```bash
   # View container logs
   docker logs container-name
   
   # For Kubernetes
   kubectl logs pod-name
   ```

### Configuration Issues

**Symptom**: Application uses incorrect configuration in different environments.

**Solutions**:

1. Use environment-specific configuration:
   ```scala
   // Load configuration based on environment
   val config = ConfigProvider.fromHoconFile("application.conf")
     .orElse(ConfigProvider.fromHoconFile(s"application.${env}.conf"))
     .load(AppConfig.descriptor)
   ```

2. Validate configuration at startup:
   ```scala
   // Validate configuration
   for {
     config <- ZIO.service[AppConfig]
     _ <- ZIO.foreach(config.validate) { error =>
       ZIO.logError(s"Configuration error: $error")
     }
     _ <- ZIO.when(config.hasErrors)(ZIO.fail(new RuntimeException("Invalid configuration")))
   } yield ()
   ```

3. Log effective configuration:
   ```scala
   // Log configuration at startup
   for {
     config <- ZIO.service[AppConfig]
     _ <- ZIO.logInfo(s"Using configuration: ${config.toDebugString}")
   } yield ()
   ```

## Getting Help

If you're unable to resolve an issue using this guide:

1. **Community Forums**: Post a question in the ROO GitHub Discussions or Discord channel.

2. **Issue Tracker**: File an issue on the GitHub repository with detailed reproduction steps.

3. **Contributing**: If you fix an issue, consider contributing the fix back to the project.

For more information, contact the ROO team at [support@agenticai.org](mailto:support@agenticai.org).