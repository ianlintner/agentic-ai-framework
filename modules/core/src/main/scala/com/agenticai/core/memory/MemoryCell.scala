package com.agenticai.core.memory

import zio._
import zio.stream._
import java.time.Instant

/**
 * Represents a cell in the agent's memory system
 */
trait MemoryCell[A] {
  /**
   * Read the current value of the memory cell
   */
  def read: ZIO[Any, MemoryError, Option[A]]

  /**
   * Write a new value to the memory cell
   */
  def write(a: A): ZIO[Any, MemoryError, Unit]

  /**
   * Update the memory cell using a function
   */
  def update(f: Option[A] => A): ZIO[Any, MemoryError, Unit]

  /**
   * Get the metadata associated with this memory cell
   */
  def metadata: ZIO[Any, MemoryError, MemoryMetadata]

  /**
   * Clear the memory cell
   */
  def clear: ZIO[Any, MemoryError, Unit]

  /**
   * Set the cell to empty (None) state
   */
  def empty: ZIO[Any, MemoryError, Unit]

  /**
   * Get the metadata associated with this memory cell
   */
  def getMetadata: ZIO[Any, MemoryError, MemoryMetadata]

  /**
   * Add a tag to the memory cell
   */
  def addTag(tag: String): ZIO[Any, MemoryError, Unit]

  /**
   * Remove a tag from the memory cell
   */
  def removeTag(tag: String): ZIO[Any, MemoryError, Unit]

  /**
   * Get the tags associated with this memory cell
   */
  def getTags: ZIO[Any, MemoryError, Set[String]]
}

/**
 * Metadata associated with a memory cell
 */
case class MemoryMetadata(
  createdAt: Instant,
  lastAccessed: Instant,
  lastModified: Instant,
  size: Long,
  tags: Set[String] = Set.empty
)

/**
 * Errors that can occur during memory operations
 */
sealed trait MemoryError extends Throwable
object MemoryError {
  case class ReadError(message: String) extends MemoryError {
    override def getMessage: String = message
  }
  case class WriteError(message: String) extends MemoryError {
    override def getMessage: String = message
  }
  case class UpdateError(message: String) extends MemoryError {
    override def getMessage: String = message
  }
  case class ClearError(message: String) extends MemoryError {
    override def getMessage: String = message
  }
  case class MetadataError(message: String) extends MemoryError {
    override def getMessage: String = message
  }
}

/**
 * Implementation of a memory cell that stores data in memory
 */
class InMemoryCell[A](val initialValue: A, ref: Ref[Option[A]], metadataRef: Ref[MemoryMetadata]) extends MemoryCell[A] {
  override def read: ZIO[Any, MemoryError, Option[A]] = {
    for {
      now <- ZIO.succeed(Instant.now())
      value <- ref.get
      _ <- metadataRef.update(_.copy(lastAccessed = now))
    } yield value
  }

  override def write(a: A): ZIO[Any, MemoryError, Unit] = {
    for {
      now <- ZIO.succeed(Instant.now())
      _ <- ref.set(Some(a))
      _ <- metadataRef.update(_.copy(
        lastModified = now,
        lastAccessed = now,
        size = estimateSize(a)
      ))
    } yield ()
  }

  override def update(f: Option[A] => A): ZIO[Any, MemoryError, Unit] = {
    for {
      now <- ZIO.succeed(Instant.now())
      current <- ref.get
      newValue = f(current)
      _ <- ref.set(Some(newValue))
      _ <- metadataRef.update(_.copy(
        lastModified = now,
        lastAccessed = now,
        size = estimateSize(newValue)
      ))
    } yield ()
  }

  override def metadata: ZIO[Any, MemoryError, MemoryMetadata] = {
    metadataRef.get
  }

  override def clear: ZIO[Any, MemoryError, Unit] = {
    for {
      now <- ZIO.succeed(Instant.now())
      _ <- ref.set(Some(initialValue))
      _ <- metadataRef.update(_.copy(
        lastModified = now,
        lastAccessed = now,
        size = estimateSize(initialValue)
      ))
    } yield ()
  }

  /**
   * Set the cell to empty (None) state
   */
  override def empty: ZIO[Any, MemoryError, Unit] = {
    for {
      now <- ZIO.succeed(Instant.now())
      _ <- ref.set(None)
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

  private def updateMetadata(f: MemoryMetadata => MemoryMetadata): ZIO[Any, MemoryError, Unit] = {
    metadataRef.update(f)
  }

  private def estimateSize(a: A): Long = {
    a match {
      case s: String => s.length.toLong
      case arr: Array[_] => arr.length.toLong
      case _ => 1L
    }
  }

  override def addTag(tag: String): ZIO[Any, MemoryError, Unit] = {
    for {
      current <- metadataRef.get
      _ <- updateMetadata(_.copy(tags = current.tags + tag))
    } yield ()
  }

  override def removeTag(tag: String): ZIO[Any, MemoryError, Unit] = {
    for {
      current <- metadataRef.get
      _ <- updateMetadata(_.copy(tags = current.tags - tag))
    } yield ()
  }

  override def getTags: ZIO[Any, MemoryError, Set[String]] = {
    metadataRef.get.map(_.tags)
  }
}

/**
 * Companion object for creating memory cells
 */
object MemoryCell {
  /**
   * Create a new in-memory cell with an initial value
   */
  def make[A](initialValue: A): ZIO[Any, MemoryError, MemoryCell[A]] = {
    for {
      now <- ZIO.succeed(Instant.now())
      ref <- Ref.make[Option[A]](Some(initialValue))
      metadataRef <- Ref.make(MemoryMetadata(
        createdAt = now,
        lastAccessed = now,
        lastModified = now,
        size = 0L
      ))
    } yield new InMemoryCell(initialValue, ref, metadataRef)
  }

  /**
   * Create a new in-memory cell with tags
   */
  def makeWithTags[A](initialValue: A, tags: Set[String]): ZIO[Any, MemoryError, MemoryCell[A]] = {
    for {
      now <- ZIO.succeed(Instant.now())
      ref <- Ref.make[Option[A]](Some(initialValue))
      metadataRef <- Ref.make(MemoryMetadata(
        createdAt = now,
        lastAccessed = now,
        lastModified = now,
        size = 0L,
        tags = tags
      ))
    } yield new InMemoryCell(initialValue, ref, metadataRef)
  }
} 