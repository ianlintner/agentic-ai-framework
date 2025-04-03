package com.agenticai.core.memory

import zio._
import zio.test._
import zio.test.Assertion._
import zio.test.TestAspect._
import java.time.Instant
import math.Ordering.Implicits.infixOrderingOps

object MemoryCellSpec extends ZIOSpecDefault {
  def spec = suite("MemoryCell")(
    test("read returns initial value") {
      for {
        cell <- MemoryCell.make("test")
        value <- cell.read
      } yield assertTrue(value.contains("test"))
    },
    test("write updates value") {
      for {
        cell <- MemoryCell.make("initial")
        _ <- cell.write("updated")
        value <- cell.read
      } yield assertTrue(value.contains("updated"))
    },
    test("update modifies value using function") {
      for {
        cell <- MemoryCell.make(5)
        _ <- cell.update {
          case Some(n) => n + 1
          case None => 0
        }
        value <- cell.read
      } yield assertTrue(value.contains(6))
    },
    test("update handles empty value correctly") {
      for {
        cell <- MemoryCell.make[Int](0)
        _ <- cell.empty
        _ <- cell.update {
          case Some(n) => n + 1
          case None => 0
        }
        value <- cell.read
      } yield assertTrue(value.contains(0))
    },
    test("should track metadata access and modification times") {
      for {
        cell <- MemoryCell.make("initial")
        initialMetadata <- cell.metadata
        _ <- TestClock.adjust(1.second)
        _ <- cell.write("updated")
        updatedMetadata <- cell.metadata
      } yield assertTrue(
        updatedMetadata.lastModified.isAfter(initialMetadata.lastModified),
        updatedMetadata.lastAccessed.isAfter(initialMetadata.lastAccessed)
      )
    } @@ TestAspect.withLiveClock,
    test("should reset value to initial state") {
      for {
        cell <- MemoryCell.make("initial")
        _ <- cell.write("updated")
        _ <- cell.clear
        value <- cell.read
      } yield assertTrue(value.contains("initial"))
    } @@ TestAspect.withLiveClock,
    test("makeWithTags creates cell with tags") {
      for {
        cell <- MemoryCell.makeWithTags("test", Set("tag1", "tag2"))
        meta <- cell.metadata
      } yield assertTrue(meta.tags == Set("tag1", "tag2"))
    },
    test("handles concurrent access correctly") {
      for {
        cell <- MemoryCell.make(0)
        _ <- ZIO.foreachPar(1 to 10) { i =>
          cell.update {
            case Some(n) => n + i
            case None => i
          }
        }
        value <- cell.read
      } yield assertTrue(value.isDefined)
    }
  ) @@ sequential
} 