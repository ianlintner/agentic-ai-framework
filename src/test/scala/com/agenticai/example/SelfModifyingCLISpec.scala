package com.agenticai.example

import zio.*
import zio.test.*
import zio.test.Assertion.*
import zio.test.TestAspect.*
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths
import scala.io.Source
import java.lang.System

object SelfModifyingCLISpec extends ZIOSpecDefault:

  def spec = suite("SelfModifyingCLI")(
    test("parseCommand correctly identifies commands") {
      val cli = SelfModifyingCLI

      assertTrue(
        cli.parseCommand("help") == SelfModifyingCLI.Command.Help,
        cli.parseCommand("show test.scala") == SelfModifyingCLI.Command.Show("test.scala"),
        cli.parseCommand("modify test.scala add a comment") == SelfModifyingCLI.Command
          .Modify("test.scala", "add a comment"),
        cli.parseCommand("restore test.scala") == SelfModifyingCLI.Command.Restore("test.scala"),
        cli.parseCommand("exit") == SelfModifyingCLI.Command.Exit
      )
    },
    test("FileModificationService creates backups and validates paths") {
      val service   = new FileModificationService()
      val timestamp = System.currentTimeMillis().toString
      val testDir   = new File(System.getProperty("java.io.tmpdir"), s"test_dir_backup_$timestamp")
      val testFile  = new File(testDir, "test_backup.txt")
      val testContent = "test content for backups test"

      ZIO.scoped {
        for
          // Setup in a scope to ensure cleanup happens
          _ <- ZIO.acquireRelease(
            ZIO.attemptBlocking {
              // Create fresh directory
              if testDir.exists() then
                testDir.listFiles().foreach(_.delete())
                testDir.delete()
              testDir.mkdirs()
              // Create initial test file
              Files.write(testFile.toPath, testContent.getBytes)
              testDir // Return the directory for cleanup
            }
          )(_ =>
            ZIO.attemptBlocking {
              // Cleanup - Delete all files in directory and the directory itself
              if testDir.exists() && testDir.isDirectory then
                testDir.listFiles().foreach(_.delete())
                testDir.delete()
            }.orDie
          )

          // Test backup creation through writeFile
          _ <- service.writeFile(testFile, "modified content for backup test")
          backupFiles <- ZIO.attemptBlocking {
            testDir
              .listFiles()
              .filter(_.getName.matches(s"${testFile.getName}\\.\\d{8}_\\d{6}\\.bak"))
          }
          _ = assertTrue(backupFiles.length == 1)

          // Test file reading
          content <- service.readFile(testFile)
          _ = assertTrue(content == "modified content for backup test")

          // Test restore
          _               <- service.restoreFromBackup(backupFiles(0))
          restoredContent <- service.readFile(testFile)
        yield assertTrue(restoredContent == testContent)
      }
    } @@ withLiveClock,
    test("should modify its own source code") {
      val service     = new FileModificationService()
      val timestamp   = java.util.UUID.randomUUID().toString // Use UUID for better uniqueness
      val testDir     = new File(System.getProperty("java.io.tmpdir"), s"test_dir_$timestamp")
      val testFile    = new File(testDir, "test.txt")
      val testContent = "test content for cli test"

      ZIO.scoped {
        for
          // Setup directory in a scope with guaranteed cleanup
          dirAndFile <- ZIO.acquireRelease(
            ZIO.attemptBlocking {
              // Create fresh directory
              if testDir.exists() then
                testDir.listFiles().foreach(_.delete())
                testDir.delete()
              val success = testDir.mkdirs()
              if !success then
                throw new RuntimeException(
                  s"Failed to create test directory at ${testDir.getAbsolutePath}"
                )
              // Create initial test file
              Files.write(testFile.toPath, testContent.getBytes)
              // Verify the file exists and has content
              if !Files.exists(testFile.toPath) || Files.size(testFile.toPath) == 0 then
                throw new RuntimeException(
                  s"Failed to write test content to ${testFile.getAbsolutePath}"
                )
              (testDir, testFile) // Return both for use in operations
            }
          )(dirAndFile =>
            ZIO.attemptBlocking {
              val (dir, _) = dirAndFile
              // Cleanup - Delete all files in directory and the directory itself
              if dir.exists() && dir.isDirectory then
                dir.listFiles().foreach(_.delete())
                dir.delete()
            }.orDie
          )

          dir  = dirAndFile._1
          file = dirAndFile._2

          // Read file
          content <- service.readFile(file)
          _ = assertTrue(content == testContent)

          // Modify file
          _               <- service.writeFile(file, "modified content for cli test")
          modifiedContent <- service.readFile(file)
          _ = assertTrue(modifiedContent == "modified content for cli test")

          // Find backup files
          backupFiles <- ZIO.attemptBlocking {
            dir.listFiles().filter(_.getName.matches(s"${file.getName}\\.\\d{8}_\\d{6}\\.bak"))
          }
          _ <- ZIO
            .fail(new RuntimeException(s"No backup files found in ${dir.getAbsolutePath}"))
            .when(backupFiles.isEmpty)

          // Restore from backup
          _               <- service.restoreFromBackup(backupFiles(0))
          restoredContent <- service.readFile(file)
        yield assertTrue(restoredContent == testContent)
      }
    } @@ withLiveClock
  )
