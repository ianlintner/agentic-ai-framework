package com.agenticai.core.llm.langchain

import com.agenticai.core.llm.langchain.test.MockChatLanguageModel
import dev.langchain4j.data.message.{ChatMessage, UserMessage}
import zio.*
import zio.test.*
import zio.test.Assertion.*
import zio.test.TestAspect.*

object ZIOChatLanguageModelSpec extends ZIOSpecDefault {

  def spec = suite("ZIOChatLanguageModel")(
    test("generate should return an AiMessage") {
      // Given a mock ZIOChatLanguageModel with a predefined response
      val responseText = "Mock response to: Hello"
      
      for {
        // Create the mock model directly
        model <- ZIO.succeed(new MockChatLanguageModel(
          defaultResponse = responseText
        ))
        
        // When generating a response
        messages = List(UserMessage.from("Hello"))
        response <- model.generate(messages)
      } yield
        // Then it should return an AiMessage with the expected text
        assertTrue(response.text().contains(responseText))
    },
    
    test("generateStream should return a stream of tokens") {
      // Given a mock ZIOChatLanguageModel with a predefined response
      val responseText = "Mock response to: Hello"
      
      for {
        // Create the mock model directly
        model <- ZIO.succeed(new MockChatLanguageModel(
          defaultResponse = responseText
        ))
        
        // When generating a streaming response
        messages = List(UserMessage.from("Hello"))
        tokens <- model.generateStream(messages).runCollect
        text = tokens.mkString
      } yield
        // Then it should return a stream of tokens that combine to the expected text
        assertTrue(text.contains(responseText))
    }
  ) @@ timeout(10.seconds)
}