package com.agenticai.core.memory

import zio._
import zio.test._
import zio.test.Assertion._

object MemorySystemSpec extends ZIOSpecDefault {
  def spec = suite("MemorySystem")(
    suite("Memory Cell Operations")(
      test("should create and retrieve cells") {
        val system = new InMemorySystem()
        
        for {
          cell <- system.createCell[String]("test")
          retrieved <- system.getCell[String]("test")
        } yield assertTrue(retrieved.contains(cell))
      },
      
      test("should write and read values") {
        val system = new InMemorySystem()
        
        for {
          cell <- system.createCell[String]("test")
          _ <- cell.write("hello")
          value <- cell.read
        } yield assertTrue(value.contains("hello"))
      },
      
      test("should clear values") {
        val system = new InMemorySystem()
        
        for {
          cell <- system.createCell[String]("test")
          _ <- cell.write("hello")
          _ <- cell.clear
          value <- cell.read
        } yield assertTrue(value.isEmpty)
      },
      
      test("should delete cells") {
        val system = new InMemorySystem()
        
        for {
          _ <- system.createCell[String]("test")
          _ <- system.deleteCell("test")
          cell <- system.getCell[String]("test")
        } yield assertTrue(cell.isEmpty)
      }
    ),
    
    suite("Type Safety")(
      test("should maintain type safety across operations") {
        val system = new InMemorySystem()
        
        for {
          cell <- system.createCell[Int]("number")
          _ <- cell.write(42)
          value <- cell.read
          retrieved <- system.getCell[Int]("number")
          retrievedValue <- retrieved.get.read
        } yield assertTrue(
          value.contains(42),
          retrievedValue.contains(42)
        )
      }
    )
  )
} 