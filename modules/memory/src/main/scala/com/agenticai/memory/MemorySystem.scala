package com.agenticai.memory

import zio.*
import scala.collection.mutable

/** Trait defining the memory system interface
  */
trait MemorySystem:
  def createCell[T](name: String): ZIO[Any, Throwable, MemoryCell[T]]
  def getCell[T](name: String): ZIO[Any, Throwable, Option[MemoryCell[T]]]
  def deleteCell(name: String): ZIO[Any, Throwable, Unit]

/** A cell in the memory system that can store and retrieve values
  */
trait MemoryCell[T]:
  def read: ZIO[Any, Throwable, Option[T]]
  def write(value: T): ZIO[Any, Throwable, Unit]
  def clear: ZIO[Any, Throwable, Unit]

/** In-memory implementation of the memory system
  */
class InMemorySystem extends MemorySystem:
  private val cells = mutable.Map[String, MemoryCell[?]]()

  def createCell[T](name: String): ZIO[Any, Throwable, MemoryCell[T]] =
    ZIO.succeed {
      val cell = new InMemoryCell[T]()
      cells(name) = cell
      cell
    }

  def getCell[T](name: String): ZIO[Any, Throwable, Option[MemoryCell[T]]] =
    ZIO.attempt(cells.get(name).map(_.asInstanceOf[MemoryCell[T]]))

  def deleteCell(name: String): ZIO[Any, Throwable, Unit] =
    ZIO.succeed(cells.remove(name)).unit

/** In-memory implementation of a memory cell
  */
class InMemoryCell[T] extends MemoryCell[T]:
  private var value: Option[T] = None

  def read: ZIO[Any, Throwable, Option[T]] =
    ZIO.succeed(value)

  def write(newValue: T): ZIO[Any, Throwable, Unit] =
    ZIO.succeed {
      value = Some(newValue)
    }

  def clear: ZIO[Any, Throwable, Unit] =
    ZIO.succeed {
      value = None
    }
