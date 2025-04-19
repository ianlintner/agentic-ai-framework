# Application and Integration Testing Guide

This document details the approach to testing applications and integrations built on the Agentic AI Framework.

## Overview

Application and integration tests verify that components work together correctly in realistic scenarios. These tests focus on:

- End-to-end functionality
- Integration between subsystems
- Real-world use cases
- API boundaries and contracts
- File system interactions
- Self-modifying capabilities

## Test Structure

Application tests are found in various locations depending on their focus:

1. `src/test/scala/com/agenticai/example/` - Core application tests
2. `modules/examples/src/test/` - Example application tests
3. `modules/core/src/test/` - Core module tests including circuit pattern demos

## Self-Modifying CLI Tests

The `SelfModifyingCLISpec` tests a practical application that demonstrates self-modifying code capabilities.

### Command Parsing

Tests that the CLI correctly parses user commands:

```scala
test("parseCommand correctly identifies commands") {
  val cli = SelfModifyingCLI

  assertTrue(
    cli.parseCommand("help") == SelfModifyingCLI.Command.Help,
    cli.parseCommand("show test.scala") == SelfModifyingCLI.Command.Show("test.scala"),
    cli.parseCommand("modify test.scala add a comment") == SelfModifyingCLI.Command.Modify("test.scala", "add a comment"),
    cli.parseCommand("restore test.scala") == SelfModifyingCLI.Command.Restore("test.scala"),
    cli.parseCommand("exit") == SelfModifyingCLI.Command.Exit
  )
}
```

### File Operations

Tests that the application can safely modify files with backup capabilities:

```scala
test("FileModificationService creates backups and validates paths") {
  val service = new FileModificationService()
  val timestamp = System.currentTimeMillis().toString
  val testDir = new File(System.getProperty("java.io.tmpdir"), s"test_dir_backup_${timestamp}")
  val testFile = new File(testDir, "test_backup.txt")
  val testContent = "test content for backups test"

  ZIO.scoped {
    for {
      // Setup in a scope to ensure cleanup happens
      _ <- ZIO.acquireRelease(
        ZIO.attemptBlocking {
          // Create test directory and file
          testDir.mkdirs()
          Files.write(testFile.toPath, testContent.getBytes)
          testDir
        }
      )(_ => ZIO.attemptBlocking {
        // Cleanup
        testDir.listFiles().foreach(_.delete())
        testDir.delete()
      }.orDie)

      // Test backup creation through writeFile
      _ <- service.writeFile(testFile, "modified content for backup test")
      backupFiles <- ZIO.attemptBlocking {
        testDir.listFiles().filter(_.getName.matches(s"${testFile.getName}\\.\\d{8}_\\d{6}\\.bak"))
      }

      // Test restore
      _ <- service.restoreFromBackup(backupFiles(0))
      restoredContent <- service.readFile(testFile)
    } yield assertTrue(restoredContent == testContent)
  }
}
```

### Self-Modification

Tests that the application can modify its own source code:

```scala
test("should modify its own source code") {
  val service = new FileModificationService()
  val timestamp = java.util.UUID.randomUUID().toString
  val testDir = new File(System.getProperty("java.io.tmpdir"), s"test_dir_$timestamp")
  val testFile = new File(testDir, "test.txt")

  ZIO.scoped {
    // Test setup, modify, and restore operations
    for {
      dirAndFile <- ZIO.acquireRelease(
        ZIO.attemptBlocking {
          // Setup test file
          testDir.mkdirs()
          Files.write(testFile.toPath, "test content for cli test".getBytes)
          (testDir, testFile)
        }
      )(dirAndFile => ZIO.attemptBlocking {
        // Cleanup
        val (dir, _) = dirAndFile
        dir.listFiles().foreach(_.delete())
        dir.delete()
      }.orDie)

      // Modify file
      _ <- service.writeFile(testFile, "modified content for cli test")

      // Find backup files
      backupFiles <- ZIO.attemptBlocking {
        testDir.listFiles().filter(_.getName.matches(s"${testFile.getName}\\.\\d{8}_\\d{6}\\.bak"))
      }

      // Restore from backup
      _ <- service.restoreFromBackup(backupFiles(0))
      restoredContent <- service.readFile(testFile)
    } yield assertTrue(restoredContent == "test content for cli test")
  }
}
```

## Integration Test Approaches

The application tests demonstrate several key integration testing approaches:

### Resource Management

Tests properly manage resources using ZIO's scoped resource pattern:

```scala
ZIO.scoped {
  for {
    resource <- ZIO.acquireRelease(
      // Acquire resource
      ZIO.attemptBlocking { /* setup */ }
    )(
      // Release resource (guaranteed cleanup)
      _ => ZIO.attemptBlocking { /* cleanup */ }.orDie
    )

    // Use the resource safely
    result <- testOperation(resource)
  } yield assertTrue(result == expected)
}
```

### Error Handling

Tests verify proper error handling in applications:

```scala
test("should handle file not found errors") {
  val service = new FileModificationService()
  val nonExistentFile = new File("/path/to/nonexistent/file.txt")

  for {
    result <- service.readFile(nonExistentFile).exit
  } yield assertTrue(result.isFailure)
}
```

### File System Safety

Tests ensure file operations are safe and don't leak resources:

```scala
test("should clean up temporary files") {
  val service = new FileModificationService()
  val tempFile = File.createTempFile("test", ".txt")

  for {
    _ <- service.writeFile(tempFile, "test content")
    _ <- service.deleteFile(tempFile)
    exists <- ZIO.attemptBlocking(tempFile.exists())
  } yield assertTrue(!exists)
}
```

## Web Dashboard Tests

Tests for the web dashboard components verify the web interface works correctly:

```scala
test("dashboard should render agent states") {
  for {
    dashboard <- WebDashboard.make
    agent1 <- Agent.make("agent1")
    agent2 <- Agent.make("agent2")

    // Register agents with the dashboard
    _ <- dashboard.registerAgent(agent1)
    _ <- dashboard.registerAgent(agent2)

    // Get rendered HTML
    html <- dashboard.renderAgentStates
  } yield assertTrue(
    html.contains("agent1") && 
    html.contains("agent2")
  )
}
```

## Agent Examples Tests

Tests for example agents ensure they behave as expected:

```scala
test("chat agent should respond to messages") {
  for {
    agent <- ChatAgent.make
    response <- agent.chat("Hello, how are you?")
  } yield assertTrue(response.nonEmpty)
}
```

## Test Design Philosophy

Application and integration tests follow these principles:

1. **Realistic Scenarios**: Tests use real-world scenarios that reflect actual usage
2. **Complete Workflows**: Tests verify entire workflows from start to finish
3. **Resource Safety**: Tests ensure resources are properly acquired and released
4. **Error Recovery**: Tests verify applications can recover from errors
5. **Independence**: Tests are independent and don't rely on external state

## Running Application Tests

Execute the application tests using SBT:

```bash
# Run core example tests
sbt "testOnly com.agenticai.example.*"

# Run specific test
sbt "testOnly com.agenticai.example.SelfModifyingCLISpec"

# Run with live file system access
sbt -Dfile.io=live "testOnly com.agenticai.example.*"
```

## Testing Temporary File Operations

Many application tests involve temporary files. The framework provides utilities for safe temporary file testing:

```scala
import com.agenticai.testing.TempFileSupport

test("file operations") {
  TempFileSupport.withTempDirectory { dir =>
    val testFile = new File(dir, "test.txt")
    // Test operations on the file
    // Directory is automatically cleaned up after the test
  }
}
```

## Testing Self-Modifying Code

Testing self-modifying code requires special care:

1. **Isolation**: Tests must run in an isolated environment
2. **Backup Verification**: Tests must verify backup functionality works correctly
3. **Restore Verification**: Tests must verify restore functionality works correctly
4. **Error Handling**: Tests must verify errors don't corrupt code

## End-to-End Testing

End-to-end tests verify complete workflows across multiple components:

```scala
test("end-to-end agent workflow") {
  for {
    memory <- MemorySystem.make
    llm <- VertexAIClient.make(VertexAIConfig.claudeDefault)

    // Create agents with memory and LLM
    agentA <- Agent.make("Agent A", memory, llm)
    agentB <- Agent.make("Agent B", memory, llm)

    // Set up communication
    _ <- agentA.connectTo(agentB)

    // Execute workflow
    _ <- agentA.sendInstruction("Collect data and send to Agent B")
    response <- agentB.awaitMessage.timeout(5.seconds)
  } yield assertTrue(response.isDefined)
}
```

## Testing External Service Integration

Tests for external service integration use mock servers for reliable testing:

```scala
test("external API integration") {
  MockServer.withServer { server =>
    // Configure mock responses
    server.whenRequest()
          .matching(path("/api/data"))
          .respond()
          .withBody("""{"status": "success", "data": [1, 2, 3]}""")

    for {
      client <- ApiClient.make(server.baseUrl)
      response <- client.fetchData()
    } yield assertTrue(response.status == "success")
  }
}
```

## Performance Testing

Performance tests verify the application meets performance requirements:

```scala
test("should handle high message volume") {
  for {
    system <- MessagingSystem.make
    // Send 10,000 messages
    startTime <- ZIO.clockWith(_.instant)
    _ <- ZIO.foreachPar(1 to 10000) { i =>
      system.sendMessage(s"test message $i")
    }
    endTime <- ZIO.clockWith(_.instant)
    duration = java.time.Duration.between(startTime, endTime).toMillis
  } yield assertTrue(duration < 5000) // Should complete in under 5 seconds
}
```

## Testing Best Practices

Application and integration testing follows several best practices:

1. **Deterministic Tests**: Tests should be deterministic and not flaky
2. **Clean Start/Clean End**: Tests start and end with a clean state
3. **Realistic Data**: Tests use realistic data scenarios
4. **Failure Injection**: Tests deliberately inject failures to verify recovery
5. **Logging Verification**: Tests verify log output when relevant
6. **Configuration Testing**: Tests verify behavior with different configurations

## Test Coverage Assessment

The current application test coverage includes:

- Self-modifying CLI functionality
- File system operations with backup and restore
- Basic web dashboard functionality
- Example agent behavior

Areas that could benefit from additional testing:

1. More comprehensive end-to-end workflows
2. Long-running stability tests
3. Cross-platform compatibility tests
4. Larger scale performance tests
5. Security-focused tests
