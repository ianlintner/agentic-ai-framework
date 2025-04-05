package com.agenticai.core.llm.langchain

import dev.langchain4j.model.chat._
import dev.langchain4j.data.message.{ChatMessage, AiMessage}
import zio._
import zio.stream._

/**
 * A ZIO wrapper for Langchain4j's ChatLanguageModel.
 * This trait provides ZIO-based methods for generating chat completions.
 */
trait ZIOChatLanguageModel {
  /**
   * Generates a chat completion synchronously.
   *
   * @param messages The list of chat messages
   * @return A ZIO effect that completes with the chat model response
   */
  def generate(messages: List[ChatMessage]): ZIO[Any, Throwable, AiMessage]
  
  /**
   * Generates a chat completion as a stream of tokens.
   *
   * @param messages The list of chat messages
   * @return A ZStream of tokens as they are generated
   */
  def generateStream(messages: List[ChatMessage]): ZStream[Any, Throwable, String]
}

/**
 * Live implementation of ZIOChatLanguageModel that delegates to a Langchain4j ChatLanguageModel.
 *
 * @param model The underlying Langchain4j chat model implementation
 */
final case class ZIOChatLanguageModelLive(model: ChatLanguageModel) extends ZIOChatLanguageModel {
  override def generate(messages: List[ChatMessage]): ZIO[Any, Throwable, AiMessage] =
    ZIO.attemptBlocking {
      // Convert the list to an array and use the varargs method
      val messagesArray = messages.toArray
      val response = model.chat(messagesArray: _*)
      // Create a new AiMessage from the response
      AiMessage.from(response.toString)
    }
  
  override def generateStream(messages: List[ChatMessage]): ZStream[Any, Throwable, String] = {
    // Implementation depends on streaming support in the chosen model
    // For now, we'll just return the full response as a single chunk
    ZStream.fromZIO(
      generate(messages).map(_.text())
    )
  }
}

object ZIOChatLanguageModel {
  /**
   * Creates a ZIOChatLanguageModel from a Langchain4j ChatLanguageModel.
   *
   * @param model The underlying Langchain4j chat model implementation
   * @return A ZIOChatLanguageModel wrapping the provided model
   */
  def apply(model: ChatLanguageModel): ZIOChatLanguageModel = 
    ZIOChatLanguageModelLive(model)
    
  /**
   * Creates a ZLayer for a ZIOChatLanguageModel.
   *
   * @param model The underlying Langchain4j chat model implementation
   * @return A ZLayer that provides a ZIOChatLanguageModel
   */
  def layer(model: ChatLanguageModel): ZLayer[Any, Nothing, ZIOChatLanguageModel] =
    ZLayer.succeed(ZIOChatLanguageModelLive(model))
}
