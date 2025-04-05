package com.agenticai.core.llm.langchain.test

import com.agenticai.core.llm.langchain._
import dev.langchain4j.model.chat._
import dev.langchain4j.data.message.{ChatMessage, UserMessage, AiMessage}
import zio._
import zio.stream._

/**
 * A mock implementation of ZIOChatLanguageModel for testing purposes.
 * This model provides predefined responses or a default response.
 *
 * @param responses A map of user messages to predefined responses
 * @param defaultResponse The response to use when no matching user message is found
 */
final class MockChatLanguageModel(
  responses: Map[String, String] = Map.empty,
  defaultResponse: String = "I am a mock AI assistant."
) extends ZIOChatLanguageModel {
  
  /**
   * Generates a response synchronously based on matching the last user message
   * to entries in the responses map.
   *
   * @param messages The list of chat messages
   * @return A ZIO effect that resolves to a mock response
   */
  override def generate(messages: List[ChatMessage]): ZIO[Any, Throwable, AiMessage] = {
    // Extract the last user message
    val lastUserMessage = extractLastUserMessage(messages)
    
    // Look up the response or use default
    val responseText = responses.getOrElse(lastUserMessage, defaultResponse)
    
    // Create a mock response
    ZIO.succeed(AiMessage.from(responseText))
  }
  
  /**
   * Generates a stream of tokens based on matching the last user message
   * to entries in the responses map.
   *
   * @param messages The list of chat messages
   * @return A ZStream containing the mock response
   */
  override def generateStream(messages: List[ChatMessage]): ZStream[Any, Throwable, String] = {
    // Extract the last user message
    val lastUserMessage = extractLastUserMessage(messages)
    
    // Look up the response or use default
    val responseText = responses.getOrElse(lastUserMessage, defaultResponse)
    
    // Create a stream of the response
    // For simplicity, we just return the entire response as a single chunk
    ZStream.succeed(responseText)
  }
  
  // Helper methods
  
  /**
   * Extracts the last user message from a list of messages.
   *
   * @param messages The list of chat messages
   * @return The content of the last user message
   */
  /**
   * Extracts the last user message from a list of messages.
   * This method finds the most recent UserMessage in the conversation history
   * and extracts its text content.
   *
   * @param messages The list of chat messages
   * @return The content of the last user message, or empty string if none found
   */
  private def extractLastUserMessage(messages: List[ChatMessage]): String = {
    messages.reverse
      .find(_.isInstanceOf[UserMessage])
      .map {
        case userMsg: UserMessage => 
          // Extract just the text content from the UserMessage
          // The format is: UserMessage { name = null contents = [TextContent { text = "Hello" }] }
          // We need to extract just the "Hello" part
          val contentStr = userMsg.toString
          val textMatch = "text = \"(.+?)\"".r.findFirstMatchIn(contentStr)
          textMatch.map(_.group(1)).getOrElse("")
        case _ => ""
      }
      .getOrElse("")
  }
}

object MockChatLanguageModel {
  /**
   * Creates a mock chat language model.
   *
   * @param responses A map of user messages to predefined responses
   * @param defaultResponse The response to use when no matching user message is found
   * @return A ZIO effect that resolves to a mock ZIOChatLanguageModel
   */
  def make(
    responses: Map[String, String] = Map.empty,
    defaultResponse: String = "I am a mock AI assistant."
  ): UIO[ZIOChatLanguageModel] =
    ZIO.succeed(new MockChatLanguageModel(responses, defaultResponse))
    
  /**
   * Creates a ZLayer for a mock chat language model.
   *
   * @param responses A map of user messages to predefined responses
   * @param defaultResponse The response to use when no matching user message is found
   * @return A ZLayer that provides a mock ZIOChatLanguageModel
   */
  def layer(
    responses: Map[String, String] = Map.empty,
    defaultResponse: String = "I am a mock AI assistant."
  ): ZLayer[Any, Nothing, ZIOChatLanguageModel] =
    ZLayer.succeed(new MockChatLanguageModel(responses, defaultResponse))
}
