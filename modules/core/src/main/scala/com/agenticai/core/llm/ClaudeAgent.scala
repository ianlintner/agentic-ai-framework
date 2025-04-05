package com.agenticai.core.llm

import zio._
import zio.stream._
import com.agenticai.core.memory._

import java.time.Instant
import scala.annotation.tailrec
import scala.collection.mutable

/**
 * Agent for interacting with Claude via Vertex AI
 * Provides memory-based context management and conversation persistence
 */
class ClaudeAgent(
  val name: String,
  val client: VertexAIClient,
  val memory: MemorySystem,
  val maxContextSize: Int = 8192,
  val maxHistoryTurns: Int = 10
) {
  
  // Since the MemorySystem doesn't support tags, we'll use a naming convention
  private val conversationPrefix = s"$name:conversation:"
  private val contextPrefix = s"$name:context:"
  
  // In-memory map to track our cells since MemorySystem is simpler than expected
  private val conversationCells = mutable.Map[String, String]()
  
  /**
   * Process user input and generate a response
   * Stores conversation in memory for context
   * 
   * @param input User input text to process
   * @param timestampOpt Optional timestamp for testing; defaults to current time
   */
  def process(input: String, timestampOpt: Option[Instant] = None): ZStream[Any, Throwable, String] = {
    for {
      _ <- ZStream.fromZIO(saveUserMessage(input, timestampOpt))
      promptWithContext <- ZStream.fromZIO(buildContextualPrompt(input))
      response <- client.streamCompletion(promptWithContext)
      responseTimestampOpt = timestampOpt.map(ts => Instant.ofEpochMilli(ts.toEpochMilli + 100)) // Add small offset for response
      _ <- ZStream.fromZIO(saveAssistantMessage(response, responseTimestampOpt))
    } yield response
  }
  
  /**
   * Stream a response for the given input
   * Same as process but with more explicit naming for streaming
   * 
   * @param input User input text to process
   * @param timestampOpt Optional timestamp for testing; defaults to current time
   */
  def generateStream(input: String, timestampOpt: Option[Instant] = None): ZStream[Any, Throwable, String] = 
    process(input, timestampOpt)
  
  /**
   * Save a user message to memory
   * 
   * @param message The message text to save
   * @param timestampOpt Optional timestamp for testing; defaults to current time
   */
  def saveUserMessage(message: String, timestampOpt: Option[Instant] = None): ZIO[Any, Throwable, Unit] = {
    for {
      timestamp <- timestampOpt match {
                     case Some(ts) => ZIO.succeed(ts)
                     case None => ZIO.clockWith(_.instant)
                   }
      turn = ConversationTurn(Role.User, message, timestamp)
      cellName = s"$conversationPrefix:user:${timestamp.toEpochMilli}"
      cell <- memory.createCell[ConversationTurn](cellName)
      _ <- cell.write(turn)
      _ <- ZIO.succeed(conversationCells.put(cellName, "user"))
    } yield ()
  }
  
  /**
   * Save an assistant message to memory
   * 
   * @param message The message text to save
   * @param timestampOpt Optional timestamp for testing; defaults to current time
   */
  def saveAssistantMessage(message: String, timestampOpt: Option[Instant] = None): ZIO[Any, Throwable, Unit] = {
    if (message.trim.isEmpty) {
      ZIO.unit
    } else {
      for {
        timestamp <- timestampOpt match {
                       case Some(ts) => ZIO.succeed(ts)
                       case None => ZIO.clockWith(_.instant)
                     }
        turn = ConversationTurn(Role.Assistant, message, timestamp)
        cellName = s"$conversationPrefix:assistant:${timestamp.toEpochMilli}"
        cell <- memory.createCell[ConversationTurn](cellName)
        _ <- cell.write(turn)
        _ <- ZIO.succeed(conversationCells.put(cellName, "assistant"))
      } yield ()
    }
  }
  
  /**
   * Get conversation history from memory using our naming convention
   */
  def getConversationHistory: ZIO[Any, Throwable, List[ConversationTurn]] = {
    ZIO.foldLeft(conversationCells.keys.toList)(List.empty[ConversationTurn]) { (acc, cellName) =>
      val name: String = cellName  // Ensure cellName is treated as String
      for {
        cellOpt <- memory.getCell[ConversationTurn](name)
        turns <- cellOpt match {
          case Some(cell) => cell.read.map(turn => acc ++ turn.toList)
          case None => ZIO.succeed(acc)
        }
      } yield turns
    }.map(_.sortBy(_.timestamp))
  }
  
  /**
   * Build a contextual prompt by including relevant conversation history
   */
  def buildContextualPrompt(userInput: String): ZIO[Any, Throwable, String] = {
    for {
      history <- getConversationHistory
      recentHistory = truncateHistory(history, maxHistoryTurns)
      prompt = formatConversationAsPrompt(recentHistory, userInput)
    } yield prompt
  }
  
  /**
   * Format the conversation history as a prompt for Claude
   * Uses the standard Claude conversation format
   */
  def formatConversationAsPrompt(history: List[ConversationTurn], currentInput: String): String = {
    val contextBuilder = new StringBuilder()
    
    history.foreach { turn =>
      val roleLabel = turn.role match {
        case Role.User => "Human"
        case Role.Assistant => "Assistant"
        case _ => "System"
      }
      
      contextBuilder.append(s"$roleLabel: ${turn.content}\n\n")
    }
    
    // Add the current user input
    contextBuilder.append(s"Human: $currentInput\n\n")
    contextBuilder.append("Assistant: ")
    
    contextBuilder.toString
  }
  
  /**
   * Truncate conversation history to the most recent turns
   * A turn is defined as a complete exchange (user message + assistant response)
   */
  def truncateHistory(history: List[ConversationTurn], maxTurns: Int): List[ConversationTurn] = {
    // Calculate how many messages to keep (2 messages per turn: user + assistant)
    val messagesPerTurn = 2
    val messagesToKeep = maxTurns * messagesPerTurn
    
    // Take most recent entries up to messagesToKeep
    history.takeRight(messagesToKeep)
  }
  
  /**
   * Clear conversation history from memory
   */
  def clearConversationHistory: ZIO[Any, Throwable, Unit] = {
    ZIO.foreachDiscard(conversationCells.keys.toList) { cellName =>
      val name: String = cellName  // Ensure cellName is treated as String
      memory.deleteCell(name)
    } *> ZIO.succeed(conversationCells.clear())
  }
  
  /**
   * Store additional context information
   */
  def addContext(key: String, value: String): ZIO[Any, Throwable, Unit] = {
    for {
      cellName <- ZIO.succeed(s"$contextPrefix$key")
      cell <- memory.createCell[String](cellName)
      _ <- cell.write(value)
    } yield ()
  }
  
  /**
   * Retrieve context information
   */
  def getContext(key: String): ZIO[Any, Throwable, Option[String]] = {
    for {
      cellName <- ZIO.succeed(s"$contextPrefix$key")
      cellOpt <- memory.getCell[String](cellName)
      value <- cellOpt match {
        case Some(cell) => cell.read
        case None => ZIO.succeed(None)
      }
    } yield value
  }
}

/**
 * Conversation turn representing a single message in a conversation
 */
case class ConversationTurn(
  role: Role,
  content: String,
  timestamp: Instant
)

/**
 * Enum-like structure for role in conversation
 */
sealed trait Role
object Role {
  case object User extends Role
  case object Assistant extends Role
  case object System extends Role
}

/**
 * Companion object for creating Claude agents
 */
object ClaudeAgent {
  /**
   * Create a new Claude agent with an existing VertexAI client
   */
  def make(
    client: VertexAIClient,
    name: String = "claude",
    memorySystem: MemorySystem = null,
    maxContextSize: Int = 8192,
    maxHistoryTurns: Int = 10
  ): ZIO[MemorySystem, Nothing, ClaudeAgent] = {
    for {
      memory <- if (memorySystem != null) ZIO.succeed(memorySystem) else ZIO.service[MemorySystem]
    } yield new ClaudeAgent(name, client, memory, maxContextSize, maxHistoryTurns)
  }
  
  /**
   * Create a new Claude agent with explicit configuration
   */
  def makeWithConfig(
    projectId: String,
    location: String = "us-central1",
    modelId: String = "claude-3-sonnet-20240229",
    name: String = "claude",
    memorySystem: MemorySystem = null,
    maxContextSize: Int = 8192,
    maxHistoryTurns: Int = 10
  ): ZIO[MemorySystem, Nothing, ClaudeAgent] = {
    val client = VertexAIClient.make(projectId, location, modelId)
    make(client, name, memorySystem, maxContextSize, maxHistoryTurns)
  }
  
  /**
   * Create a Claude agent using environment-defined configuration
   */
  def makeDefault(name: String = "claude"): ZIO[MemorySystem, Throwable, ClaudeAgent] = {
    for {
      // Get project ID directly as a String
      projectIdStr <- System.env("GOOGLE_CLOUD_PROJECT").flatMap { 
        case Some(id) => ZIO.succeed(id)
        case None => ZIO.fail(new RuntimeException("GOOGLE_CLOUD_PROJECT not set"))
      }
      
      // Get optional configs with defaults
      locationOpt <- System.env("VERTEX_LOCATION")
      location = locationOpt.getOrElse("us-central1")
      modelIdOpt <- System.env("CLAUDE_MODEL_ID")
      modelId = modelIdOpt.getOrElse("claude-3-sonnet-20240229")
      
      // Get memory system
      memory <- ZIO.service[MemorySystem]
      
      // Create the client with properly typed parameters
      client = VertexAIClient.make(
        projectId = projectIdStr, 
        location = location, 
        modelId = modelId
      )
      
      // Create the agent with client and memory
      claudeAgent = new ClaudeAgent(
        name = name,
        client = client,
        memory = memory
      )
    } yield claudeAgent
  }
  
  /**
   * Create a new Claude agent as a ZLayer
   */
  def live(name: String = "claude"): ZLayer[MemorySystem, Throwable, ClaudeAgent] = {
    ZLayer.fromZIO(makeDefault(name))
  }
}
