package com.agenticai.core.llm.langchain.test

import com.agenticai.core.llm.langchain.ZIOChatLanguageModel
import dev.langchain4j.data.message.{AiMessage, ChatMessage}
import zio._
import zio.stream._

/**
 * A simple mock implementation of ZIOChatLanguageModel for testing.
 * This implementation returns predefined or default responses.
 */
class MockChatLanguageModel(
  responses: Map[String, String] = Map.empty,
  defaultResponse: String = "I am a mock AI assistant."
) extends ZIOChatLanguageModel {
  
  /**
   * Extracts the text from the last message to use as key for response lookup.
   */
  private def getLastMessageText(messages: List[ChatMessage]): String = {
    if (messages.isEmpty) ""
    else messages.last.toString
  }
  
  /**
   * Generates a response synchronously.
   */
  override def generate(messages: List[ChatMessage]): ZIO[Any, Throwable, AiMessage] = {
    ZIO.succeed {
      val lastMsg = getLastMessageText(messages)
      val responseText = responses.getOrElse(lastMsg, defaultResponse)
      AiMessage.from(responseText)
    }
  }
  
  /**
   * Generates a response as a stream of tokens.
   */
  override def generateStream(messages: List[ChatMessage]): ZStream[Any, Throwable, String] = {
    ZStream.succeed {
      val lastMsg = getLastMessageText(messages)
      responses.getOrElse(lastMsg, defaultResponse)
    }
  }
}

object MockChatLanguageModel {
  /**
   * Creates a new mock chat language model.
   */
  def make(
    responses: Map[String, String] = Map.empty,
    defaultResponse: String = "I am a mock AI assistant."
  ): UIO[ZIOChatLanguageModel] =
    ZIO.succeed(new MockChatLanguageModel(responses, defaultResponse))
    
  /**
   * Creates a layer that provides a mock chat language model.
   */
  def layer(
    responses: Map[String, String] = Map.empty,
    defaultResponse: String = "I am a mock AI assistant."
  ): ZLayer[Any, Nothing, ZIOChatLanguageModel] =
    ZLayer.succeed(new MockChatLanguageModel(responses, defaultResponse))
}
