package com.agenticai.examples.selfmodifying

import zio.*
import java.io.{File, IOException}

/** Simple command-line interface for a self-modifying application
  */
object SelfModifyingCLI extends ZIOAppDefault:

  private def getUserInput(prompt: String): ZIO[Any, Throwable, String] =
    for
      _     <- Console.print(s"$prompt: ")
      input <- Console.readLine.orDie
    yield input

  private val fileService = new FileModificationService()

  private val mainLoop: ZIO[Any, Throwable, Unit] =
    for
      _ <- Console.printLine("\n=== Self-Modifying CLI ===")
      _ <- Console.printLine("1. View file content")
      _ <- Console.printLine("2. Modify file content")
      _ <- Console.printLine("3. Restore from backup")
      _ <- Console.printLine("4. Exit")
      _ <- Console.print("Enter choice: ")

      choice <- Console.readLine.orDie
      _      <- handleChoice(choice)
    yield ()

  private def handleChoice(choice: String): ZIO[Any, Throwable, Unit] =
    choice match
      case "1" => viewFileContent.catchAll(handleError) *> mainLoop
      case "2" => modifyFileContent.catchAll(handleError) *> mainLoop
      case "3" => restoreFromBackup.catchAll(handleError) *> mainLoop
      case "4" => Console.printLine("Exiting...") *> ZIO.unit
      case _   => Console.printLine("Invalid choice") *> mainLoop

  private def viewFileContent: ZIO[Any, Throwable, Unit] =
    for
      filePath <- getUserInput("Enter file path to view")
      file     = new File(filePath)
      content  <- fileService.readFile(file)
      _        <- Console.printLine(s"\nFile content:\n$content")
    yield ()

  private def modifyFileContent: ZIO[Any, Throwable, Unit] =
    for
      filePath <- getUserInput("Enter file path to modify")
      file     = new File(filePath)
      _        <- fileService.readFile(file) // Check file exists and is readable

      _ <- Console.printLine("\nEnter new content (empty line to finish):")
      newContent <- readMultilineInput

      _ <- fileService.writeFile(file, newContent)
      _ <- Console.printLine(s"File updated successfully")
    yield ()

  private def restoreFromBackup: ZIO[Any, Throwable, Unit] =
    for
      backupPath <- getUserInput("Enter backup file path")
      backupFile = new File(backupPath)
      _          <- fileService.restoreFromBackup(backupFile)
      _          <- Console.printLine("Restore successful")
    yield ()

  private def readMultilineInput: ZIO[Any, Throwable, String] =
    def loop(lines: List[String]): ZIO[Any, Throwable, List[String]] =
      for
        line <- Console.readLine.orDie
        result <-
          if line.isEmpty && lines.nonEmpty then ZIO.succeed(lines.reverse)
          else loop(line :: lines)
      yield result

    loop(List.empty).map(_.mkString("\n"))

  private def handleError(e: Throwable): ZIO[Any, Throwable, Unit] =
    Console.printLine(s"Error: ${e.getMessage}")

  def run: ZIO[Any, Throwable, Unit] =
    for
      _ <- Console.printLine("Self-Modifying CLI started")
      _ <- mainLoop.catchAll(e => Console.printLine(s"Fatal error: ${e.getMessage}"))
    yield ()
