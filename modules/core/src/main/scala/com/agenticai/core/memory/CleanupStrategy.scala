package com.agenticai.core.memory

import zio._
import java.time.Instant
import java.time.{Duration => JavaDuration}

/**
 * Represents a strategy for cleaning up memory cells
 */
trait CleanupStrategy {
  /**
   * Evaluates whether a memory cell should be cleaned up
   */
  def shouldCleanup(cell: MemoryCell[_]): ZIO[Any, MemoryError, Boolean]
  
  /**
   * Gets the name of the strategy
   */
  def name: String
}

/**
 * Cleanup strategies for memory cells
 */
object CleanupStrategy {
  /**
   * Creates a time-based cleanup strategy that removes cells that haven't been 
   * accessed for a certain period of time
   */
  def timeBasedAccess(maxAge: JavaDuration): CleanupStrategy = new CleanupStrategy {
    override def shouldCleanup(cell: MemoryCell[_]): ZIO[Any, MemoryError, Boolean] = {
      for {
        // Use ZIO.clockWith(_.instant) instead of Instant.now() to work with TestClock
        now <- ZIO.clockWith(_.instant)
        metadata <- cell.metadata
        age = JavaDuration.between(metadata.lastAccessed, now)
        shouldClean = age.compareTo(maxAge) > 0
        // Add debugging when used in tests
        _ <- ZIO.when(shouldClean)(ZIO.logDebug(
          s"Cell should be cleaned: age=${age.toMillis}ms > maxAge=${maxAge.toMillis}ms, " +
          s"lastAccessed=${metadata.lastAccessed}, now=$now"
        ))
      } yield shouldClean
    }
    
    override def name: String = s"TimeBasedAccess(${maxAge.toSeconds}s)"
  }
  
  /**
   * Creates a time-based cleanup strategy that removes cells that haven't been 
   * modified for a certain period of time
   */
  def timeBasedModification(maxAge: JavaDuration): CleanupStrategy = new CleanupStrategy {
    override def shouldCleanup(cell: MemoryCell[_]): ZIO[Any, MemoryError, Boolean] = {
      for {
        // Use ZIO.clockWith(_.instant) instead of Instant.now() to work with TestClock
        now <- ZIO.clockWith(_.instant)
        metadata <- cell.metadata
        age = JavaDuration.between(metadata.lastModified, now)
        shouldClean = age.compareTo(maxAge) > 0
        // Add debugging when used in tests
        _ <- ZIO.when(shouldClean)(ZIO.logDebug(
          s"Cell should be cleaned: age=${age.toMillis}ms > maxAge=${maxAge.toMillis}ms, " +
          s"lastModified=${metadata.lastModified}, now=$now"
        ))
      } yield shouldClean
    }
    
    override def name: String = s"TimeBasedModification(${maxAge.toSeconds}s)"
  }
  
  /**
   * Creates a size-based cleanup strategy that removes cells that exceed a certain size
   */
  def sizeBasedCleanup(maxSize: Long): CleanupStrategy = new CleanupStrategy {
    override def shouldCleanup(cell: MemoryCell[_]): ZIO[Any, MemoryError, Boolean] = {
      for {
        metadata <- cell.metadata
      } yield metadata.size > maxSize
    }
    
    override def name: String = s"SizeBasedCleanup($maxSize bytes)"
  }
  
  /**
   * Creates a tag-based cleanup strategy that removes cells with specific tags
   */
  def tagBasedCleanup(tags: Set[String]): CleanupStrategy = new CleanupStrategy {
    override def shouldCleanup(cell: MemoryCell[_]): ZIO[Any, MemoryError, Boolean] = {
      for {
        cellTags <- cell.getTags
      } yield tags.exists(cellTags.contains)
    }
    
    override def name: String = s"TagBasedCleanup(${tags.mkString(", ")})"
  }
  
  /**
   * Combines multiple cleanup strategies with OR logic
   */
  def any(strategies: CleanupStrategy*): CleanupStrategy = new CleanupStrategy {
    override def shouldCleanup(cell: MemoryCell[_]): ZIO[Any, MemoryError, Boolean] = {
      ZIO.foldLeft(strategies)(false) { (result, strategy) =>
        if (result) ZIO.succeed(true)
        else strategy.shouldCleanup(cell)
      }
    }
    
    override def name: String = s"Any(${strategies.map(_.name).mkString(", ")})"
  }
  
  /**
   * Combines multiple cleanup strategies with AND logic
   */
  def all(strategies: CleanupStrategy*): CleanupStrategy = new CleanupStrategy {
    override def shouldCleanup(cell: MemoryCell[_]): ZIO[Any, MemoryError, Boolean] = {
      ZIO.foldLeft(strategies)(true) { (result, strategy) =>
        if (!result) ZIO.succeed(false)
        else strategy.shouldCleanup(cell)
      }
    }
    
    override def name: String = s"All(${strategies.map(_.name).mkString(", ")})"
  }
}