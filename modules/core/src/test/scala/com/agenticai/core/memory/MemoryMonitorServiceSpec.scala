package com.agenticai.core.memory

import zio.*
import zio.test.*
import zio.test.Assertion.*
import zio.test.TestAspect.*
import java.time.{Duration as JavaDuration, Instant}

object MemoryMonitorServiceSpec extends ZIOSpecDefault:

  override def spec = suite("MemoryMonitorService")(
    test("should monitor memory usage") {
      for
        // Create a memory system
        memorySystem <- MemorySystem.make

        // Create a monitor service
        monitorService <- MemoryMonitorService.makeWithSystem(memorySystem)

        // Create test cells
        cell1 <- memorySystem.createCell[String]("cell1")
        cell2 <- memorySystem.createCell[String]("cell2")
        cell3 <- memorySystem.createCell[String]("cell3")

        // Write data to cells
        _ <- cell1.write("Test data 1")
        _ <- cell2.write("Test data with longer content 2")
        _ <- cell3.write("Another test data 3")

        // Add tags to cells
        _ <- cell1.addTag("test")
        _ <- cell2.addTag("test")
        _ <- cell3.addTag("important")

        // Get initial metrics
        initialMetrics <- monitorService.getMetrics

        // Get metrics again to ensure they're collected
        secondMetrics <- monitorService.getMetrics

        // Get metrics after collection
        afterFirstMetrics <- monitorService.getMetrics

        // Get historical metrics
        historicalMetrics <- monitorService.getHistoricalMetrics(Instant.EPOCH, Instant.now())

        // Get statistics
        statistics <- monitorService.getStatistics
      yield assert(initialMetrics.totalCells)(equalTo(3)) &&
      assert(initialMetrics.cellsByTag.get("test"))(isSome(equalTo(2))) &&
      assert(initialMetrics.cellsByTag.get("important"))(isSome(equalTo(1))) &&
      assert(historicalMetrics.nonEmpty)(isTrue) &&
      assert(statistics.collectionCount)(isGreaterThanEqualTo(1))
    },
    test("should handle cleanup based on size") {
      for
        // Create a memory system
        memorySystem <- MemorySystem.make

        // Register a size-based cleanup strategy
        sizeStrategy = CleanupStrategy.sizeBasedCleanup(100L) // Clean cells larger than 100 bytes
        _ <- memorySystem.registerCleanupStrategy(sizeStrategy)

        // Create a monitor service
        monitorService <- MemoryMonitorService.makeWithSystem(memorySystem)

        // Create test cells
        smallCell <- memorySystem.createCell[String]("small")
        largeCell <- memorySystem.createCell[String]("large")

        // Write data to cells - one small, one large
        _ <- smallCell.write("Small data")
        _ <- largeCell.write("X" * 200) // Much larger than 100 bytes

        // Get initial metrics
        initialMetrics <- monitorService.getMetrics

        // Run cleanup manually
        cleanupCount <- memorySystem.runCleanup

        // Get metrics after cleanup
        afterCleanupMetrics <- monitorService.getMetrics

        // Read cells to see if the large one was emptied
        smallValue <- smallCell.read
        largeValue <- largeCell.read
      yield assert(initialMetrics.totalCells)(equalTo(2)) &&
      assert(cleanupCount)(isGreaterThan(0)) &&
      assert(smallValue.isDefined)(isTrue) &&
      assert(largeValue.isEmpty)(isTrue)
    },
    test("should track memory statistics over time") {
      for
        // Create a memory system
        memorySystem <- MemorySystem.make

        // Create a monitor service
        monitorService <- MemoryMonitorService.makeWithSystem(memorySystem)

        // Set thresholds
        _ <- monitorService.setSizeThreshold(1000L)
        _ <- monitorService.setCountThreshold(10)

        // Create test cells in stages
        cell1 <- memorySystem.createCell[String]("cell1")
        _     <- cell1.write("Test data 1")

        // Get metrics at first stage
        _ <- monitorService.getMetrics

        // Create more cells
        cell2 <- memorySystem.createCell[String]("cell2")
        cell3 <- memorySystem.createCell[String]("cell3")
        _     <- cell2.write("Test data with longer content 2")
        _     <- cell3.write("Another test data 3")

        // Get metrics at second stage
        _ <- monitorService.getMetrics

        // Get statistics
        statistics <- monitorService.getStatistics
      yield assert(statistics.maxCount)(isGreaterThanEqualTo(3)) &&
      assert(statistics.minCount)(isLessThanEqualTo(3))
    },
    test("should handle cleanup strategies") {
      for
        // Create a memory system
        memorySystem <- MemorySystem.make

        // Register multiple cleanup strategies
        sizeStrategy = CleanupStrategy.sizeBasedCleanup(100L)
        timeStrategy = CleanupStrategy.timeBasedAccess(JavaDuration.ofSeconds(5))
        tagStrategy  = CleanupStrategy.tagBasedCleanup(Set("temporary"))

        _ <- memorySystem.registerCleanupStrategy(sizeStrategy)
        _ <- memorySystem.registerCleanupStrategy(timeStrategy)
        _ <- memorySystem.registerCleanupStrategy(tagStrategy)

        // Create a monitor service
        monitorService <- MemoryMonitorService.makeWithSystem(memorySystem)

        // Create test cells
        normalCell <- memorySystem.createCell[String]("normal")
        largeCell  <- memorySystem.createCell[String]("large")
        tempCell   <- memorySystem.createCell[String]("temp")

        // Write data to cells
        _ <- normalCell.write("Normal data")
        _ <- largeCell.write("X" * 200) // Exceeds the size threshold
        _ <- tempCell.write("Temporary data")

        // Add tags to cells
        _ <- tempCell.addTag("temporary")

        // Run cleanup
        cleanupCount <- memorySystem.runCleanup

        // Get metrics after cleanup
        afterCleanupMetrics <- monitorService.getMetrics

        // Read cells to see if they were emptied
        normalValue <- normalCell.read
        largeValue  <- largeCell.read
        tempValue   <- tempCell.read
      yield assert(cleanupCount)(isGreaterThan(0)) &&
      assert(normalValue.isDefined)(isTrue) &&
      assert(largeValue.isEmpty || tempValue.isEmpty)(isTrue)
    }
  ) @@ timeout(30.seconds) @@ sequential
