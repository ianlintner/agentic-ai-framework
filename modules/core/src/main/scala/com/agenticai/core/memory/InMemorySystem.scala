package com.agenticai.core.memory

import zio.*
import java.time.Duration as JavaDuration

/** Simple in-memory implementation of MemorySystem
  */
class InMemorySystem extends MemorySystem:
  // Maps to store cells by name
  private val cells = scala.collection.mutable.Map[String, MemoryCell[_]]()
  // Map to store cleanup strategies
  private val strategies = scala.collection.mutable.Map[String, CleanupStrategy]()
  // Fiber for automatic cleanup
  private var cleanupFiber: Option[Fiber.Runtime[Throwable, Unit]] = None

  override def createCell[T](name: String): ZIO[Any, Throwable, MemoryCell[T]] =
    for
      cell <- MemoryCell.make[T](name.asInstanceOf[T])
      _    <- ZIO.succeed(cells(name) = cell)
    yield cell

  override def getCell[T](name: String): ZIO[Any, Throwable, Option[MemoryCell[T]]] =
    ZIO.attempt(cells.get(name).map(_.asInstanceOf[MemoryCell[T]]))

  override def deleteCell(name: String): ZIO[Any, Throwable, Unit] =
    ZIO.succeed(cells.remove(name)).unit

  override def getAllCells: ZIO[Any, Throwable, List[MemoryCell[_]]] =
    ZIO.succeed(cells.values.toList)

  override def getCellsByTag(tag: String): ZIO[Any, Throwable, List[MemoryCell[_]]] =
    for
      allCells <- getAllCells
      // Filter cells that have the given tag
      cellsWithTags <- ZIO
        .foreach(allCells) { cell =>
          cell.getTags
            .map(tags => (cell, tags))
            .catchAll(_ => ZIO.succeed((cell, Set.empty[String])))
        }
        .map(cellsWithTags => cellsWithTags.filter(_._2.contains(tag)).map(_._1).toList)
    yield cellsWithTags

  override def createCellWithTags[A](
      initialValue: A,
      tags: Set[String]
  ): ZIO[Any, MemoryError, MemoryCell[A]] =
    for
      cell <- MemoryCell.makeWithTags(initialValue, tags)
      _    <- ZIO.succeed(cells(initialValue.toString) = cell)
    yield cell

  override def enableAutomaticCleanup(interval: JavaDuration): ZIO[Any, MemoryError, Unit] =
    for
      _     <- disableAutomaticCleanup
      fiber <- scheduleCleanup(interval).fork
      _ <- ZIO.succeed {
        cleanupFiber = Some(fiber)
      }
    yield ()

  override def disableAutomaticCleanup: ZIO[Any, MemoryError, Unit] =
    for
      _ <- ZIO.foreachDiscard(cleanupFiber)(_.interrupt)
      _ <- ZIO.succeed {
        cleanupFiber = None
      }
    yield ()

  private def scheduleCleanup(interval: JavaDuration): ZIO[Any, MemoryError, Unit] =
    val intervalDuration = zio.Duration.fromMillis(interval.toMillis)
    val schedule         = Schedule.fixed(intervalDuration)

    runCleanup
      .tap(count => ZIO.logInfo(s"Automatic cleanup removed $count cells"))
      .repeat(schedule)
      .unit
      .catchAll(e => ZIO.logError(s"Error during automatic cleanup: ${e.getMessage}"))

  override def runCleanup: ZIO[Any, MemoryError, Int] =
    for
      strategies <- getCleanupStrategies
      counts     <- ZIO.foreach(strategies)(runCleanup)
      totalCount = counts.sum
    yield totalCount

  override def runCleanup(strategy: CleanupStrategy): ZIO[Any, MemoryError, Int] =
    for
      allCells <- getAllCells.mapError(e => MemoryError.ReadError(e.getMessage))
      count <- ZIO.foldLeft(allCells)(0) { (count, cell) =>
        for
          shouldClean <- strategy.shouldCleanup(cell)
          newCount <-
            if shouldClean then cell.empty.as(count + 1)
            else ZIO.succeed(count)
        yield newCount
      }
    yield count

  override def registerCleanupStrategy(strategy: CleanupStrategy): ZIO[Any, MemoryError, Unit] =
    ZIO.succeed(strategies(strategy.name) = strategy)

  override def unregisterCleanupStrategy(strategyName: String): ZIO[Any, MemoryError, Unit] =
    ZIO.succeed(strategies.remove(strategyName))

  override def getCleanupStrategies: ZIO[Any, MemoryError, List[CleanupStrategy]] =
    ZIO.succeed(strategies.values.toList)

  override def clearAll: ZIO[Any, MemoryError, Unit] =
    ZIO.succeed {
      cells.clear()
    }
