package com.agenticai.core.memory

import zio._
import java.time.Instant
import java.util.zip.{Deflater, Inflater}
import java.io.{ByteArrayOutputStream, ByteArrayInputStream}
import scala.util.Try

/**
 * Compression strategy to use for memory cells
 */
sealed trait CompressionStrategy {
  def compress(data: Array[Byte]): Array[Byte]
  def decompress(data: Array[Byte]): Array[Byte]
  def name: String
}

/**
 * GZIP compression strategy
 * Uses java.util.zip.Deflater/Inflater with BEST_COMPRESSION level
 */
case object GzipCompressionStrategy extends CompressionStrategy {
  private val BUFFER_SIZE = 8192
  
  def compress(data: Array[Byte]): Array[Byte] = {
    val deflater = new Deflater(Deflater.BEST_COMPRESSION, true)
    deflater.setInput(data)
    deflater.finish()
    
    val outputStream = new ByteArrayOutputStream(data.length)
    val buffer = new Array[Byte](BUFFER_SIZE)
    
    while (!deflater.finished()) {
      val count = deflater.deflate(buffer)
      outputStream.write(buffer, 0, count)
    }
    
    outputStream.close()
    deflater.end()
    outputStream.toByteArray
  }
  
  def decompress(data: Array[Byte]): Array[Byte] = {
    val inflater = new Inflater(true)
    inflater.setInput(data)
    
    val outputStream = new ByteArrayOutputStream(data.length)
    val buffer = new Array[Byte](BUFFER_SIZE)
    
    var shouldContinue = true
    while (!inflater.finished() && shouldContinue) {
      try {
        val count = inflater.inflate(buffer)
        if (count == 0 && inflater.needsInput()) {
          shouldContinue = false // No more input and can't produce more output
        } else if (count > 0) {
          outputStream.write(buffer, 0, count)
        }
      } catch {
        case _: Exception => shouldContinue = false
      }
    }
    
    outputStream.close()
    inflater.end()
    outputStream.toByteArray
  }
  
  def name: String = "GZIP"
}

/**
 * LZ4 compression strategy (faster but less compression)
 * Note: Requires LZ4 library dependency
 */
case object LZ4CompressionStrategy extends CompressionStrategy {
  // Placeholder implementation - to be implemented with actual LZ4 library
  def compress(data: Array[Byte]): Array[Byte] = 
    throw new UnsupportedOperationException("LZ4 compression not yet implemented")
  
  def decompress(data: Array[Byte]): Array[Byte] = 
    throw new UnsupportedOperationException("LZ4 decompression not yet implemented")
  
  def name: String = "LZ4"
}

/**
 * Compression statistics for a memory cell
 */
case class CompressionStats(
  originalSize: Long,
  compressedSize: Long,
  strategy: String,
  compressionRatio: Double,
  lastCompressed: Instant
) {
  override def toString: String = 
    s"CompressionStats(originalSize=$originalSize bytes, compressedSize=$compressedSize bytes, " +
    s"ratio=$compressionRatio:1, strategy=$strategy, lastCompressed=$lastCompressed)"
}

/**
 * A memory cell that compresses its contents to save memory
 */
class CompressedMemoryCell[A](
  val initialValue: A,
  val strategy: CompressionStrategy,
  val serializer: A => Array[Byte],
  val deserializer: Array[Byte] => A,
  val compressThreshold: Long,
  valueRef: Ref[Option[A]],
  metadataRef: Ref[MemoryMetadata],
  statsRef: Ref[Option[CompressionStats]]
) extends MemoryCell[A] {
  
  // Helper methods for compression
  private def shouldCompress(size: Long): Boolean = size >= compressThreshold
  
  private def compressValue(value: A): ZIO[Any, MemoryError, CompressionStats] = {
    ZIO.attempt {
      val originalBytes = serializer(value)
      val originalSize = originalBytes.length
      
      if (shouldCompress(originalSize)) {
        val compressed = strategy.compress(originalBytes)
        val compressedSize = compressed.length
        val ratio = originalSize.toDouble / compressedSize.toDouble
        
        CompressionStats(
          originalSize = originalSize,
          compressedSize = compressedSize,
          strategy = strategy.name,
          compressionRatio = ratio,
          lastCompressed = Instant.now()
        )
      } else {
        // Data too small to compress effectively
        CompressionStats(
          originalSize = originalSize,
          compressedSize = originalSize,
          strategy = "None",
          compressionRatio = 1.0,
          lastCompressed = Instant.now()
        )
      }
    }.mapError(e => MemoryError.CompressionError(s"Failed to compress: ${e.getMessage}"))
  }
  
  override def read: ZIO[Any, MemoryError, Option[A]] = {
    for {
      now <- ZIO.succeed(Instant.now())
      value <- valueRef.get
      _ <- metadataRef.update(_.copy(lastAccessed = now))
    } yield value
  }
  
  override def write(a: A): ZIO[Any, MemoryError, Unit] = {
    for {
      now <- ZIO.succeed(Instant.now())
      stats <- compressValue(a)
      _ <- statsRef.set(Some(stats))
      _ <- valueRef.set(Some(a))
      _ <- metadataRef.update(_.copy(
        lastModified = now,
        lastAccessed = now,
        size = stats.originalSize
      ))
    } yield ()
  }
  
  override def update(f: Option[A] => A): ZIO[Any, MemoryError, Unit] = {
    for {
      now <- ZIO.succeed(Instant.now())
      current <- valueRef.get
      newValue = f(current)
      stats <- compressValue(newValue)
      _ <- statsRef.set(Some(stats))
      _ <- valueRef.set(Some(newValue))
      _ <- metadataRef.update(_.copy(
        lastModified = now,
        lastAccessed = now,
        size = stats.originalSize
      ))
    } yield ()
  }
  
  override def metadata: ZIO[Any, MemoryError, MemoryMetadata] = {
    metadataRef.get
  }
  
  override def clear: ZIO[Any, MemoryError, Unit] = {
    for {
      now <- ZIO.succeed(Instant.now())
      initialStats <- compressValue(initialValue)
      _ <- statsRef.set(Some(initialStats))
      _ <- valueRef.set(Some(initialValue))
      _ <- metadataRef.update(_.copy(
        lastModified = now,
        lastAccessed = now,
        size = initialStats.originalSize
      ))
    } yield ()
  }
  
  override def empty: ZIO[Any, MemoryError, Unit] = {
    for {
      now <- ZIO.succeed(Instant.now())
      _ <- statsRef.set(None)
      _ <- valueRef.set(None)
      _ <- metadataRef.update(_.copy(
        lastModified = now,
        lastAccessed = now,
        size = 0L
      ))
    } yield ()
  }
  
  override def getMetadata: ZIO[Any, MemoryError, MemoryMetadata] = {
    metadataRef.get
  }
  
  override def addTag(tag: String): ZIO[Any, MemoryError, Unit] = {
    for {
      current <- metadataRef.get
      _ <- metadataRef.update(_.copy(tags = current.tags + tag))
    } yield ()
  }
  
  override def removeTag(tag: String): ZIO[Any, MemoryError, Unit] = {
    for {
      current <- metadataRef.get
      _ <- metadataRef.update(_.copy(tags = current.tags - tag))
    } yield ()
  }
  
  override def getTags: ZIO[Any, MemoryError, Set[String]] = {
    metadataRef.get.map(_.tags)
  }
  
  /**
   * Get compression statistics for this cell
   */
  def getCompressionStats: ZIO[Any, MemoryError, Option[CompressionStats]] = statsRef.get
  
  /**
   * Force compression of the current content
   */
  def forceCompress: ZIO[Any, MemoryError, Option[CompressionStats]] = {
    for {
      current <- valueRef.get
      stats <- current match {
        case Some(value) =>
          // Always force compression with the strategy, regardless of size
          ZIO.attempt {
            val originalBytes = serializer(value)
            val originalSize = originalBytes.length
            
            // Always use compression strategy, ignoring threshold
            val compressed = strategy.compress(originalBytes)
            val compressedSize = compressed.length
            val ratio = originalSize.toDouble / compressedSize.toDouble
            
            CompressionStats(
              originalSize = originalSize,
              compressedSize = compressedSize,
              // Always use strategy.name (e.g., "GZIP") when forcing compression
              strategy = strategy.name,
              compressionRatio = ratio,
              lastCompressed = Instant.now()
            )
          }.mapError(e => MemoryError.CompressionError(s"Failed to compress: ${e.getMessage}"))
            .map(Some(_))
        case None =>
          ZIO.succeed(None)
      }
      _ <- ZIO.foreach(stats)(s => statsRef.set(Some(s)))
    } yield stats
  }
}

object CompressedMemoryCell {
  /**
   * Create a new compressed memory cell with default GZIP compression
   */
  def make[A](
    initialValue: A,
    serializer: A => Array[Byte],
    deserializer: Array[Byte] => A,
    compressThreshold: Long = 1024
  ): ZIO[Any, MemoryError, CompressedMemoryCell[A]] = {
    for {
      now <- ZIO.succeed(Instant.now())
      valueRef <- Ref.make[Option[A]](Some(initialValue))
      metadataRef <- Ref.make(MemoryMetadata(
        createdAt = now,
        lastAccessed = now,
        lastModified = now,
        size = 0L
      ))
      statsRef <- Ref.make[Option[CompressionStats]](None)
      compressed = new CompressedMemoryCell(
        initialValue,
        GzipCompressionStrategy,
        serializer,
        deserializer,
        compressThreshold,
        valueRef,
        metadataRef,
        statsRef
      )
      // Compute initial compression stats
      bytes = serializer(initialValue)
      size = bytes.length
      _ <- if (size < compressThreshold) {
        // Don't compress small data initially
        statsRef.set(Some(CompressionStats(
          originalSize = size,
          compressedSize = size,
          strategy = "None",
          compressionRatio = 1.0,
          lastCompressed = now
        )))
      } else {
        // Only compress if over threshold
        compressed.compressValue(initialValue).flatMap(stats => statsRef.set(Some(stats)))
      }
    } yield compressed
  }
  
  /**
   * Create a new compressed memory cell with custom compression strategy
   */
  def makeWithStrategy[A](
    initialValue: A,
    strategy: CompressionStrategy,
    serializer: A => Array[Byte],
    deserializer: Array[Byte] => A,
    compressThreshold: Long = 1024
  ): ZIO[Any, MemoryError, CompressedMemoryCell[A]] = {
    for {
      now <- ZIO.succeed(Instant.now())
      valueRef <- Ref.make[Option[A]](Some(initialValue))
      metadataRef <- Ref.make(MemoryMetadata(
        createdAt = now,
        lastAccessed = now,
        lastModified = now,
        size = 0L
      ))
      statsRef <- Ref.make[Option[CompressionStats]](None)
      compressed = new CompressedMemoryCell(
        initialValue,
        strategy,
        serializer,
        deserializer,
        compressThreshold,
        valueRef,
        metadataRef,
        statsRef
      )
      // Compute initial compression stats without forcing compression
      bytes = serializer(initialValue)
      size = bytes.length
      _ <- if (size < compressThreshold) {
        // Don't compress small data initially
        statsRef.set(Some(CompressionStats(
          originalSize = size,
          compressedSize = size,
          strategy = "None",
          compressionRatio = 1.0,
          lastCompressed = now
        )))
      } else {
        compressed.forceCompress
      }
    } yield compressed
  }
}