package com.agenticai.core.llm.langchain

import dev.langchain4j.memory.ChatMemory
import dev.langchain4j.memory.chat.MessageWindowChatMemory
import dev.langchain4j.data.message.{ChatMessage, UserMessage, AiMessage}
import zio._

/**
 * A ZIO wrapper for Langchain4j's ChatMemory interface.
 * This trait provides ZIO effects for interacting with conversation history.
 */
trait ZIOChatMemory {
  /**
   * Adds a user message to memory.
   *
   * @param message The message text to add
   * @return A ZIO effect that completes when the message is added
   */
  def addUserMessage(message: String): ZIO[Any, Throwable, Unit]
  
  /**
   * Adds an assistant message to memory.
   *
   * @param message The message text to add
   * @return A ZIO effect that completes when the message is added
   */
  def addAssistantMessage(message: String): ZIO[Any, Throwable, Unit]
  
  /**
   * Retrieves all messages in the conversation history.
   *
   * @return A ZIO effect that resolves to a list of chat messages
   */
  def messages: ZIO[Any, Throwable, List[ChatMessage]]
  
  /**
   * Clears all messages from memory.
   *
   * @return A ZIO effect that completes when the memory is cleared
   */
  def clear: ZIO[Any, Throwable, Unit]
}

/**
 * Live implementation of ZIOChatMemory that delegates to a Langchain4j ChatMemory.
 *
 * @param memory The underlying Langchain4j chat memory implementation
 */
final case class ZIOChatMemoryLive(memory: ChatMemory) extends ZIOChatMemory {
  override def addUserMessage(message: String): ZIO[Any, Throwable, Unit] =
    ZIO.attemptBlocking(memory.add(UserMessage.from(message)))
  
  override def addAssistantMessage(message: String): ZIO[Any, Throwable, Unit] =
    ZIO.attemptBlocking(memory.add(AiMessage.from(message)))
  
  override def messages: ZIO[Any, Throwable, List[ChatMessage]] =
    ZIO.attemptBlocking(memory.messages().toArray.toList.asInstanceOf[List[ChatMessage]])
  
  override def clear: ZIO[Any, Throwable, Unit] =
    ZIO.attemptBlocking(memory.clear())
}

object ZIOChatMemory {
  /**
   * Creates a windowed chat memory with the given maximum number of messages.
   *
   * @param maxMessages The maximum number of messages to store
   * @return A ZIO effect that resolves to a ZIOChatMemory
   */
  def createWindow(maxMessages: Int): UIO[ZIOChatMemory] =
    ZIO.succeed(ZIOChatMemoryLive(MessageWindowChatMemory.withMaxMessages(maxMessages)))
    
  /**
   * Creates a ZLayer for a windowed chat memory.
   *
   * @param maxMessages The maximum number of messages to store
   * @return A ZLayer that provides a ZIOChatMemory
   */
  def windowLayer(maxMessages: Int): ZLayer[Any, Nothing, ZIOChatMemory] =
    ZLayer.succeed(ZIOChatMemoryLive(MessageWindowChatMemory.withMaxMessages(maxMessages)))
}
