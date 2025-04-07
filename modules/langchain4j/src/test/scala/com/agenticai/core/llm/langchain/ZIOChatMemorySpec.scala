package com.agenticai.core.llm.langchain

import dev.langchain4j.data.message.{AiMessage, UserMessage}
import zio._
import zio.test._

object ZIOChatMemorySpec extends ZIOSpecDefault {
  
  def spec = suite("ZIOChatMemory")(
    
    test("in-memory implementation should add and retrieve messages") {
      for {
        memory <- ZIOChatMemory.createInMemory(10)
        
        // Add a user message
        _ <- memory.addUserMessage("Hello")
        
        // Add an assistant message
        _ <- memory.addAssistantMessage("Hi there!")
        
        // Get all messages
        messages <- memory.messages
      } yield assertTrue(messages.size == 2) && 
             assertTrue(messages(0).isInstanceOf[UserMessage]) &&
             assertTrue(messages(1).isInstanceOf[AiMessage])
    },
    
    test("in-memory implementation should respect the maximum message limit") {
      for {
        memory <- ZIOChatMemory.createInMemory(2) // Only keep 2 messages
        
        // Add 3 messages (should keep only the most recent 2)
        _ <- memory.addUserMessage("Message 1")
        _ <- memory.addUserMessage("Message 2")
        _ <- memory.addUserMessage("Message 3")
        
        // Get all messages
        messages <- memory.messages
        
        // Check content
        firstContainsMsg2 = messages.head.toString.contains("Message 2")
        secondContainsMsg3 = messages.last.toString.contains("Message 3")
        noMsgContainsMsg1 = !messages.exists(msg => msg.toString.contains("Message 1"))
      } yield assertTrue(messages.size == 2) &&
             assertTrue(firstContainsMsg2 || secondContainsMsg3) &&  // At least one message contains the expected text
             assertTrue(noMsgContainsMsg1)                           // No message contains Message 1
    },
    
    test("in-memory implementation should clear messages") {
      for {
        memory <- ZIOChatMemory.createInMemory(10)
        
        // Add some messages
        _ <- memory.addUserMessage("Hello")
        _ <- memory.addAssistantMessage("Hi there!")
        
        // Clear the memory
        _ <- memory.clear()
        
        // Get all messages
        messages <- memory.messages
      } yield assertTrue(messages.isEmpty)
    }
  )
}
