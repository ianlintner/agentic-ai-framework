package com.agenticai.examples.selfmodifying

import zio._
import zio.Console
import com.agenticai.core.llm._
import com.agenticai.core.memory._
import java.io.File
import scala.io.Source

/**
 * A command-line interface that can modify its own framework
 */
object SelfModifyingCLI extends ZIOAppDefault {
  
  // Command types for our CLI
  enum Command:
    case Help
    case Modify(file: String, instruction: String)
    case Show(file: String)
    case Restore(file: String)
    case Exit
    
  // CLI state
  case class CLIState(
    workingDir: File = new File("."),
    memory: MemorySystem = null,
    fileService: FileModificationService = new FileModificationService(),
    claudeAgent: Option[ClaudeAgent] = None
  )
  
  // Main program
  override def run = {
    for {
      memory <- MemorySystem.make
      _ <- Console.printLine("=== Self-Modifying CLI ===")
      _ <- Console.printLine("Type 'help' for available commands")
      _ <- runCLI(CLIState(memory = memory))
    } yield ()
  }
  
  // Main CLI loop
  def runCLI(state: CLIState): ZIO[Any, Throwable, Unit] = {
    for {
      _ <- Console.print("\n> ")
      input <- Console.readLine
      command = parseCommand(input)
      newState <- handleCommand(command, state)
      _ <- if (command == Command.Exit) ZIO.unit else runCLI(newState)
    } yield ()
  }
  
  // Parse user input into commands
  def parseCommand(input: String): Command = {
    input.trim.toLowerCase match {
      case "help" => Command.Help
      case "exit" => Command.Exit
      case s if s.startsWith("show ") => 
        Command.Show(s.drop(5).trim)
      case s if s.startsWith("modify ") =>
        val parts = s.drop(7).split(" ", 2)
        if (parts.length == 2) {
          Command.Modify(parts(0), parts(1))
        } else {
          Command.Help
        }
      case s if s.startsWith("restore ") =>
        Command.Restore(s.drop(8).trim)
      case _ => Command.Help
    }
  }
  
  // Handle CLI commands
  def handleCommand(command: Command, state: CLIState): ZIO[Any, Throwable, CLIState] = {
    command match {
      case Command.Help =>
        for {
          _ <- Console.printLine("Available commands:")
          _ <- Console.printLine("  help                    - Show this help message")
          _ <- Console.printLine("  show <file>             - Show contents of a file")
          _ <- Console.printLine("  modify <file> <instruction> - Modify a file based on instruction")
          _ <- Console.printLine("  restore <file>          - Restore file from latest backup")
          _ <- Console.printLine("  exit                    - Exit the CLI")
        } yield state
        
      case Command.Show(filePath) =>
        for {
          file <- ZIO.attempt(new File(state.workingDir, filePath))
          content <- state.fileService.readFile(file)
          _ <- Console.printLine(content)
        } yield state
        
      case Command.Modify(filePath, instruction) =>
        for {
          file <- ZIO.attempt(new File(state.workingDir, filePath))
          _ <- if (file.exists()) {
            for {
              // Get current content
              content <- state.fileService.readFile(file)
              _ <- Console.printLine("Current file contents:")
              _ <- Console.printLine(content)
              _ <- Console.printLine("\nModification instruction:")
              _ <- Console.printLine(instruction)
              
              // Get Claude's suggestion
              _ <- Console.printLine("\nGenerating modification with Claude...")
              agent <- getOrCreateClaudeAgent(state)
              response <- agent.process(instruction).runHead
                .someOrFail(new RuntimeException("No response from Claude"))
              
              _ <- Console.printLine("\nProposed modification:")
              _ <- Console.printLine(response)
              _ <- Console.printLine("\nWould you like to apply this modification? (y/n)")
              confirm <- Console.readLine
              
              _ <- if (confirm.toLowerCase == "y") {
                state.fileService.modifyFile(file, _ => response)
              } else {
                Console.printLine("Modification cancelled")
              }
            } yield ()
          } else {
            Console.printLine(s"File not found: $filePath")
          }
        } yield state
        
      case Command.Restore(filePath) =>
        for {
          file <- ZIO.attempt(new File(state.workingDir, filePath))
          _ <- if (file.exists()) {
            for {
              _ <- Console.printLine("Restoring from latest backup...")
              _ <- state.fileService.restoreFromBackup(file)
              _ <- Console.printLine("File restored successfully")
            } yield ()
          } else {
            Console.printLine(s"File not found: $filePath")
          }
        } yield state
        
      case Command.Exit =>
        for {
          _ <- Console.printLine("Goodbye!")
        } yield state
    }
  }
  
  // Get or create Claude agent using ZIO service pattern
  def getOrCreateClaudeAgent(state: CLIState): ZIO[Any, Throwable, ClaudeAgent] = {
    state.claudeAgent match {
      case Some(agent) => ZIO.succeed(agent)
      case None =>
        // Create a layer for the Claude agent
        val claudeLayer = ZLayer.succeed(VertexAIConfig.claudeDefault) >>> 
                          ClaudeAgent.live("Claude")
                          .tapError(e => ZIO.debug(s"Failed to create Claude agent: $e"))
        
        // Provide the memory system to the layer
        for {
          agent <- ZIO.service[ClaudeAgent].provideLayer(
            ZLayer.succeed(state.memory) >>> claudeLayer
          )
        } yield agent
    }
  }
}
