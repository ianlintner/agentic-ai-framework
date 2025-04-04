package com.agenticai.core.memory

import zio._
import zio.stream._
import java.time.{Duration => JavaDuration, Instant}
import scala.collection.concurrent.TrieMap

/**
 * A system for managing multiple memory cells
 */
trait MemorySystem {
  /**
   * Create a new memory cell with the given initial value
   */
  def createCell[A](initialValue: A): ZIO[Any, MemoryError, MemoryCell[A]]

  /**
   * Create a new memory cell with tags
   */
  def createCellWithTags[A](initialValue: A, tags: Set[String]): ZIO[Any, MemoryError, MemoryCell[A]]

  /**
   * Get all memory cells with the given tag
   */
  def getCellsByTag(tag: String): ZIO[Any, MemoryError, Set[MemoryCell[_]]]

  /**
   * Get all memory cells
   */
  def getAllCells: ZIO[Any, MemoryError, Set[MemoryCell[_]]]

  /**
   * Clear all memory cells
   */
  def clearAll: ZIO[Any, MemoryError, Unit]
  
  /**
   * Register a cleanup strategy to be applied automatically
   */
  def registerCleanupStrategy(strategy: CleanupStrategy): ZIO[Any, MemoryError, Unit]
  
  /**
   * Unregister a cleanup strategy
   */
  def unregisterCleanupStrategy(strategyName: String): ZIO[Any, MemoryError, Unit]
  
  /**
   * Get all registered cleanup strategies
   */
  def getCleanupStrategies: ZIO[Any, MemoryError, List[CleanupStrategy]]
  
  /**
   * Run cleanup manually using all registered strategies
   */
  def runCleanup: ZIO[Any, MemoryError, Int]
  
  /**
   * Run cleanup manually using a specific strategy
   */
  def runCleanup(strategy: CleanupStrategy): ZIO[Any, MemoryError, Int]
  
  /**
   * Enable automatic cleanup with the given interval
   */
  def enableAutomaticCleanup(interval: JavaDuration): ZIO[Any, MemoryError, Unit]
  
  /**
   * Disable automatic cleanup
   */
  def disableAutomaticCleanup: ZIO[Any, MemoryError, Unit]
}

/**
 * Implementation of a memory system that stores cells in memory
 */
class InMemorySystem extends MemorySystem {
  private val cells = new TrieMap[String, MemoryCell[_]]()
  private val tagIndex = new TrieMap[String, Set[String]]()
  private val cleanupStrategies = new TrieMap[String, CleanupStrategy]()
  private var cleanupFiber: Option[Fiber.Runtime[Throwable, Unit]] = None
  private var automaticCleanupInterval: Option[JavaDuration] = None

  override def createCell[A](initialValue: A): ZIO[Any, MemoryError, MemoryCell[A]] = {
    for {
      cell <- MemoryCell.make(initialValue)
      _ <- ZIO.succeed {
        val id = java.util.UUID.randomUUID().toString
        cells.put(id, cell)
      }
    } yield cell
  }

  override def createCellWithTags[A](initialValue: A, tags: Set[String]): ZIO[Any, MemoryError, MemoryCell[A]] = {
    for {
      cell <- MemoryCell.makeWithTags(initialValue, tags)
      _ <- ZIO.succeed {
        val id = java.util.UUID.randomUUID().toString
        cells.put(id, cell)
        tags.foreach { tag =>
          tagIndex.update(tag, tagIndex.getOrElse(tag, Set.empty) + id)
        }
      }
    } yield cell
  }

  override def getCellsByTag(tag: String): ZIO[Any, MemoryError, Set[MemoryCell[_]]] = {
    ZIO.succeed {
      tagIndex.getOrElse(tag, Set.empty)
        .flatMap(id => cells.get(id))
        .toSet
    }
  }

  override def getAllCells: ZIO[Any, MemoryError, Set[MemoryCell[_]]] = {
    ZIO.succeed(cells.values.toSet)
  }

  override def clearAll: ZIO[Any, MemoryError, Unit] = {
    for {
      _ <- ZIO.foreach(cells.values)(_.clear)
      _ <- ZIO.succeed {
        cells.clear()
        tagIndex.clear()
      }
    } yield ()
  }
  
  override def registerCleanupStrategy(strategy: CleanupStrategy): ZIO[Any, MemoryError, Unit] = {
    ZIO.succeed {
      cleanupStrategies.put(strategy.name, strategy)
    }
  }
  
  override def unregisterCleanupStrategy(strategyName: String): ZIO[Any, MemoryError, Unit] = {
    ZIO.succeed {
      cleanupStrategies.remove(strategyName)
    }
  }
  
  override def getCleanupStrategies: ZIO[Any, MemoryError, List[CleanupStrategy]] = {
    ZIO.succeed {
      cleanupStrategies.values.toList
    }
  }
  
  override def runCleanup: ZIO[Any, MemoryError, Int] = {
    for {
      strategies <- getCleanupStrategies
      results <- ZIO.foreach(strategies)(runCleanup)
    } yield results.sum
  }
  
  override def runCleanup(strategy: CleanupStrategy): ZIO[Any, MemoryError, Int] = {
    for {
      allCells <- getAllCells
      cellsToCleanup <- ZIO.filter(allCells.toList)(strategy.shouldCleanup)
      _ <- ZIO.foreach(cellsToCleanup)(_.empty)
    } yield cellsToCleanup.size
  }
  
  override def enableAutomaticCleanup(interval: JavaDuration): ZIO[Any, MemoryError, Unit] = {
    for {
      _ <- disableAutomaticCleanup
      fiber <- scheduleCleanup(interval).fork
      _ <- ZIO.succeed {
        cleanupFiber = Some(fiber)
        automaticCleanupInterval = Some(interval)
      }
    } yield ()
  }
  
  override def disableAutomaticCleanup: ZIO[Any, MemoryError, Unit] = {
    for {
      // Actually interrupt the fiber if it exists
      _ <- ZIO.whenCase(cleanupFiber) {
        case Some(fiber) => fiber.interrupt
      }
      _ <- ZIO.succeed {
        cleanupFiber = None
        automaticCleanupInterval = None
      }
    } yield ()
  }
  
  private def scheduleCleanup(interval: JavaDuration): ZIO[Any, MemoryError, Unit] = {
    val intervalDuration = zio.Duration.fromMillis(interval.toMillis)

    // A simpler approach that avoids type inference issues
    def cleanupAction = for {
      _ <- ZIO.logInfo("Running scheduled cleanup")
      count <- runCleanup
      _ <- ZIO.logInfo(s"Automatic cleanup removed $count cells")
    } yield ()

    def loop: ZIO[Any, Nothing, Unit] = {
      // Make sure the loop never fails by converting all errors to logged messages
      (for {
        _ <- cleanupAction.catchAll(e => ZIO.logError(s"Error during cleanup: $e"))
        _ <- ZIO.sleep(intervalDuration)
        // Only continue if cleanupFiber is still active
        _ <- ZIO.whenCase(cleanupFiber) {
          case Some(_) => loop
        }
      } yield ()).catchAllDefect { e =>
        ZIO.logError(s"Fatal error in cleanup loop: ${e.getMessage}") *> loop
      }
    }

    ZIO.logInfo("Starting automatic cleanup schedule") *> loop.ensuring(ZIO.logInfo("Cleanup loop terminated"))
  }
}

/**
 * Companion object for creating memory systems
 */
object MemorySystem {
  /**
   * Create a new in-memory system
   */
  def make: ZIO[Any, MemoryError, MemorySystem] = {
    ZIO.succeed(new InMemorySystem())
  }
  
  /**
   * Create a new in-memory system with automatic cleanup
   */
  def makeWithAutomaticCleanup(
    interval: JavaDuration,
    strategies: CleanupStrategy*
  ): ZIO[Any, MemoryError, MemorySystem] = {
    for {
      system <- make
      _ <- ZIO.foreach(strategies)(system.registerCleanupStrategy)
      _ <- system.enableAutomaticCleanup(interval)
    } yield system
  }
  
  /**
   * Create a time-based cleanup memory system
   *
   * Uses time-based access for the default strategy, which cleans up cells
   * that haven't been accessed for a specified time period.
   */
  def makeWithTimeBasedCleanup(
    maxAge: JavaDuration,
    interval: JavaDuration = java.time.Duration.ofMinutes(5)
  ): ZIO[Any, MemoryError, MemorySystem] = {
    for {
      system <- make
      // The exact timeBasedAccess instance that will be used in the test
      cleanupStrategy = CleanupStrategy.timeBasedAccess(maxAge)
      // Name this strategy so it can be found later
      _ <- system.registerCleanupStrategy(cleanupStrategy)
      // Enable automatic cleanup
      _ <- system.enableAutomaticCleanup(interval)
    } yield system
  }
  
  /**
   * Create a size-based cleanup memory system
   */
  def makeWithSizeBasedCleanup(
    maxSize: Long,
    interval: JavaDuration = java.time.Duration.ofMinutes(5)
  ): ZIO[Any, MemoryError, MemorySystem] = {
    makeWithAutomaticCleanup(
      interval,
      CleanupStrategy.sizeBasedCleanup(maxSize)
    )
  }
}