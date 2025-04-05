package com.agenticai.examples

import com.agenticai.core.memory._
import zio._
import zio.test._
import java.time.Instant

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

object ChatExampleSpec extends ZIOSpecDefault {
  def spec = suite("ChatExample")(
    test("should store and retrieve messages") {
      for {
        memorySystem <- ZIO.service[MemorySystem]
        conversationCell <- memorySystem.createCell[Vector[Message]]("conversation")
        _ <- conversationCell.write(Vector.empty[Message])
        
        // Test message
        message = Message("TestUser", "Hello, World!")
        
        // Write message
        _ <- conversationCell.write(Vector(message))
        
        // Read back
        messages <- conversationCell.read
        
        // Verify
        result = messages match {
          case Some(msgs) => 
            assertTrue(msgs.length == 1) &&
            assertTrue(msgs.head.sender == "TestUser") &&
            assertTrue(msgs.head.content == "Hello, World!")
          case None => 
            assertTrue(false) && assertTrue(false) && assertTrue(false)
        }
      } yield result
    },
    
    test("should update user preferences") {
      for {
        memorySystem <- ZIO.service[MemorySystem]
        initialPrefs = UserPreferences("light", true, "en")
        preferencesCell <- memorySystem.createCell[UserPreferences]("preferences")
        _ <- preferencesCell.write(initialPrefs)
        
        // Update preferences
        updatedPrefs = initialPrefs.copy(theme = "dark")
        _ <- preferencesCell.write(updatedPrefs)
        
        // Read back
        currentPrefs <- preferencesCell.read
        
        // Verify
        result = currentPrefs match {
          case Some(prefs) =>
            assertTrue(prefs.theme == "dark") &&
            assertTrue(prefs.notifications) &&
            assertTrue(prefs.language == "en")
          case None =>
            assertTrue(false) && assertTrue(false) && assertTrue(false)
        }
      } yield result
    },
    
    test("should maintain conversation history") {
      for {
        memorySystem <- ZIO.service[MemorySystem]
        conversationCell <- memorySystem.createCell[Vector[Message]]("conversation-history")
        _ <- conversationCell.write(Vector.empty[Message])
        
        // Create messages
        messages = Vector(
          Message("User1", "First message"),
          Message("User2", "Second message"),
          Message("User1", "Third message")
        )
        
        // Write messages
        _ <- conversationCell.write(messages)
        
        // Read back
        history <- conversationCell.read
        
        // Verify
        result = history match {
          case Some(msgs) =>
            assertTrue(msgs.length == 3) &&
            assertTrue(msgs(0).sender == "User1") &&
            assertTrue(msgs(1).sender == "User2") &&
            assertTrue(msgs(2).sender == "User1")
          case None =>
            assertTrue(false) && assertTrue(false) && assertTrue(false)
        }
      } yield result
    }
  ).provide(ZLayer.succeed(new InMemorySystem()))
}
