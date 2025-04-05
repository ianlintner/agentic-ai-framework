package com.agenticai.core.memory

import zio._
import zio.test._
import zio.test.Assertion._
import java.time.{Duration => JavaDuration, Instant}
import zio.test.TestAspect._
import java.util.concurrent.TimeUnit

object CleanupStrategySpec extends ZIOSpecDefault {
  def spec = suite("CleanupStrategy")(
    test("timeBasedAccess should mark old cells for cleanup") {
      for {
        // Create a cell with default access time
        cell <- MemoryCell.make("test value")
        
        // Create a strategy that cleans up cells older than 1 minute
        strategy <- ZIO.succeed(CleanupStrategy.timeBasedAccess(JavaDuration.ofMinutes(1)))
        
        // Advance the test clock by 2 minutes to make the cell "old"
        _ <- TestClock.adjust(Duration.fromMillis(2 * 60 * 1000))
        
        // Check if the strategy marks the cell for cleanup
        shouldCleanup <- strategy.shouldCleanup(cell)
        
        // Log values for debugging
        _ <- ZIO.logDebug(s"Should cleanup: $shouldCleanup")
        meta <- cell.getMetadata
        now <- ZIO.clockWith(_.instant)
        _ <- ZIO.logDebug(s"lastAccessed: ${meta.lastAccessed}, now: $now, diff: ${JavaDuration.between(meta.lastAccessed, now).toMillis}ms")
      } yield assertTrue(shouldCleanup)
    },
    
    test("timeBasedAccess should not mark recent cells for cleanup") {
      for {
        // Create a cell with recent access time
        cell <- MemoryCell.make("test value")
        // Read the cell to update the access time
        _ <- cell.read
        // Create a strategy that cleans up cells older than 1 hour
        strategy <- ZIO.succeed(CleanupStrategy.timeBasedAccess(JavaDuration.ofHours(1)))
        // Check if the strategy marks the cell for cleanup
        shouldCleanup <- strategy.shouldCleanup(cell)
      } yield assertTrue(!shouldCleanup)
    },
    
    test("timeBasedModification should mark cells not modified recently for cleanup") {
      for {
        // Create a cell
        cell <- MemoryCell.make("test value")
        
        // Create a strategy that cleans up cells not modified for 1 minute
        strategy <- ZIO.succeed(CleanupStrategy.timeBasedModification(JavaDuration.ofMinutes(1)))
        
        // Advance the test clock by 2 minutes to make the cell "old"
        _ <- TestClock.adjust(Duration.fromMillis(2 * 60 * 1000))
        
        // Check if the strategy marks the cell for cleanup
        shouldCleanup <- strategy.shouldCleanup(cell)
        
        // Log values for debugging
        _ <- ZIO.logDebug(s"Should cleanup: $shouldCleanup")
        meta <- cell.getMetadata
        now <- ZIO.clockWith(_.instant)
        _ <- ZIO.logDebug(s"lastModified: ${meta.lastModified}, now: $now, diff: ${JavaDuration.between(meta.lastModified, now).toMillis}ms")
      } yield assertTrue(shouldCleanup)
    },
    
    test("sizeBasedCleanup should mark large cells for cleanup") {
      for {
        // Create a cell with a large value
        cell <- MemoryCell.make("initial")
        // Write a large value to update the size
        largeValue = "a" * 10000 // 10KB string
        _ <- cell.write(largeValue)
        // Create a strategy that cleans up cells larger than 5KB
        strategy <- ZIO.succeed(CleanupStrategy.sizeBasedCleanup(5000))
        // Check if the strategy marks the cell for cleanup
        shouldCleanup <- strategy.shouldCleanup(cell)
      } yield assertTrue(shouldCleanup)
    },
    
    test("sizeBasedCleanup should not mark small cells for cleanup") {
      for {
        // Create a cell with a small value
        cell <- MemoryCell.make("initial")
        // Write a small value to update the size
        smallValue = "small"
        _ <- cell.write(smallValue)
        // Create a strategy that cleans up cells larger than 5KB
        strategy <- ZIO.succeed(CleanupStrategy.sizeBasedCleanup(5000))
        // Check if the strategy marks the cell for cleanup
        shouldCleanup <- strategy.shouldCleanup(cell)
      } yield assertTrue(!shouldCleanup)
    },
    
    test("tagBasedCleanup should mark cells with specific tags for cleanup") {
      for {
        // Create a cell with specific tags
        cell <- MemoryCell.makeWithTags("test value", Set("temp", "cache"))
        // Create a strategy that cleans up cells with the 'temp' tag
        strategy <- ZIO.succeed(CleanupStrategy.tagBasedCleanup(Set("temp")))
        // Check if the strategy marks the cell for cleanup
        shouldCleanup <- strategy.shouldCleanup(cell)
      } yield assertTrue(shouldCleanup)
    },
    
    test("tagBasedCleanup should not mark cells without specific tags for cleanup") {
      for {
        // Create a cell with different tags
        cell <- MemoryCell.makeWithTags("test value", Set("permanent", "important"))
        // Create a strategy that cleans up cells with the 'temp' tag
        strategy <- ZIO.succeed(CleanupStrategy.tagBasedCleanup(Set("temp")))
        // Check if the strategy marks the cell for cleanup
        shouldCleanup <- strategy.shouldCleanup(cell)
      } yield assertTrue(!shouldCleanup)
    },
    
    test("any combinator should mark cells for cleanup if any strategy matches") {
      for {
        // Create a cell with specific tags and small size
        cell <- MemoryCell.makeWithTags("small", Set("permanent", "important"))
        // Write a value to update the size
        _ <- cell.write("small")
        // Create strategies
        sizeStrategy <- ZIO.succeed(CleanupStrategy.sizeBasedCleanup(5000)) // Won't match
        tagStrategy <- ZIO.succeed(CleanupStrategy.tagBasedCleanup(Set("important"))) // Will match
        // Combine strategies with OR logic
        combinedStrategy <- ZIO.succeed(CleanupStrategy.any(sizeStrategy, tagStrategy))
        // Check if the combined strategy marks the cell for cleanup
        shouldCleanup <- combinedStrategy.shouldCleanup(cell)
      } yield assertTrue(shouldCleanup)
    },
    
    test("all combinator should mark cells for cleanup only if all strategies match") {
      for {
        // Create a cell with specific tags and large size
        cell <- MemoryCell.makeWithTags("initial", Set("temp", "cache"))
        // Write a large value to update the size
        largeValue = "a" * 10000 // 10KB string
        _ <- cell.write(largeValue)
        // Create strategies
        sizeStrategy <- ZIO.succeed(CleanupStrategy.sizeBasedCleanup(5000)) // Will match
        tagStrategy <- ZIO.succeed(CleanupStrategy.tagBasedCleanup(Set("temp"))) // Will match
        // Combine strategies with AND logic
        combinedStrategy <- ZIO.succeed(CleanupStrategy.all(sizeStrategy, tagStrategy))
        // Check if the combined strategy marks the cell for cleanup
        shouldCleanup <- combinedStrategy.shouldCleanup(cell)
      } yield assertTrue(shouldCleanup)
    },
    
    test("all combinator should not mark cells for cleanup if any strategy doesn't match") {
      for {
        // Create a cell with specific tags but small size
        cell <- MemoryCell.makeWithTags("initial", Set("temp", "cache"))
        // Write a small value to update the size
        _ <- cell.write("small")
        // Create strategies
        sizeStrategy <- ZIO.succeed(CleanupStrategy.sizeBasedCleanup(5000)) // Won't match
        tagStrategy <- ZIO.succeed(CleanupStrategy.tagBasedCleanup(Set("temp"))) // Will match
        // Combine strategies with AND logic
        combinedStrategy <- ZIO.succeed(CleanupStrategy.all(sizeStrategy, tagStrategy))
        // Check if the combined strategy marks the cell for cleanup
        shouldCleanup <- combinedStrategy.shouldCleanup(cell)
      } yield assertTrue(!shouldCleanup)
    }
  ) @@ TestAspect.sequential
}