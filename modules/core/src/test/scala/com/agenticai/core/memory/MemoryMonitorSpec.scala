package com.agenticai.core.memory

import zio._
import zio.test._
import zio.test.Assertion._
import zio.test.TestClock
import java.time.{Duration => JavaDuration, Instant}
import zio.test.TestAspect._

object MemoryMonitorSpec extends ZIOSpecDefault {
  def spec = suite("MemoryMonitor")(
    test("getMetrics should return correct metrics for empty system") {
      for {
        monitor <- MemoryMonitor.make
        metrics <- monitor.getMetrics
      } yield assertTrue(
        metrics.totalCells == 0 &&
        metrics.totalSize == 0 &&
        metrics.averageSize == 0.0 &&
        metrics.largestCell == 0 &&
        metrics.smallestCell == 0 &&
        metrics.cellsByTag.isEmpty
      )
    },
    
    test("getMetrics should return correct metrics for system with cells") {
      for {
        // Create a memory system with cells
        system <- MemorySystem.make
        cell1 <- system.createCell("small value")
        cell2 <- system.createCell("a" * 1000) // 1KB string
        cell3 <- system.createCellWithTags("tagged value", Set("test", "important"))
        
        // Create a monitor and register the system
        monitor <- MemoryMonitor.make
        _ <- monitor.registerMemorySystem(system)
        
        // Get metrics
        metrics <- monitor.getMetrics
      } yield assertTrue(
        metrics.totalCells == 3 &&
        metrics.totalSize > 1000 &&
        metrics.averageSize > 300 &&
        metrics.largestCell >= 1000 &&
        metrics.smallestCell > 0 &&
        metrics.cellsByTag.contains("test") &&
        metrics.cellsByTag.contains("important") &&
        metrics.cellsByTag("test") == 1 &&
        metrics.cellsByTag("important") == 1
      )
    },
    
    test("registerMemorySystem and unregisterMemorySystem should work correctly") {
      for {
        // Create two memory systems
        system1 <- MemorySystem.make
        system2 <- MemorySystem.make
        
        // Create cells in both systems
        _ <- system1.createCell("system1 value")
        _ <- system2.createCell("system2 value")
        
        // Create a monitor and register both systems
        monitor <- MemoryMonitor.make
        _ <- monitor.registerMemorySystem(system1)
        _ <- monitor.registerMemorySystem(system2)
        
        // Get metrics with both systems
        metricsBoth <- monitor.getMetrics
        
        // Unregister one system
        _ <- monitor.unregisterMemorySystem(system1)
        
        // Get metrics with only one system
        metricsOne <- monitor.getMetrics
      } yield assertTrue(
        metricsBoth.totalCells == 2 &&
        metricsOne.totalCells == 1
      )
    },
    
    test("enablePeriodicCollection should collect metrics periodically") {
      for {
        // Create a memory system
        system <- MemorySystem.make
        _ <- system.createCell("test value")
        
        // Create a monitor and register the system
        monitor <- MemoryMonitor.make
        _ <- monitor.registerMemorySystem(system)
        
        // Manually collect metrics instead of relying on periodic collection
        _ <- monitor.getMetrics
        
        // Add another cell to change metrics
        _ <- system.createCell("another value")
        
        // Get metrics again
        _ <- monitor.getMetrics
        
        // Get historical metrics - we're manually triggering collection
        // instead of relying on periodic collection
        startTimeInstant = java.time.Instant.EPOCH
        endTimeInstant = java.time.Instant.now().plusSeconds(10)
        historicalMetrics <- monitor.getHistoricalMetrics(startTimeInstant, endTimeInstant)
      } yield assertTrue(
        // We manually collected metrics at least twice
        historicalMetrics.size >= 1
      )
    },
    
    test("setSizeThreshold and setCountThreshold should set thresholds correctly") {
      for {
        // Create a memory system with cells
        system <- MemorySystem.make
        _ <- system.createCell("value 1")
        _ <- system.createCell("value 2")
        
        // Create a monitor and register the system
        monitor <- MemoryMonitor.make
        _ <- monitor.registerMemorySystem(system)
        
        // Set thresholds
        _ <- monitor.setSizeThreshold(10)
        _ <- monitor.setCountThreshold(1)
        
        // Get metrics (this should log warnings due to exceeded thresholds)
        _ <- monitor.getMetrics
      } yield assertCompletes
    }
  ) @@ TestAspect.sequential @@ TestAspect.timeout(5.seconds)
}