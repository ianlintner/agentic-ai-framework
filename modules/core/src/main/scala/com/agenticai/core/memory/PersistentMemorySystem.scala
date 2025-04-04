package com.agenticai.core.memory

import zio._
import zio.stream._
import java.time.{Duration => JavaDuration, Instant}
import java.io.{File, FileInputStream, FileOutputStream, ObjectInputStream, ObjectOutputStream, Serializable}
import scala.collection.concurrent.TrieMap
import scala.util.{Try, Success, Failure}

/**
 * A serializable wrapper for memory cells
 */
case class SerializableCell[A](
  value: Option[A],
  initialValue: A,
  metadata: MemoryMetadata
) extends Serializable

object SerializableCell {
  def fromMemoryCell[A](cell: MemoryCell[A]): ZIO[Any, MemoryError, SerializableCell[A]] = {
    for {
      value <- cell.read
      meta <- cell.getMetadata
      // Get the initial value from the cell
      initialValue <- ZIO.succeed {
        // If it's an InMemoryCell, we can get its initialValue directly
        cell match {
          case inMemory: InMemoryCell[A] => 
            inMemory.initialValue
          // For proxied cells or other types, we'll use the current value or a default
          case _ => 
            // Use the current value as the initial value, or fallback to a sensible default
            value.getOrElse(null.asInstanceOf[A])
        }
      }
    } yield new SerializableCell(value, initialValue, meta)
  }
}

/**
 * Implementation of a memory system that persists cells to disk
 */
class PersistentMemorySystem(baseDir: File, runtime: Runtime[Any]) extends MemorySystem {
  private val cells = new TrieMap[String, MemoryCell[_]]()
  private val tagIndex = new TrieMap[String, Set[String]]()
  private val cellFiles = new TrieMap[String, File]()
  private val cellIdMap = new TrieMap[MemoryCell[_], String]()
  private val cleanupStrategies = new TrieMap[String, CleanupStrategy]()
  private var cleanupFiber: Option[Fiber.Runtime[Throwable, Unit]] = None

  // Create base directory if it doesn't exist
  if (!baseDir.exists()) {
    baseDir.mkdirs()
  }

  // Load existing cells from disk
  Unsafe.unsafe { implicit unsafe =>
    runtime.unsafe.run(loadCells()).getOrThrow()
  }

  override def createCell[A](initialValue: A): ZIO[Any, MemoryError, MemoryCell[A]] = {
    for {
      cell <- MemoryCell.make(initialValue)
      id = java.util.UUID.randomUUID().toString
      file = new File(baseDir, s"cell_$id.ser")
      // First put entries in maps
      _ <- ZIO.succeed {
        cellFiles.put(id, file)
      }
      // Then save cell
      _ <- saveCell(id, cell)
      
      // Create a proxy cell that will automatically persist on write
      proxiedCell = new MemoryCell[A] {
        override def read = cell.read
        override def write(a: A) = for {
          _ <- cell.write(a)
          _ <- saveCell(id, cell)  // Save after write
        } yield ()
        override def update(f: Option[A] => A) = for {
          _ <- cell.update(f)
          _ <- saveCell(id, cell)  // Save after update
        } yield ()
        override def metadata = cell.metadata
        override def clear = for {
          _ <- cell.clear
          _ <- saveCell(id, cell)  // Save after clear
        } yield ()
        override def empty = for {
          _ <- cell.empty
          _ <- saveCell(id, cell)  // Save after empty
        } yield ()
        override def getMetadata = cell.getMetadata
        override def addTag(tag: String) = for {
          _ <- cell.addTag(tag)
          _ <- saveCell(id, cell)  // Save after adding tag
        } yield ()
        override def removeTag(tag: String) = for {
          _ <- cell.removeTag(tag)
          _ <- saveCell(id, cell)  // Save after removing tag
        } yield ()
        override def getTags = cell.getTags
      }
      
      _ <- ZIO.succeed {
        cells.put(id, proxiedCell)
        // cellFiles already contains the file
        cellIdMap.put(proxiedCell, id)  // Track proxiedCell → id mapping
      }
    } yield proxiedCell
  }

  override def createCellWithTags[A](initialValue: A, tags: Set[String]): ZIO[Any, MemoryError, MemoryCell[A]] = {
    for {
      cell <- MemoryCell.makeWithTags(initialValue, tags)
      id = java.util.UUID.randomUUID().toString
      file = new File(baseDir, s"cell_$id.ser")
      // First put entries in maps
      _ <- ZIO.succeed {
        cellFiles.put(id, file)
      }
      // Then save cell
      _ <- saveCell(id, cell)
      
      // Create a proxy cell that will automatically persist on write
      proxiedCell = new MemoryCell[A] {
        override def read = cell.read
        override def write(a: A) = for {
          _ <- cell.write(a)
          _ <- saveCell(id, cell)  // Save after write
        } yield ()
        override def update(f: Option[A] => A) = for {
          _ <- cell.update(f)
          _ <- saveCell(id, cell)  // Save after update
        } yield ()
        override def metadata = cell.metadata
        override def clear = for {
          _ <- cell.clear
          _ <- saveCell(id, cell)  // Save after clear
        } yield ()
        override def empty = for {
          _ <- cell.empty
          _ <- saveCell(id, cell)  // Save after empty
        } yield ()
        override def getMetadata = cell.getMetadata
        override def addTag(tag: String) = for {
          _ <- cell.addTag(tag)
          _ <- saveCell(id, cell)  // Save after adding tag
        } yield ()
        override def removeTag(tag: String) = for {
          _ <- cell.removeTag(tag)
          _ <- saveCell(id, cell)  // Save after removing tag
        } yield ()
        override def getTags = cell.getTags
      }
      
      _ <- ZIO.succeed {
        cells.put(id, proxiedCell)
        // cellFiles already contains the file
        cellIdMap.put(proxiedCell, id)  // Track proxiedCell → id mapping
        tags.foreach { tag =>
          tagIndex.update(tag, tagIndex.getOrElse(tag, Set.empty) + id)
        }
      }
    } yield proxiedCell
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
        cellFiles.values.foreach(_.delete())
        cellFiles.clear()
        cellIdMap.clear()
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
      // For persistent cells, we also need to save the changes to disk
      _ <- ZIO.foreach(cellsToCleanup) { cell =>
        ZIO.succeed {
          cellIdMap.get(cell).foreach { id =>
            Unsafe.unsafe { implicit unsafe =>
              runtime.unsafe.run(saveCell(id, cell)).getOrThrow()
            }
          }
        }
      }
    } yield cellsToCleanup.size
  }
  
  override def enableAutomaticCleanup(interval: JavaDuration): ZIO[Any, MemoryError, Unit] = {
    for {
      _ <- disableAutomaticCleanup
      fiber <- scheduleCleanup(interval).fork
      _ <- ZIO.succeed {
        cleanupFiber = Some(fiber)
      }
    } yield ()
  }
  
  override def disableAutomaticCleanup: ZIO[Any, MemoryError, Unit] = {
    for {
      _ <- ZIO.foreachDiscard(cleanupFiber)(_.interrupt)
      _ <- ZIO.succeed {
        cleanupFiber = None
      }
    } yield ()
  }
  
  private def scheduleCleanup(interval: JavaDuration): ZIO[Any, MemoryError, Unit] = {
    val intervalDuration = zio.Duration.fromMillis(interval.toMillis)
    val schedule = Schedule.fixed(intervalDuration)
    
    runCleanup
      .tap(count => ZIO.logInfo(s"Automatic cleanup removed $count cells"))
      .repeat(schedule)
      .unit
      .catchAll(e => ZIO.logError(s"Error during automatic cleanup: ${e.getMessage}"))
  }

  private def saveCell[A](id: String, cell: MemoryCell[A]): ZIO[Any, MemoryError, Unit] = {
    for {
      serializableCell <- SerializableCell.fromMemoryCell(cell)
      _ <- ZIO.attemptBlockingIO {
        val file = cellFiles(id)
        val oos = new ObjectOutputStream(new FileOutputStream(file))
        try {
          oos.writeObject(serializableCell)
        } finally {
          oos.close()
        }
      }
    } yield ()
  }.mapError(e => MemoryError.WriteError(e.getMessage))

  private def loadCells(): ZIO[Any, MemoryError, Unit] = {
    ZIO.attemptBlockingIO {
      if (baseDir.exists()) {
        val cellFiles = baseDir.listFiles().filter(_.getName.endsWith(".ser"))
        
        for (file <- cellFiles) {
          val id = file.getName.replace("cell_", "").replace(".ser", "")
          this.cellFiles.put(id, file)
          
          loadSingleCell(file, id)
        }
      }
    }.mapError(e => MemoryError.ReadError(e.getMessage))
  }
  
  // Helper method to load a single cell from file
  private def loadSingleCell(file: File, id: String): Unit = {
    var ois: ObjectInputStream = null
    try {
      ois = new ObjectInputStream(new FileInputStream(file))
      
      // Read serialized cell and cast using type erasure
      val serCell = ois.readObject().asInstanceOf[SerializableCell[_]]
      
      // Extract important values for reconstructing the cell
      val initialValue = serCell.initialValue
      val tags = serCell.metadata.tags
      val currentValue = serCell.value
      
      // Prepare to create new cell using unsafe
      Unsafe.unsafe { implicit unsafe =>
        // Create cell with initial value and tags
        val innerCell = runtime.unsafe.run(
          MemoryCell.makeWithTags(initialValue, tags)
        ).getOrThrow()
        
        // If there was a stored value, write it back
        val writeOpResult = currentValue match {
          case Some(value) => 
            // Special handling for strings
            val writeOp = value match {
              case s: String => innerCell.asInstanceOf[MemoryCell[String]].write(s)
              case other => 
                // For Any other type
                innerCell.asInstanceOf[MemoryCell[Any]].write(other)
            }
            // Execute the write operation
            try {
              runtime.unsafe.run(writeOp).getOrThrow()
            } catch {
              case e: Exception => println(s"Error writing value to cell: ${e.getMessage}")
            }
          case None => () // No stored value to write back
        }
        
        // Create proxy that defers to inner cell but handles persistence
        val proxy = new MemoryCell[Any] {
          override def read = innerCell.read.asInstanceOf[ZIO[Any, MemoryError, Option[Any]]]
          
          override def write(a: Any) = {
            // This cast is necessary to handle type erasure correctly
            val writeOp = a match {
              case s: String => innerCell.asInstanceOf[MemoryCell[String]].write(s)
              case other => innerCell.asInstanceOf[MemoryCell[Any]].write(other)
            }
            writeOp.flatMap(_ => saveCell(id, innerCell.asInstanceOf[MemoryCell[Any]]))
          }
          
          override def update(f: Option[Any] => Any) = {
            // Need to cast the function to match inner cell's type
            val typedF = f.asInstanceOf[Option[Any] => Any]
            innerCell.asInstanceOf[MemoryCell[Any]].update(typedF)
              .flatMap(_ => saveCell(id, innerCell.asInstanceOf[MemoryCell[Any]]))
          }
          
          override def metadata = innerCell.metadata
          override def clear = innerCell.clear.flatMap(_ => saveCell(id, innerCell.asInstanceOf[MemoryCell[Any]]))
          override def empty = innerCell.empty.flatMap(_ => saveCell(id, innerCell.asInstanceOf[MemoryCell[Any]]))
          override def getMetadata = innerCell.getMetadata
          override def addTag(tag: String) = innerCell.addTag(tag).flatMap(_ => saveCell(id, innerCell.asInstanceOf[MemoryCell[Any]]))
          override def removeTag(tag: String) = innerCell.removeTag(tag).flatMap(_ => saveCell(id, innerCell.asInstanceOf[MemoryCell[Any]]))
          override def getTags = innerCell.getTags
        }
        
        // Register in our collections
        cells.put(id, proxy)
        cellIdMap.put(proxy, id) 
        
        // Update tag index
        val loadedTags = runtime.unsafe.run(innerCell.getTags).getOrThrow()
        loadedTags.foreach { tag =>
          tagIndex.update(tag, tagIndex.getOrElse(tag, Set.empty) + id)
        }
      }
    } catch {
      case e: Exception =>
        println(s"Error loading cell from file $file: ${e.getMessage}")
        e.printStackTrace()
    } finally {
      if (ois != null) {
        try {
          ois.close()
        } catch {
          case _: Exception => // Ignore close errors
        }
      }
    }
  }
}

/**
 * Companion object for creating persistent memory systems
 */
object PersistentMemorySystem {
  /**
   * Create a new persistent memory system
   */
  def make(baseDir: File): ZIO[Any, MemoryError, PersistentMemorySystem] = {
    for {
      runtime <- ZIO.runtime[Any]
      system = new PersistentMemorySystem(baseDir, runtime)
    } yield system
  }
  
  /**
   * Create a new persistent memory system with automatic cleanup
   */
  def makeWithAutomaticCleanup(
    baseDir: File,
    interval: JavaDuration,
    strategies: CleanupStrategy*
  ): ZIO[Any, MemoryError, PersistentMemorySystem] = {
    for {
      system <- make(baseDir)
      _ <- ZIO.foreach(strategies)(system.registerCleanupStrategy)
      _ <- system.enableAutomaticCleanup(interval)
    } yield system
  }
  
  /**
   * Create a time-based cleanup persistent memory system
   */
  def makeWithTimeBasedCleanup(
    baseDir: File,
    maxAge: JavaDuration,
    interval: JavaDuration = java.time.Duration.ofMinutes(5)
  ): ZIO[Any, MemoryError, PersistentMemorySystem] = {
    makeWithAutomaticCleanup(
      baseDir,
      interval,
      CleanupStrategy.timeBasedAccess(maxAge)
    )
  }
  
  /**
   * Create a size-based cleanup persistent memory system
   */
  def makeWithSizeBasedCleanup(
    baseDir: File,
    maxSize: Long,
    interval: JavaDuration = java.time.Duration.ofMinutes(5)
  ): ZIO[Any, MemoryError, PersistentMemorySystem] = {
    makeWithAutomaticCleanup(
      baseDir,
      interval,
      CleanupStrategy.sizeBasedCleanup(maxSize)
    )
  }
}