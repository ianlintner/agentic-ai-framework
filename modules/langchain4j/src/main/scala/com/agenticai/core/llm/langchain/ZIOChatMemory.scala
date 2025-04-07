package com.agenticai.core.llm.langchain

import dev.langchain4j.data.message.{ChatMessage, UserMessage, AiMessage}
import zio._
import scala.jdk.CollectionConverters._

/**
 * A ZIO wrapper for Langchain4j's chat memory.
 * This trait provides methods for managing conversation history.
 */
trait ZIOChatMemory {
  /**
   * Adds a user message to the conversation history.
   *
   * @param message The user's message
   * @return A ZIO effect that completes when the message is added
   */
  def addUserMessage(message: String): ZIO[Any, Throwable, Unit]
  
  /**
   * Adds an assistant message to the conversation history.
   *
   * @param message The assistant's message
   * @return A ZIO effect that completes when the message is added
   */
  def addAssistantMessage(message: String): ZIO[Any, Throwable, Unit]
  
  /**
   * Gets all messages in the conversation history.
   *
   * @return A ZIO effect that completes with the list of messages
   */
  def messages: ZIO[Any, Throwable, List[ChatMessage]]
  
  /**
   * Clears the conversation history.
   *
   * @return A ZIO effect that completes when the history is cleared
   */
  def clear(): ZIO[Any, Throwable, Unit]
}

/**
 * Implementation of ZIOChatMemory that uses ZIO Ref for in-memory storage.
 */
private class InMemoryZIOChatMemory(
  maxMessages: Int,
  ref: Ref[List[ChatMessage]]
) extends ZIOChatMemory {
  
  override def addUserMessage(message: String): ZIO[Any, Throwable, Unit] = {
    ref.update { messages =>
      (UserMessage.from(message) :: messages).take(maxMessages)
    }
  }
  
  override def addAssistantMessage(message: String): ZIO[Any, Throwable, Unit] = {
    ref.update { messages =>
      (AiMessage.from(message) :: messages).take(maxMessages)
    }
  }
  
  override def messages: ZIO[Any, Throwable, List[ChatMessage]] = {
    ref.get.map(_.reverse) // Return in chronological order
  }
  
  override def clear(): ZIO[Any, Throwable, Unit] = {
    ref.set(List.empty)
  }
}

/**
 * Factory object for creating ZIOChatMemory instances.
 */
object ZIOChatMemory {
  /**
   * Creates a new in-memory chat memory.
   *
   * @param maxMessages The maximum number of messages to store
   * @return A ZIO effect that completes with a new ZIOChatMemory
   */
  def createInMemory(maxMessages: Int): ZIO[Any, Nothing, ZIOChatMemory] =
    for {
      ref <- Ref.make(List.empty[ChatMessage])
    } yield new InMemoryZIOChatMemory(maxMessages, ref)
  
  /**
   * Creates a ZLayer that provides an in-memory ZIOChatMemory.
   *
   * @param maxMessages The maximum number of messages to store
   * @return A ZLayer that provides a ZIOChatMemory
   */
  def inMemoryLayer(maxMessages: Int): ZLayer[Any, Nothing, ZIOChatMemory] =
    ZLayer.fromZIO(createInMemory(maxMessages))
}
