package com.agenticai.core.memory

import zio._
import zio.stream._
import java.time.Instant
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
}

/**
 * Implementation of a memory system that stores cells in memory
 */
class InMemorySystem extends MemorySystem {
  private val cells = new TrieMap[String, MemoryCell[_]]()
  private val tagIndex = new TrieMap[String, Set[String]]()

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
} 