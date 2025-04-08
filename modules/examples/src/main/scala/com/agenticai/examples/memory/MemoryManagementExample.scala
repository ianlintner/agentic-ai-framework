package com.agenticai.examples.memory

import com.agenticai.core.memory._
import zio._
import java.io.File
import java.time.{Duration => JavaDuration, Instant}
import java.time.temporal.ChronoUnit

/**
 * Example demonstrating memory management, monitoring, and cleanup features
 */
object MemoryManagementExample extends ZIOAppDefault {
  
  private val logMemoryMetrics = (metrics: MemoryMetrics) => {
    Console.printLine(s"""
      |=== Memory Metrics ===
      |Total Cells: ${metrics.totalCells}
      |Total Size: ${metrics.totalSize} bytes
      |Average Size: ${metrics.averageSize} bytes
      |Largest Cell: ${metrics.largestCell} bytes
      |Smallest Cell: ${metrics.smallestCell} bytes
      |Cells by Tag: ${metrics.cellsByTag.map { case (tag, count) => s"$tag: $count" }.mkString(", ")}
      |Timestamp: ${metrics.timestamp}
      |====================
      |""".stripMargin)
  }
  
  private val logStatistics = (stats: MemoryStatistics) => {
    Console.printLine(s"""
      |=== Memory Statistics ===
      |Average Size: ${stats.averageSize} bytes
      |Min Size: ${stats.minSize} bytes
      |Max Size: ${stats.maxSize} bytes
      |Average Cell Count: ${stats.averageCount}
      |Min Cell Count: ${stats.minCount}
      |Max Cell Count: ${stats.maxCount}
      |Collection Count: ${stats.collectionCount}
      |Monitoring Period: ${stats.startTime} to ${stats.endTime}
      |====================
      |""".stripMargin)
  }
  
  // Example program
  override def run = {
    program.catchAll(e => Console.printLine(s"Error: ${e.getMessage}").exitCode)
  }
  
  val program = for {
    // 1. Create both memory systems
    inMemorySystem <- MemorySystem.make
    baseDir = new File("./tmp/memory-example")
    _ <- ZIO.attempt(baseDir.mkdirs())
    persistentSystem <- MemorySystem.makePersistent(baseDir)
    
    // 2. Set up monitoring for both systems
    inMemoryMonitor <- MemoryMonitorService.makeWithSystem(inMemorySystem)
    persistentMonitor <- MemoryMonitorService.makeWithSystem(persistentSystem)
    
    // 3. Configure and enable automatic monitoring
    _ <- inMemoryMonitor.enablePeriodicCollection(JavaDuration.ofSeconds(5))
    _ <- persistentMonitor.enablePeriodicCollection(JavaDuration.ofSeconds(5))
    
    // 4. Set monitoring thresholds
    _ <- inMemoryMonitor.setSizeThreshold(10000)
    _ <- inMemoryMonitor.setCountThreshold(50)
    
    // 5. Set up cleanup strategies for both systems
    timeStrategy = CleanupStrategy.timeBasedAccess(JavaDuration.ofMinutes(10))
    sizeStrategy = CleanupStrategy.sizeBasedCleanup(1000)
    tagStrategy = CleanupStrategy.tagBasedCleanup(Set("temporary"))
    combinedStrategy = CleanupStrategy.any(timeStrategy, tagStrategy)
    
    _ <- inMemorySystem.registerCleanupStrategy(combinedStrategy)
    _ <- persistentSystem.registerCleanupStrategy(sizeStrategy)
    
    // 6. Enable automatic cleanup
    _ <- inMemorySystem.enableAutomaticCleanup(JavaDuration.ofMinutes(5))
    _ <- persistentSystem.enableAutomaticCleanup(JavaDuration.ofMinutes(5))
    
    // 7. Create and populate memory cells with various tags and sizes
    _ <- Console.printLine("=== Creating Memory Cells ===")
    
    // In-memory cells
    smallCell <- inMemorySystem.createCell[String]("small")
    mediumCell <- inMemorySystem.createCell[String]("medium")
    largeCell <- inMemorySystem.createCell[String]("large")
    
    // Persistent cells
    configCell <- persistentSystem.createCellWithTags[Map[String, String]](
      Map("api_key" -> "secret", "endpoint" -> "https://api.example.com"),
      Set("config", "important")
    )
    tempDataCell <- persistentSystem.createCellWithTags[List[Int]](
      List(1, 2, 3, 4, 5),
      Set("temporary", "data")
    )
    
    // 8. Write data to cells
    _ <- smallCell.write("Small cell content")
    _ <- mediumCell.write("Medium cell content " * 10)
    _ <- largeCell.write("Large cell content " * 100)
    
    // Add tags
    _ <- smallCell.addTag("small")
    _ <- mediumCell.addTag("medium")
    _ <- largeCell.addTag("large")
    _ <- mediumCell.addTag("important")
    
    // 9. Get initial metrics
    _ <- Console.printLine("\n=== Initial Memory Metrics ===")
    inMemoryMetrics <- inMemoryMonitor.getMetrics
    persistentMetrics <- persistentMonitor.getMetrics
    
    _ <- logMemoryMetrics(inMemoryMetrics)
    _ <- logMemoryMetrics(persistentMetrics)
    
    // 10. Demonstrate manual cleanup
    _ <- Console.printLine("\n=== Running Manual Cleanup ===")
    inMemoryCleanupCount <- inMemorySystem.runCleanup(tagStrategy)
    persistentCleanupCount <- persistentSystem.runCleanup(sizeStrategy)
    
    _ <- Console.printLine(s"In-memory cleanup removed $inMemoryCleanupCount cells based on tags")
    _ <- Console.printLine(s"Persistent cleanup removed $persistentCleanupCount cells based on size")
    
    // 11. Get metrics after cleanup
    _ <- Console.printLine("\n=== Memory Metrics After Cleanup ===")
    inMemoryMetricsAfterCleanup <- inMemoryMonitor.getMetrics
    persistentMetricsAfterCleanup <- persistentMonitor.getMetrics
    
    _ <- logMemoryMetrics(inMemoryMetricsAfterCleanup)
    _ <- logMemoryMetrics(persistentMetricsAfterCleanup)
    
    // 12. Wait for a bit to collect more metrics
    _ <- Console.printLine("\n=== Waiting for 10 seconds for more metrics collection ===")
    _ <- ZIO.sleep(10.seconds)
    
    // 13. Get historical metrics and statistics
    _ <- Console.printLine("\n=== Memory Statistics ===")
    tenMinutesAgo = Instant.now().minus(10, ChronoUnit.MINUTES)
    
    inMemoryStats <- inMemoryMonitor.getStatistics
    persistentStats <- persistentMonitor.getStatistics
    
    _ <- Console.printLine("In-memory System Statistics:")
    _ <- logStatistics(inMemoryStats)
    
    _ <- Console.printLine("Persistent System Statistics:")
    _ <- logStatistics(persistentStats)
    
    // 14. Disable monitoring and cleanup
    _ <- Console.printLine("\n=== Disabling Monitoring and Cleanup ===")
    _ <- inMemoryMonitor.disablePeriodicCollection
    _ <- persistentMonitor.disablePeriodicCollection
    _ <- inMemorySystem.disableAutomaticCleanup
    _ <- persistentSystem.disableAutomaticCleanup
    
    // 15. Clean up
    _ <- Console.printLine("\n=== Cleaning Up Resources ===")
    _ <- inMemorySystem.clearAll
    _ <- persistentSystem.clearAll
    
    _ <- Console.printLine("\n=== Example Complete ===")
  } yield ExitCode.success
}