package com.agenticai.examples

import com.agenticai.core.memory.*
import zio.*
import zio.stream.*
import java.time.Instant

object ChatExample extends ZIOAppDefault:

  case class Message(
      sender: String,
      content: String,
      timestamp: Instant = Instant.now()
  )

  case class UserPreferences(
      theme: String,
      notifications: Boolean,
      language: String
  )

  def run = for
    // Create memory system
    memorySystem <- MemorySystem.make

    // Create cells for different types of data
    conversationCell <- memorySystem.createCell[Vector[Message]]("conversation")
    _                <- conversationCell.write(Vector(Message("System", "Welcome to the chat!")))
    _                <- conversationCell.addTag("conversation")

    preferencesCell <- memorySystem.createCell[UserPreferences]("preferences")
    _ <- preferencesCell.write(
      UserPreferences(
        theme = "dark",
        notifications = true,
        language = "en"
      )
    )
    _ <- preferencesCell.addTag("preferences")

    // Simulate chat interactions
    _ <- simulateChat(conversationCell)
  yield ()

  def simulateChat(conversationCell: MemoryCell[Vector[Message]]) = for
    // Simulate messages
    _ <- ZIO.foreach(
      List(
        Message("Alice", "Hello everyone!"),
        Message("Bob", "Hi Alice!"),
        Message("Alice", "How are you?"),
        Message("Bob", "I'm doing great, thanks!")
      )
    ) { message =>
      for
        current <- conversationCell.read.map(_.getOrElse(Vector.empty[Message]))
        _       <- conversationCell.write(current :+ message)
        _       <- ZIO.logInfo(s"New message from ${message.sender}: ${message.content}")
      yield ()
    }

    // Read final conversation
    finalConversation <- conversationCell.read
    _ <- ZIO.logInfo(s"Final conversation: ${finalConversation.getOrElse(Vector.empty)}")
  yield ()

  override val bootstrap: ZLayer[ZIOAppArgs, Any, Any] = ZLayer.succeed(new InMemorySystem())
