package com.agenticai.core.memory

import zio._
import java.io.File

/**
 * Interface for memory systems that manage memory cells
 */
trait MemorySystem {
  def createCell[T](name: String): ZIO[Any, Throwable, MemoryCell[T]]
  def getCell[T](name: String): ZIO[Any, Throwable, Option[MemoryCell[T]]]
  def deleteCell(name: String): ZIO[Any, Throwable, Unit]
  
  // Methods needed for memory monitoring
  def getAllCells: ZIO[Any, Throwable, List[MemoryCell[_]]]
  def getCellsByTag(tag: String): ZIO[Any, Throwable, List[MemoryCell[_]]]
  
  // Methods for creating cells with tags
  def createCellWithTags[A](initialValue: A, tags: Set[String]): ZIO[Any, MemoryError, MemoryCell[A]]
  
  // Methods for cleanup
  def enableAutomaticCleanup(interval: java.time.Duration): ZIO[Any, MemoryError, Unit]
  def disableAutomaticCleanup: ZIO[Any, MemoryError, Unit]
  
  // Methods for cleanup strategies
  def runCleanup: ZIO[Any, MemoryError, Int]
  def runCleanup(strategy: CleanupStrategy): ZIO[Any, MemoryError, Int]
  def registerCleanupStrategy(strategy: CleanupStrategy): ZIO[Any, MemoryError, Unit]
  def unregisterCleanupStrategy(strategyName: String): ZIO[Any, MemoryError, Unit]
  def getCleanupStrategies: ZIO[Any, MemoryError, List[CleanupStrategy]]
  
  // Method for clearing all cells
  def clearAll: ZIO[Any, MemoryError, Unit]
}

/**
 * Companion object for creating memory systems
 */
object MemorySystem {
  /**
   * Create a new in-memory system
   */
  def make: ZIO[Any, Nothing, MemorySystem] = {
    ZIO.succeed(new InMemorySystem())
  }
  
  /**
   * Create a new persistent memory system
   */
  def makePersistent(baseDir: File): ZIO[Any, MemoryError, PersistentMemorySystem] = {
    PersistentMemorySystem.make(baseDir)
  }
  
  /**
   * Create a new persistent memory system with automatic cleanup
   */
  def makeWithAutomaticCleanup(
    baseDir: File,
    interval: java.time.Duration,
    strategies: CleanupStrategy*
  ): ZIO[Any, MemoryError, PersistentMemorySystem] = {
    PersistentMemorySystem.makeWithAutomaticCleanup(baseDir, interval, strategies: _*)
  }
  
  /**
   * Create a time-based cleanup persistent memory system
   */
  def makeWithTimeBasedCleanup(
    baseDir: File,
    maxAge: java.time.Duration,
    interval: java.time.Duration = java.time.Duration.ofMinutes(5)
  ): ZIO[Any, MemoryError, PersistentMemorySystem] = {
    PersistentMemorySystem.makeWithTimeBasedCleanup(baseDir, maxAge, interval)
  }
  
  /**
   * Create a size-based cleanup persistent memory system
   */
  def makeWithSizeBasedCleanup(
    baseDir: File,
    maxSize: Long,
    interval: java.time.Duration = java.time.Duration.ofMinutes(5)
  ): ZIO[Any, MemoryError, PersistentMemorySystem] = {
    PersistentMemorySystem.makeWithSizeBasedCleanup(baseDir, maxSize, interval)
  }
}
