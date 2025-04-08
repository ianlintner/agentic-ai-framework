package com.agenticai.core.memory

import zio._
import zio.test._
import zio.test.Assertion._
import zio.test.TestAspect._
import java.time.{Duration => JavaDuration, Instant}

object CleanupStrategySpec extends ZIOSpecDefault {
  
  override def spec = suite("CleanupStrategy")(
    
    test("timeBasedAccess should mark cells for cleanup based on access time") {
      for {
        // Create a memory system and cells
        memorySystem <- MemorySystem.make
        cell <- memorySystem.createCell[String]("test-cell")
        
        // Write initial data
        _ <- cell.write("test data")
        
        // Create a strategy that cleans up cells not accessed for 5 seconds
        strategy = CleanupStrategy.timeBasedAccess(JavaDuration.ofSeconds(5))
        
        // Check initially (should not be marked for cleanup)
        initialShouldCleanup <- strategy.shouldCleanup(cell)
        
        // Advance time by 6 seconds
        _ <- TestClock.adjust(6.seconds)
        
        // Check after time advancement (should be marked for cleanup)
        afterShouldCleanup <- strategy.shouldCleanup(cell)
        
        // Read the cell to update access time
        _ <- cell.read
        
        // Check after access (should not be marked for cleanup)
        afterAccessShouldCleanup <- strategy.shouldCleanup(cell)
      } yield {
        assert(initialShouldCleanup)(isFalse) &&
        assert(afterShouldCleanup)(isTrue) &&
        assert(afterAccessShouldCleanup)(isFalse)
      }
    },
    
    test("timeBasedModification should mark cells for cleanup based on modification time") {
      for {
        // Create a memory system and cells
        memorySystem <- MemorySystem.make
        cell <- memorySystem.createCell[String]("test-cell")
        
        // Write initial data
        _ <- cell.write("test data")
        
        // Create a strategy that cleans up cells not modified for 5 seconds
        strategy = CleanupStrategy.timeBasedModification(JavaDuration.ofSeconds(5))
        
        // Check initially (should not be marked for cleanup)
        initialShouldCleanup <- strategy.shouldCleanup(cell)
        
        // Advance time by 6 seconds
        _ <- TestClock.adjust(6.seconds)
        
        // Check after time advancement (should be marked for cleanup)
        afterShouldCleanup <- strategy.shouldCleanup(cell)
        
        // Read the cell (should not affect modification time)
        _ <- cell.read
        afterReadShouldCleanup <- strategy.shouldCleanup(cell)
        
        // Modify the cell
        _ <- cell.write("updated data")
        
        // Check after modification (should not be marked for cleanup)
        afterModificationShouldCleanup <- strategy.shouldCleanup(cell)
      } yield {
        assert(initialShouldCleanup)(isFalse) &&
        assert(afterShouldCleanup)(isTrue) &&
        assert(afterReadShouldCleanup)(isTrue) &&
        assert(afterModificationShouldCleanup)(isFalse)
      }
    },
    
    test("sizeBasedCleanup should mark cells for cleanup based on size") {
      for {
        // Create a memory system and cells
        memorySystem <- MemorySystem.make
        smallCell <- memorySystem.createCell[String]("small-cell")
        largeCell <- memorySystem.createCell[String]("large-cell")
        
        // Write data of different sizes
        _ <- smallCell.write("small data")
        _ <- largeCell.write("large " * 100) // Much larger data
        
        // Create a strategy that cleans up cells larger than 50 bytes
        strategy = CleanupStrategy.sizeBasedCleanup(50)
        
        // Check both cells
        smallCellShouldCleanup <- strategy.shouldCleanup(smallCell)
        largeCellShouldCleanup <- strategy.shouldCleanup(largeCell)
      } yield {
        assert(smallCellShouldCleanup)(isFalse) &&
        assert(largeCellShouldCleanup)(isTrue)
      }
    },
    
    test("tagBasedCleanup should mark cells for cleanup based on tags") {
      for {
        // Create a memory system and cells
        memorySystem <- MemorySystem.make
        cell1 <- memorySystem.createCell[String]("cell1")
        cell2 <- memorySystem.createCell[String]("cell2")
        cell3 <- memorySystem.createCell[String]("cell3")
        
        // Add tags
        _ <- cell1.addTag("important")
        _ <- cell2.addTag("temporary")
        _ <- cell3.addTag("important")
        _ <- cell3.addTag("temporary")
        
        // Create a strategy that cleans up cells with the "temporary" tag
        strategy = CleanupStrategy.tagBasedCleanup(Set("temporary"))
        
        // Check all cells
        cell1ShouldCleanup <- strategy.shouldCleanup(cell1)
        cell2ShouldCleanup <- strategy.shouldCleanup(cell2)
        cell3ShouldCleanup <- strategy.shouldCleanup(cell3)
      } yield {
        assert(cell1ShouldCleanup)(isFalse) &&
        assert(cell2ShouldCleanup)(isTrue) &&
        assert(cell3ShouldCleanup)(isTrue)
      }
    },
    
    test("any combinator should combine strategies with OR logic") {
      for {
        // Create a memory system and cells
        memorySystem <- MemorySystem.make
        cell1 <- memorySystem.createCell[String]("cell1") // Tagged with "important"
        cell2 <- memorySystem.createCell[String]("cell2") // Large data
        cell3 <- memorySystem.createCell[String]("cell3") // Both large and tagged with "temporary"
        cell4 <- memorySystem.createCell[String]("cell4") // Neither large nor tagged
        
        // Add data and tags
        _ <- cell1.addTag("important")
        _ <- cell1.write("small data")
        
        _ <- cell2.write("large " * 100)
        
        _ <- cell3.addTag("temporary")
        _ <- cell3.write("large " * 100)
        
        _ <- cell4.write("small data")
        
        // Create strategies
        sizeStrategy = CleanupStrategy.sizeBasedCleanup(50)
        tagStrategy = CleanupStrategy.tagBasedCleanup(Set("temporary"))
        
        // Combine strategies with OR logic
        combinedStrategy = CleanupStrategy.any(sizeStrategy, tagStrategy)
        
        // Check all cells
        cell1ShouldCleanup <- combinedStrategy.shouldCleanup(cell1)
        cell2ShouldCleanup <- combinedStrategy.shouldCleanup(cell2)
        cell3ShouldCleanup <- combinedStrategy.shouldCleanup(cell3)
        cell4ShouldCleanup <- combinedStrategy.shouldCleanup(cell4)
      } yield {
        assert(cell1ShouldCleanup)(isFalse) &&
        assert(cell2ShouldCleanup)(isTrue) &&
        assert(cell3ShouldCleanup)(isTrue) &&
        assert(cell4ShouldCleanup)(isFalse)
      }
    },
    
    test("all combinator should combine strategies with AND logic") {
      for {
        // Create a memory system and cells
        memorySystem <- MemorySystem.make
        cell1 <- memorySystem.createCell[String]("cell1") // Tagged with "important"
        cell2 <- memorySystem.createCell[String]("cell2") // Large data
        cell3 <- memorySystem.createCell[String]("cell3") // Both large and tagged with "temporary"
        cell4 <- memorySystem.createCell[String]("cell4") // Neither large nor tagged
        
        // Add data and tags
        _ <- cell1.addTag("important")
        _ <- cell1.write("small data")
        
        _ <- cell2.write("large " * 100)
        
        _ <- cell3.addTag("temporary")
        _ <- cell3.write("large " * 100)
        
        _ <- cell4.write("small data")
        
        // Create strategies
        sizeStrategy = CleanupStrategy.sizeBasedCleanup(50)
        tagStrategy = CleanupStrategy.tagBasedCleanup(Set("temporary"))
        
        // Combine strategies with AND logic
        combinedStrategy = CleanupStrategy.all(sizeStrategy, tagStrategy)
        
        // Check all cells
        cell1ShouldCleanup <- combinedStrategy.shouldCleanup(cell1)
        cell2ShouldCleanup <- combinedStrategy.shouldCleanup(cell2)
        cell3ShouldCleanup <- combinedStrategy.shouldCleanup(cell3)
        cell4ShouldCleanup <- combinedStrategy.shouldCleanup(cell4)
      } yield {
        assert(cell1ShouldCleanup)(isFalse) &&
        assert(cell2ShouldCleanup)(isFalse) &&
        assert(cell3ShouldCleanup)(isTrue) &&
        assert(cell4ShouldCleanup)(isFalse)
      }
    }
  ) @@ timeout(10.seconds) @@ sequential
}
