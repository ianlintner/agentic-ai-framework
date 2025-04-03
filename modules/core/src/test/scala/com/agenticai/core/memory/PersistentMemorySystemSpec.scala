package com.agenticai.core.memory

import zio._
import zio.test._
import zio.test.Assertion._
import java.io.File
import java.nio.file.Files

object PersistentMemorySystemSpec extends ZIOSpecDefault {
  def spec = suite("PersistentMemorySystem")(
    test("should persist and load cells") {
      val tempDir = Files.createTempDirectory("memory-test").toFile
      tempDir.deleteOnExit()

      for {
        system <- PersistentMemorySystem.make(tempDir)
        cell1 <- system.createCell("test1")
        cell2 <- system.createCellWithTags("test2", Set("tag1", "tag2"))
        _ <- cell1.write("updated1")
        _ <- cell2.write("updated2")
        
        // Create a new system instance to test loading
        newSystem <- PersistentMemorySystem.make(tempDir)
        loadedCells <- newSystem.getAllCells
        
        // Verify cells were loaded correctly
        loadedValues <- ZIO.foreach(loadedCells)(_.read)
        _ <- ZIO.debug(s"Loaded values: $loadedValues")
        loadedCell1 <- ZIO.succeed(loadedValues.exists {
          case Some(value: String) => 
            val matches = value == "updated1"
            matches
          case _ => false
        })
        _ <- ZIO.debug(s"Loaded cell 1 matches: $loadedCell1")
        loadedCell2 <- ZIO.succeed(loadedValues.exists {
          case Some(value: String) => 
            val matches = value == "updated2"
            matches
          case _ => false
        })
        _ <- ZIO.debug(s"Loaded cell 2 matches: $loadedCell2")
        
        // Clean up
        _ <- newSystem.clearAll
      } yield assertTrue(
        loadedCell1,
        loadedCell2,
        loadedCells.size == 2
      )
    },
    
    test("should maintain tags across persistence") {
      val tempDir = Files.createTempDirectory("memory-test").toFile
      tempDir.deleteOnExit()

      for {
        system <- PersistentMemorySystem.make(tempDir)
        _ <- system.createCellWithTags("test1", Set("tag1"))
        _ <- system.createCellWithTags("test2", Set("tag1", "tag2"))
        
        // Create a new system instance to test loading
        newSystem <- PersistentMemorySystem.make(tempDir)
        taggedCells <- newSystem.getCellsByTag("tag1")
        taggedValues <- ZIO.foreach(taggedCells)(_.read)
        
        // Clean up
        _ <- newSystem.clearAll
      } yield assertTrue(
        taggedCells.size == 2,
        taggedValues.forall(v => v == Some("test1") || v == Some("test2"))
      )
    },
    
    test("should handle clearAll correctly") {
      val tempDir = Files.createTempDirectory("memory-test").toFile
      tempDir.deleteOnExit()

      for {
        system <- PersistentMemorySystem.make(tempDir)
        _ <- system.createCell("test1")
        _ <- system.createCellWithTags("test2", Set("tag1"))
        _ <- system.clearAll
        
        // Create a new system instance to verify clearing
        newSystem <- PersistentMemorySystem.make(tempDir)
        cells <- newSystem.getAllCells
        taggedCells <- newSystem.getCellsByTag("tag1")
      } yield assertTrue(
        cells.isEmpty,
        taggedCells.isEmpty
      )
    }
  )
} 