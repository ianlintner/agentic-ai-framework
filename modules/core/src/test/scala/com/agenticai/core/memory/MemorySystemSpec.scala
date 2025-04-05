package com.agenticai.core.memory

import zio._
import zio.test._
import zio.test.Assertion._
import zio.test.TestAspect._
import java.time.{Duration => JavaDuration}

object MemorySystemSpec extends ZIOSpecDefault {
  def spec = suite("MemorySystem")(
    test("createCell creates a new memory cell") {
      for {
        system <- MemorySystem.make
        cell <- system.createCell("test")
        value <- cell.read
      } yield assertTrue(value.contains("test"))
    },
    test("createCellWithTags creates a cell with tags") {
      for {
        system <- MemorySystem.make
        cell <- system.createCellWithTags("test", Set("tag1", "tag2"))
        meta <- cell.metadata
      } yield assertTrue(meta.tags == Set("tag1", "tag2"))
    },
    test("getCellsByTag returns cells with matching tag") {
      for {
        system <- MemorySystem.make
        cell1 <- system.createCellWithTags("test1", Set("tag1"))
        cell2 <- system.createCellWithTags("test2", Set("tag1", "tag2"))
        cells <- system.getCellsByTag("tag1")
      } yield assertTrue(cells.size == 2 && cells.contains(cell1) && cells.contains(cell2))
    },
    test("getAllCells returns all created cells") {
      for {
        system <- MemorySystem.make
        cell1 <- system.createCell("test1")
        cell2 <- system.createCell("test2")
        cells <- system.getAllCells
      } yield assertTrue(cells.size == 2 && cells.contains(cell1) && cells.contains(cell2))
    },
    test("clearAll clears all cells") {
      for {
        system <- MemorySystem.make
        cell1 <- system.createCell("test1")
        cell2 <- system.createCell("test2")
        _ <- system.clearAll
        cells <- system.getAllCells
      } yield assertTrue(cells.isEmpty)
    },
    
    suite("Cleanup Strategies")(
      test("registerCleanupStrategy registers a strategy") {
        for {
          system <- MemorySystem.make
          strategy = CleanupStrategy.timeBasedAccess(JavaDuration.ofHours(1))
          _ <- system.registerCleanupStrategy(strategy)
          strategies <- system.getCleanupStrategies
        } yield assertTrue(strategies.contains(strategy))
      },
      
      test("unregisterCleanupStrategy removes a strategy") {
        for {
          system <- MemorySystem.make
          strategy = CleanupStrategy.timeBasedAccess(JavaDuration.ofHours(1))
          _ <- system.registerCleanupStrategy(strategy)
          _ <- system.unregisterCleanupStrategy(strategy.name)
          strategies <- system.getCleanupStrategies
        } yield assertTrue(!strategies.contains(strategy))
      },
      test("runCleanup with timeBasedAccess cleans up old cells") {
        for {
          system <- MemorySystem.make
          // Create a cell
          cell <- system.createCell("test")
          
          // Create a strategy that cleans up cells older than 1 second
          strategy = CleanupStrategy.timeBasedAccess(JavaDuration.ofSeconds(1))
          
          // Get initial time for logs
          initialTime <- ZIO.clockWith(_.instant)
          _ <- ZIO.logDebug(s"Initial time: $initialTime")
          
          // Advance the test clock to make the cell "old"
          _ <- TestClock.adjust(Duration.fromMillis(2 * 1000))
          
          // Get time after advancing clock
          afterTime <- ZIO.clockWith(_.instant)
          _ <- ZIO.logDebug(s"After adjusting clock: $afterTime")
          
          // Run cleanup
          count <- system.runCleanup(strategy)
          _ <- ZIO.logDebug(s"Cleaned up $count cells")
          
          // Check that the cell was emptied
          value <- cell.read
          _ <- ZIO.logDebug(s"Cell value after cleanup: $value")
        } yield assertTrue(count == 1 && value.isEmpty)
      },
      
      test("runCleanup with sizeBasedCleanup cleans up large cells") {
        for {
          system <- MemorySystem.make
          // Create a cell with a small initial value
          cell <- system.createCell("initial")
          // Write a large value to ensure size is updated
          largeValue = "a" * 1000 // 1KB string
          _ <- cell.write(largeValue)
          // Verify the cell has the large value
          valueBeforeCleanup <- cell.read
          _ <- ZIO.log(s"Value before cleanup: $valueBeforeCleanup")
          // Get metadata to verify size
          metadata <- cell.getMetadata
          _ <- ZIO.log(s"Cell size: ${metadata.size}")
          // Create a strategy that cleans up cells larger than 100 bytes
          strategy = CleanupStrategy.sizeBasedCleanup(100)
          // Run cleanup
          count <- system.runCleanup(strategy)
          _ <- ZIO.log(s"Cleaned up $count cells")
          // Check that the cell was emptied
          value <- cell.read
        } yield assertTrue(count == 1 && value.isEmpty)
      },
      
      test("runCleanup with tagBasedCleanup cleans up cells with specific tags") {
        for {
          system <- MemorySystem.make
          // Create cells with different tags
          cell1 <- system.createCellWithTags("test1", Set("temp"))
          cell2 <- system.createCellWithTags("test2", Set("permanent"))
          // Create a strategy that cleans up cells with the 'temp' tag
          strategy = CleanupStrategy.tagBasedCleanup(Set("temp"))
          // Run cleanup
          count <- system.runCleanup(strategy)
          // Check that only the cell with the 'temp' tag was emptied
          value1 <- cell1.read
          value2 <- cell2.read
        } yield assertTrue(count == 1 && value1.isEmpty && value2.contains("test2"))
      },
      
      test("makeWithTimeBasedCleanup creates a system with time-based cleanup") {
        for {
          tempDir <- ZIO.attempt(java.nio.file.Files.createTempDirectory("memory-test").toFile)
          system <- MemorySystem.makeWithTimeBasedCleanup(
            baseDir = tempDir,
            maxAge = JavaDuration.ofHours(1),
            interval = JavaDuration.ofMinutes(5)
          )
          strategies <- system.getCleanupStrategies
          // Check that a time-based strategy was registered
          hasTimeBasedStrategy = strategies.exists(_.name.contains("TimeBasedAccess"))
        } yield assertTrue(hasTimeBasedStrategy)
      },
      
      test("makeWithSizeBasedCleanup creates a system with size-based cleanup") {
        for {
          tempDir <- ZIO.attempt(java.nio.file.Files.createTempDirectory("memory-test").toFile)
          system <- MemorySystem.makeWithSizeBasedCleanup(
            baseDir = tempDir,
            maxSize = 1000,
            interval = JavaDuration.ofMinutes(5)
          )
          strategies <- system.getCleanupStrategies
          // Check that a size-based strategy was registered
          hasSizeBasedStrategy = strategies.exists(_.name.contains("SizeBasedCleanup"))
        } yield assertTrue(hasSizeBasedStrategy)
      }
    )
  ) @@ TestAspect.sequential
}
