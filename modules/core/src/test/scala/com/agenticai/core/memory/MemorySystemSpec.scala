package com.agenticai.core.memory

import zio._
import zio.test._
import zio.test.Assertion._
import zio.test.TestAspect._

object MemorySystemSpec extends ZIOSpecDefault {
  def spec = suite("MemorySystem")(
    test("createCell creates a new memory cell") {
      for {
        system <- MemorySystem.make
        cell <- system.createCell("test")
        value <- cell.read
      } yield assertTrue(value.contains("test"))
    },
    test("createCellWithTags creates a cell with tags") {
      for {
        system <- MemorySystem.make
        cell <- system.createCellWithTags("test", Set("tag1", "tag2"))
        meta <- cell.metadata
      } yield assertTrue(meta.tags == Set("tag1", "tag2"))
    },
    test("getCellsByTag returns cells with matching tag") {
      for {
        system <- MemorySystem.make
        cell1 <- system.createCellWithTags("test1", Set("tag1"))
        cell2 <- system.createCellWithTags("test2", Set("tag1", "tag2"))
        cells <- system.getCellsByTag("tag1")
      } yield assertTrue(cells.size == 2 && cells.contains(cell1) && cells.contains(cell2))
    },
    test("getAllCells returns all created cells") {
      for {
        system <- MemorySystem.make
        cell1 <- system.createCell("test1")
        cell2 <- system.createCell("test2")
        cells <- system.getAllCells
      } yield assertTrue(cells.size == 2 && cells.contains(cell1) && cells.contains(cell2))
    },
    test("clearAll clears all cells") {
      for {
        system <- MemorySystem.make
        cell1 <- system.createCell("test1")
        cell2 <- system.createCell("test2")
        _ <- system.clearAll
        cells <- system.getAllCells
      } yield assertTrue(cells.isEmpty)
    }
  ) @@ TestAspect.sequential
} 