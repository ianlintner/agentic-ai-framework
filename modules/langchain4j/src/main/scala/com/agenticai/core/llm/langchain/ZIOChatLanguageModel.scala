package com.agenticai.core.llm.langchain

import dev.langchain4j.model.chat.*
import dev.langchain4j.data.message.{AiMessage, ChatMessage}
import zio.*
import zio.stream.*

import scala.jdk.CollectionConverters.*

/** A ZIO wrapper for Langchain4j's ChatLanguageModel. This trait provides ZIO-based methods for
  * generating chat completions.
  */
trait ZIOChatLanguageModel:
  /** Generates a chat completion synchronously.
    *
    * @param messages
    *   The list of chat messages
    * @return
    *   A ZIO effect that completes with the chat model response
    */
  def generate(messages: List[ChatMessage]): ZIO[Any, LangchainError, AiMessage]

  /** Generates a chat completion as a stream of tokens.
    *
    * @param messages
    *   The list of chat messages
    * @return
    *   A ZStream of tokens as they are generated
    */
  def generateStream(messages: List[ChatMessage]): ZStream[Any, LangchainError, String]

/** Live implementation of ZIOChatLanguageModel that delegates to a Langchain4j ChatLanguageModel.
  *
  * @param model
  *   The underlying Langchain4j chat model implementation
  */
final case class ZIOChatLanguageModelLive(model: ChatLanguageModel) extends ZIOChatLanguageModel:

  override def generate(messages: List[ChatMessage]): ZIO[Any, LangchainError, AiMessage] =
    ZIO.attempt {
      // Convert the list to an array and use the varargs method
      val messagesArray = messages.toArray
      val response = model.chat(messagesArray*)
      
      // Extract the AiMessage from the response
      response.aiMessage()
    }.mapError(LangchainError.fromLangchain4jException)

  override def generateStream(messages: List[ChatMessage]): ZStream[Any, LangchainError, String] =
    // Simulate streaming by splitting the response
    ZStream.fromZIO(
      generate(messages).map(_.text())
    ).flatMap { text =>
      // Split the text into words to simulate streaming
      ZStream.fromIterable(text.split(" ").map(_ + " "))
    }

object ZIOChatLanguageModel:

  /** Creates a ZIOChatLanguageModel from a Langchain4j ChatLanguageModel.
    *
    * @param model
    *   The underlying Langchain4j chat model implementation
    * @return
    *   A ZIOChatLanguageModel wrapping the provided model
    */
  def apply(model: ChatLanguageModel): ZIOChatLanguageModel =
    ZIOChatLanguageModelLive(model)

  /** Creates a ZLayer for a ZIOChatLanguageModel.
    *
    * @param model
    *   The underlying Langchain4j chat model implementation
    * @return
    *   A ZLayer that provides a ZIOChatLanguageModel
    */
  def layer(model: ChatLanguageModel): ZLayer[Any, Nothing, ZIOChatLanguageModel] =
    ZLayer.succeed(apply(model))
