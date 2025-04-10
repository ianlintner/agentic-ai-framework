package com.agenticai.examples

import com.agenticai.core.memory.*
import zio.*
import java.nio.charset.StandardCharsets
import java.io.File
import java.lang.Runtime
import scala.util.Random

/** Demonstrates the use of compressed memory cells for optimizing memory usage.
  */
object MemoryCompressionDemo extends ZIOAppDefault:

  // Simple string serializer and deserializer for compression
  val stringSerializer: String => Array[Byte]   = _.getBytes(StandardCharsets.UTF_8)
  val stringDeserializer: Array[Byte] => String = bytes => new String(bytes, StandardCharsets.UTF_8)

  // Generate a large random string for testing compression
  def generateLargeString(size: Int): String =
    val chars  = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789"
    val sb     = new StringBuilder(size)
    val random = new Random()

    // Create a string with some repetitive patterns to improve compression
    for i <- 0 until size do
      // Every 100 characters, add a repeating pattern
      if i % 100 == 0 then sb.append("PATTERN_REPEATING_")
      else sb.append(chars.charAt(random.nextInt(chars.length)))

    sb.toString

  // Demonstrate creating and using compressed memory cells
  def demoCompressedMemory: ZIO[Any, Any, Unit] =
    for
      // Generate test data
      smallData  <- ZIO.succeed(generateLargeString(500))   // 500 bytes, below threshold
      mediumData <- ZIO.succeed(generateLargeString(5000))  // 5KB
      largeData  <- ZIO.succeed(generateLargeString(50000)) // 50KB

      // Create memory cells
      _ <- Console.printLine("Creating memory cells...")

      // Regular uncompressed cell
      regularCell <- MemoryCell.make(largeData)

      // Compressed cell with default settings
      compressedCell <- CompressedMemoryCell.make(
        largeData,
        stringSerializer,
        stringDeserializer
      )

      // Compressed cell with custom threshold (compress even small data)
      aggressiveCell <- CompressedMemoryCell.make(
        smallData,
        stringSerializer,
        stringDeserializer,
        compressThreshold = 100 // 100 bytes threshold
      )

      // Get compression stats
      _         <- Console.printLine("\nInitial compression stats:")
      regStats  <- regularCell.metadata.map(m => s"Regular cell size: ${m.size} bytes")
      compStats <- compressedCell.getCompressionStats
      aggrStats <- aggressiveCell.getCompressionStats

      _ <- Console.printLine(regStats)
      _ <- ZIO.foreach(compStats) { stats =>
        Console.printLine(
          s"Compressed cell: ${stats.originalSize} bytes original → " +
            s"${stats.compressedSize} bytes compressed (${stats.compressionRatio}:1 ratio)"
        )
      }
      _ <- ZIO.foreach(aggrStats) { stats =>
        Console.printLine(
          s"Aggressive cell: ${stats.originalSize} bytes original → " +
            s"${stats.compressedSize} bytes compressed (${stats.compressionRatio}:1 ratio)"
        )
      }

      // Update with medium data
      _ <- Console.printLine("\nUpdating cells with medium data (5KB)...")
      _ <- regularCell.write(mediumData)
      _ <- compressedCell.write(mediumData)

      // Get updated stats
      _          <- Console.printLine("\nUpdated compression stats:")
      regStats2  <- regularCell.metadata.map(m => s"Regular cell size: ${m.size} bytes")
      compStats2 <- compressedCell.getCompressionStats

      _ <- Console.printLine(regStats2)
      _ <- ZIO.foreach(compStats2) { stats =>
        Console.printLine(
          s"Compressed cell: ${stats.originalSize} bytes original → " +
            s"${stats.compressedSize} bytes compressed (${stats.compressionRatio}:1 ratio)"
        )
      }

      // Test with persistent memory system
      _       <- Console.printLine("\nTesting with persistent memory system...")
      tempDir <- ZIO.succeed(new File("./temp-memory-demo"))
      _       <- ZIO.succeed(tempDir.mkdirs())

      persistentSystem <- PersistentMemorySystem.make(tempDir)
      _                <- persistentSystem.createCell("uncompressed large data")

      // Create tagged cell with compression in persistent system
      _ <- persistentSystem.createCell("compressed large data")

      // Clean up temp directory when done
      _ <- ZIO.succeed {
        def deleteRecursively(file: File): Unit =
          if file.isDirectory then file.listFiles.foreach(deleteRecursively)
          file.delete()
        Runtime.getRuntime.addShutdownHook(new Thread(() => deleteRecursively(tempDir)))
      }

      _ <- Console.printLine("\nDemo complete. Memory cell compression demonstrated successfully.")
    yield ()

  // Main program
  override def run: ZIO[Any, Any, ExitCode] =
    for
      _ <- Console.printLine("=== Memory Compression Demo ===")
      _ <- demoCompressedMemory.catchAll { error =>
        Console.printLine(s"Error: ${error.toString}")
      }
    yield ExitCode.success
