package com.agenticai.examples.selfmodifying

import zio.*
import java.io.File
import scala.io.Source
import java.nio.file.{Files, Paths, StandardCopyOption}
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.lang.System
import java.io.FileNotFoundException

/** Service for safely modifying files with backup and validation
  */
class FileModificationService:

  // Create a backup of a file before modification
  private def createBackup(file: File): ZIO[Any, Throwable, File] =
    for
      timestamp <- ZIO.succeed(
        LocalDateTime.now.format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"))
      )
      backupPath = s"${file.getPath}.$timestamp.bak"
      backupFile = new File(backupPath)
      _ <- ZIO.attempt {
        // Create parent directories if they don't exist
        val parentFile = file.getParentFile
        if parentFile != null && !parentFile.exists() then parentFile.mkdirs()
        val backupParent = backupFile.getParentFile
        if backupParent != null && !backupParent.exists() then backupParent.mkdirs()

        // Create original file if it doesn't exist
        if !file.exists() then
          file.getParentFile.mkdirs() // Ensure parent directory exists
          file.createNewFile()
          Files.write(file.toPath, Array[Byte]())
      }
      // Copy the file to backup with retry logic
      _ <- ZIO
        .attempt {
          Files.copy(file.toPath, backupFile.toPath, StandardCopyOption.REPLACE_EXISTING)
        }
        .retry(Schedule.recurs(3) && Schedule.exponential(100.milliseconds))
        .tapError(e => ZIO.logWarning(s"Failed to create backup: ${e.getMessage}"))
      _ <- ZIO.logInfo(s"Created backup at: $backupPath")
    yield backupFile

  // Validate that a file is within the project directory or temp directory
  private def validateFilePath(file: File): ZIO[Any, Throwable, Unit] =
    for
      projectRoot <- ZIO.attempt(new File(".").getCanonicalFile)
      filePath    <- ZIO.attempt(file.getCanonicalFile)
      tempDir <- ZIO.attempt(
        new File(java.lang.System.getProperty("java.io.tmpdir")).getCanonicalFile
      )
      projectRootPath = projectRoot.getPath
      filePathStr     = filePath.getPath
      tempDirPath     = tempDir.getPath
      _ <-
        if filePathStr.startsWith(projectRootPath) || filePathStr.startsWith(tempDirPath) then
          ZIO.unit
        else
          ZIO.fail(
            new SecurityException(
              s"Access denied: $filePath is outside project directory and temp directory"
            )
          )
    yield ()

  // Read file contents
  def readFile(file: File): ZIO[Any, Throwable, String] =
    for
      _ <- validateFilePath(file)
      content <- ZIO.attempt {
        if !file.exists() then throw new FileNotFoundException(s"File not found: ${file.getPath}")
        val source = Source.fromFile(file)
        try
          source.mkString
        finally
          source.close()
      }
    yield content

  // Write file contents with backup
  def writeFile(file: File, content: String): ZIO[Any, Throwable, Unit] =
    for
      _ <- validateFilePath(file)
      _ <- ZIO.attempt {
        val parentFile = file.getParentFile
        if parentFile != null && !parentFile.exists() then parentFile.mkdirs()
      }
      _ <- createBackup(file)
      _ <- ZIO.attempt {
        Files.write(file.toPath, content.getBytes)
      }
      _ <- ZIO.logInfo(s"Successfully modified file: ${file.getPath}")
    yield ()

  // Apply a modification to a file
  def modifyFile(file: File, modification: String => String): ZIO[Any, Throwable, Unit] =
    for
      content <- readFile(file)
      newContent = modification(content)
      _ <- writeFile(file, newContent)
    yield ()

  // Restore from backup
  def restoreFromBackup(backup: File): ZIO[Any, Throwable, Unit] =
    for
      originalPath <- ZIO.succeed(backup.getPath.replaceAll("""\.\d{8}_\d{6}\.bak$""", ""))
      originalFile <- ZIO.attempt(new File(originalPath))
      _            <- validateFilePath(originalFile)
      _ <- ZIO
        .attempt {
          // Check if backup file exists
          if !backup.exists() then
            throw new FileNotFoundException(s"Backup file not found: ${backup.getPath}")

          // Create parent directory if it doesn't exist
          val parentFile = originalFile.getParentFile
          if parentFile != null && !parentFile.exists() then parentFile.mkdirs()

          // Create original file if it doesn't exist
          if !originalFile.exists() then originalFile.createNewFile()

          // Read backup content and write to original file
          val content = Files.readString(backup.toPath)
          Files.write(originalFile.toPath, content.getBytes)
        }
        .retry(Schedule.recurs(3) && Schedule.exponential(100.milliseconds))
        .tapError(e => ZIO.logWarning(s"Failed to restore from backup: ${e.getMessage}"))
      _ <- ZIO.logInfo(s"Restored file from backup: ${backup.getPath}")
    yield ()
