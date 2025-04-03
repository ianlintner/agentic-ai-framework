package com.agenticai.example

import zio._
import zio.test.*
import zio.test.Assertion.*
import zio.test.TestAspect.*
import java.io.File
import java.nio.file.Files
import java.time.Instant
import java.lang.System

object FileModificationServiceSpec extends ZIOSpecDefault {
  def spec = suite("FileModificationService")(
    suite("File Operations")(
      test("should read and write files") {
        val service = new FileModificationService()
        val testFile = new File("test.txt")
        
        for {
          // Setup
          _ <- TestClock.setTime(Instant.parse("2023-01-01T00:00:00Z"))
          _ <- ZIO.attempt(Files.write(testFile.toPath, "test content".getBytes))
          
          // Test reading
          content <- service.readFile(testFile)
          _ <- TestClock.adjust(1.second)
          
          // Test writing
          _ <- service.writeFile(testFile, "modified content")
          modifiedContent <- service.readFile(testFile)
          
          // Cleanup
          _ <- ZIO.attempt(testFile.delete())
        } yield assertTrue(
          content == "test content",
          modifiedContent == "modified content"
        )
      } @@ TestAspect.withLiveClock,
      
      test("should create backups before modifications") {
        val service = new FileModificationService()
        val testFile = new File(java.lang.System.getProperty("java.io.tmpdir"), "test.txt")
        
        for {
          // Setup
          _ <- ZIO.attempt(Files.write(testFile.toPath, "original content".getBytes))
          
          // Modify file
          _ <- service.writeFile(testFile, "modified content")
          
          // Find backup file
          backupFiles <- ZIO.attempt {
            val parentDir = testFile.getParentFile
            if (parentDir != null && parentDir.exists()) {
              Option(parentDir.listFiles())
                .map(_.filter(_.getName.matches(s"${testFile.getName}\\.\\d{8}_\\d{6}\\.bak")))
                .getOrElse(Array.empty[File])
            } else {
              Array.empty[File]
            }
          }
          
          // Read backup content
          _ <- ZIO.fail(new RuntimeException("No backup file found")).when(backupFiles.isEmpty)
          backupContent <- ZIO.attempt(Files.readString(backupFiles.head.toPath))
          
          // Cleanup
          _ <- ZIO.attempt {
            testFile.delete()
            backupFiles.foreach(_.delete())
          }
        } yield assertTrue(
          backupFiles.length == 1,
          backupContent == "original content"
        )
      } @@ TestAspect.withLiveClock,

      test("should restore from backup") {
        val service = new FileModificationService()
        val timestamp = System.currentTimeMillis().toString
        val testDir = new File(java.lang.System.getProperty("java.io.tmpdir"), s"test_dir_$timestamp")
        val testFile = new File(testDir, "test.txt")
        // Use a dedicated file name for this test to avoid conflicts
        val testContent = "original content for restore test"
        
        // Create a fresh test directory for this specific test
        ZIO.scoped {
          for {
            // Setup in a scope to ensure cleanup happens
            _ <- ZIO.acquireRelease(
              ZIO.attemptBlocking {
                // Create fresh directory
                if (testDir.exists()) {
                  testDir.listFiles().foreach(_.delete())
                  testDir.delete()
                }
                testDir.mkdirs()
                // Create initial test file
                Files.write(testFile.toPath, testContent.getBytes)
                testDir // Return the directory for cleanup
              }
            )(_ => ZIO.attemptBlocking {
              // Cleanup - Delete all files in directory and the directory itself
              if (testDir.exists() && testDir.isDirectory) {
                testDir.listFiles().foreach(_.delete())
                testDir.delete()
              }
            }.orDie)
            
            // Modify and get backup
            _ <- service.writeFile(testFile, "modified content for restore test")
            backupFiles <- ZIO.attemptBlocking {
              testDir.listFiles().filter(_.getName.matches(s"${testFile.getName}\\.\\d{8}_\\d{6}\\.bak"))
            }
            _ <- ZIO.fail(new RuntimeException("No backup files found")).when(backupFiles.isEmpty)
            
            // Restore from backup
            _ <- service.restoreFromBackup(backupFiles(0))
            restoredContent <- service.readFile(testFile)
          } yield assertTrue(restoredContent == testContent)
        }
      } @@ TestAspect.withLiveClock
    ),
    
    suite("Security")(
      test("should prevent access outside project directory") {
        val service = new FileModificationService()
        val outsideFile = new File("/tmp/test.txt")
        
        for {
          result <- service.readFile(outsideFile).either
        } yield assertTrue(
          result.isLeft,
          result.left.exists(_.isInstanceOf[SecurityException])
        )
      }
    )
  )
} 