package com.agenticai.core.memory

import zio.*
import java.time.Duration as JavaDuration
import java.io.File

/** Trait defining the memory system interface.
  * Provides a unified interface for different memory system implementations.
  */
trait MemorySystem:
  /** Create a new memory cell with the given name
    */
  def createCell[T](name: String): ZIO[Any, Throwable, MemoryCell[T]]
  
  /** Get a memory cell by name
    */
  def getCell[T](name: String): ZIO[Any, Throwable, Option[MemoryCell[T]]]
  
  /** Delete a memory cell by name
    */
  def deleteCell(name: String): ZIO[Any, Throwable, Unit]
  
  /** Get all memory cells
    */
  def getAllCells: ZIO[Any, Throwable, List[MemoryCell[_]]]
  
  /** Get memory cells by tag
    */
  def getCellsByTag(tag: String): ZIO[Any, Throwable, List[MemoryCell[_]]]
  
  /** Create a memory cell with tags
    */
  def createCellWithTags[A](initialValue: A, tags: Set[String]): ZIO[Any, MemoryError, MemoryCell[A]]
  
  /** Clear all memory cells
    */
  def clearAll: ZIO[Any, MemoryError, Unit]
  
  /** Register a cleanup strategy
    */
  def registerCleanupStrategy(strategy: CleanupStrategy): ZIO[Any, MemoryError, Unit]
  
  /** Unregister a cleanup strategy
    */
  def unregisterCleanupStrategy(strategyName: String): ZIO[Any, MemoryError, Unit]
  
  /** Get all cleanup strategies
    */
  def getCleanupStrategies: ZIO[Any, MemoryError, List[CleanupStrategy]]
  
  /** Run cleanup using all registered strategies
    */
  def runCleanup: ZIO[Any, MemoryError, Int]
  
  /** Run cleanup using a specific strategy
    */
  def runCleanup(strategy: CleanupStrategy): ZIO[Any, MemoryError, Int]
  
  /** Enable automatic cleanup
    */
  def enableAutomaticCleanup(interval: JavaDuration): ZIO[Any, MemoryError, Unit]
  
  /** Disable automatic cleanup
    */
  def disableAutomaticCleanup: ZIO[Any, MemoryError, Unit]

/** Companion object for creating memory systems
  */
object MemorySystem:
  /** Create a new memory system
    */
  def make: ZIO[Any, Nothing, MemorySystem] =
    ZIO.succeed(new InMemorySystem())
    
  /** Create a new persistent memory system
    */
  def makePersistent(baseDir: File): ZIO[Any, MemoryError, MemorySystem] =
    PersistentMemorySystem.make(baseDir)
