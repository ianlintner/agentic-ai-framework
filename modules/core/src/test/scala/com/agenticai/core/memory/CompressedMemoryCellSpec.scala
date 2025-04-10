package com.agenticai.core.memory

import zio.*
import zio.test.*
import zio.test.Assertion.*
import zio.test.TestAspect.*
import java.util.UUID
import java.nio.charset.StandardCharsets

object CompressedMemoryCellSpec extends ZIOSpecDefault:

  // Test serializers for String data
  private val stringSerializer: String => Array[Byte] =
    _.getBytes(StandardCharsets.UTF_8)

  private val stringDeserializer: Array[Byte] => String =
    bytes => new String(bytes, StandardCharsets.UTF_8)

  // Create a string of specified size for testing compression ratios
  private def generateLargeString(sizeInKb: Int): String =
    val repeated = "abcdefghijklmnopqrstuvwxyz0123456789"
    val buffer   = new StringBuilder(sizeInKb * 1024)
    for _ <- 0 until (sizeInKb * 1024 / repeated.length) + 1 do buffer.append(repeated)
    buffer.toString.substring(0, sizeInKb * 1024)

  // Generate a highly compressible string (lots of repetition)
  private def generateCompressibleString(sizeInKb: Int): String =
    val repeatedChar = "a"
    repeatedChar * (sizeInKb * 1024)

  // Generate a random string that's less compressible
  private def generateRandomString(sizeInKb: Int): String =
    val random = new scala.util.Random(42) // Fixed seed for reproducibility
    val buffer = new StringBuilder(sizeInKb * 1024)
    for _ <- 0 until sizeInKb * 1024 do buffer.append((random.nextInt(26) + 'a').toChar)
    buffer.toString

  def spec = suite("CompressedMemoryCell")(
    test("should not compress data smaller than threshold") {
      for
        // Create a small string (less than default 1KB threshold)
        smallValue <- ZIO.succeed("This is a small string under 1KB")

        // Create the compressed cell
        cell <- CompressedMemoryCell.make(
          smallValue,
          stringSerializer,
          stringDeserializer
        )

        // Get compression stats
        stats <- cell.getCompressionStats
      yield assertTrue(
        stats.exists(s =>
          s.strategy == "None" &&
            s.compressionRatio == 1.0 &&
            s.originalSize == smallValue.getBytes(StandardCharsets.UTF_8).length
        )
      )
    },
    test("should compress data larger than threshold") {
      for
        // Create a 10KB string with high compressibility
        largeValue <- ZIO.succeed(generateCompressibleString(10))

        // Create the compressed cell
        cell <- CompressedMemoryCell.make(
          largeValue,
          stringSerializer,
          stringDeserializer
        )

        // Get compression stats
        stats <- cell.getCompressionStats
      yield assertTrue(
        stats.exists(s =>
          s.strategy == "GZIP" &&
            s.compressionRatio > 1.0 &&
            s.originalSize == largeValue.getBytes(StandardCharsets.UTF_8).length &&
            s.compressedSize < s.originalSize
        )
      )
    },
    test("should correctly read and write compressed values") {
      for
        // Create two different large strings
        initialValue <- ZIO.succeed(generateCompressibleString(5))
        newValue     <- ZIO.succeed(generateCompressibleString(8))

        // Create the compressed cell
        cell <- CompressedMemoryCell.make(
          initialValue,
          stringSerializer,
          stringDeserializer
        )

        // Read initial value
        readInitial <- cell.read

        // Write new value
        _ <- cell.write(newValue)

        // Read new value
        readNew <- cell.read

        // Get updated compression stats
        statsAfterWrite <- cell.getCompressionStats
      yield assertTrue(
        readInitial.contains(initialValue) &&
          readNew.contains(newValue) &&
          statsAfterWrite.exists(s =>
            s.originalSize == newValue.getBytes(StandardCharsets.UTF_8).length
          )
      )
    },
    test("compression ratio should be better with compressible data") {
      for
        // Create a compressible string (repeated characters)
        compressibleValue <- ZIO.succeed(generateCompressibleString(20))

        // Create a less compressible string (random characters)
        randomValue <- ZIO.succeed(generateRandomString(20))

        // Create cells for both
        compressibleCell <- CompressedMemoryCell.make(
          compressibleValue,
          stringSerializer,
          stringDeserializer
        )

        randomCell <- CompressedMemoryCell.make(
          randomValue,
          stringSerializer,
          stringDeserializer
        )

        // Get compression stats for both
        compressibleStats <- compressibleCell.getCompressionStats
        randomStats       <- randomCell.getCompressionStats
      yield assertTrue(
        compressibleStats.exists(s => s.compressionRatio > 1.0) &&
          randomStats.exists(s => s.compressionRatio > 1.0) &&
          compressibleStats.exists(cs =>
            randomStats.exists(rs => cs.compressionRatio > rs.compressionRatio)
          )
      )
    },
    test("should allow forcing compression") {
      for
        // Create a small string (would normally not compress)
        smallValue <- ZIO.succeed("Small string")

        // Create the compressed cell with a high threshold
        cell <- CompressedMemoryCell.makeWithStrategy(
          smallValue,
          GzipCompressionStrategy,
          stringSerializer,
          stringDeserializer,
          10 * 1024 // 10KB threshold
        )

        // Initial stats should show no compression
        initialStats <- cell.getCompressionStats

        // Force compression despite being under threshold
        _ <- cell.forceCompress

        // Get new stats
        forcedStats <- cell.getCompressionStats
      yield assertTrue(
        initialStats.exists(s => s.strategy == "None") &&
          forcedStats.exists(s => s.strategy == "GZIP")
      )
    },
    test("should correctly update values with a function") {
      for
        // Create initial value
        initialValue <- ZIO.succeed("Initial")

        // Create the compressed cell
        cell <- CompressedMemoryCell.make(
          initialValue,
          stringSerializer,
          stringDeserializer
        )

        // Update using a function
        _ <- cell.update(opt => opt.map(_ + " + Updated").getOrElse("Updated"))

        // Read updated value
        readUpdated <- cell.read

        // Stats should reflect new size
        statsAfterUpdate <- cell.getCompressionStats
      yield assertTrue(
        readUpdated.contains("Initial + Updated") &&
          statsAfterUpdate.exists(s =>
            s.originalSize == "Initial + Updated".getBytes(StandardCharsets.UTF_8).length
          )
      )
    },
    test("should clear data correctly") {
      for
        // Create initial value
        initialValue <- ZIO.succeed(generateCompressibleString(5))

        // Create the compressed cell
        cell <- CompressedMemoryCell.make(
          initialValue,
          stringSerializer,
          stringDeserializer
        )

        // Clear the cell
        _ <- cell.clear

        // Read the value after clearing
        readAfterClear <- cell.read

        // Stats should reflect original value size
        statsAfterClear <- cell.getCompressionStats
      yield assertTrue(
        readAfterClear.contains(initialValue) &&
          statsAfterClear.exists(s =>
            s.originalSize == initialValue.getBytes(StandardCharsets.UTF_8).length
          )
      )
    },
    test("should empty the cell correctly") {
      for
        // Create initial value
        initialValue <- ZIO.succeed(generateCompressibleString(5))

        // Create the compressed cell
        cell <- CompressedMemoryCell.make(
          initialValue,
          stringSerializer,
          stringDeserializer
        )

        // Empty the cell
        _ <- cell.empty

        // Read the value after emptying
        readAfterEmpty <- cell.read

        // Stats should show zero size
        statsAfterEmpty <- cell.getCompressionStats
      yield
        assertTrue(
          readAfterEmpty.isEmpty
        )
        // When the cell is emptied, compression stats should also be None
        assertTrue(statsAfterEmpty.isEmpty)
    }
  ) @@ timeout(60.seconds)
