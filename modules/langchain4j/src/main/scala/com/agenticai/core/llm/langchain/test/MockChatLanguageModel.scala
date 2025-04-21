package com.agenticai.core.llm.langchain.test

import com.agenticai.core.llm.langchain.{LangchainError, ZIOChatLanguageModel}
import dev.langchain4j.data.message.{AiMessage, ChatMessage}
import zio.*
import zio.stream.*

/** A simple mock implementation of ZIOChatLanguageModel for testing. This implementation returns
  * predefined or default responses.
  */
class MockChatLanguageModel(
    responses: Map[String, String] = Map.empty,
    defaultResponse: String = "I am a mock AI assistant.",
    generateStreamFn: List[ChatMessage] => ZStream[Any, LangchainError, String] = null
) extends ZIOChatLanguageModel:

  /** Extracts the text from the last message to use as key for response lookup.
    */
  private def getLastMessageText(messages: List[ChatMessage]): String =
    if messages.isEmpty then ""
    else messages.last.toString

  /** Generates a response synchronously.
    */
  override def generate(messages: List[ChatMessage]): ZIO[Any, LangchainError, AiMessage] =
    ZIO.succeed {
      val lastMsg      = getLastMessageText(messages)
      val responseText = responses.getOrElse(lastMsg, defaultResponse)
      AiMessage.from(responseText)
    }

  /** Generates a response as a stream of tokens.
    */
  override def generateStream(messages: List[ChatMessage]): ZStream[Any, LangchainError, String] =
    if (generateStreamFn != null) then
      // Use the provided stream function if available
      generateStreamFn(messages)
    else
      // Default implementation: split the response text into words to simulate streaming
      val lastMsg = getLastMessageText(messages)
      val responseText = responses.getOrElse(lastMsg, defaultResponse)
      val chunks = responseText.split(" ").toList.map(_ + " ")
      ZStream.fromIterable(chunks)

object MockChatLanguageModel:

  /** Creates a new mock chat language model.
    */
  def make(
      responses: Map[String, String] = Map.empty,
      defaultResponse: String = "I am a mock AI assistant.",
      generateStreamFn: List[ChatMessage] => ZStream[Any, LangchainError, String] = null
  ): UIO[ZIOChatLanguageModel] =
    ZIO.succeed(new MockChatLanguageModel(responses, defaultResponse, generateStreamFn))

  /** Creates a layer that provides a mock chat language model.
    */
  def layer(
      responses: Map[String, String] = Map.empty,
      defaultResponse: String = "I am a mock AI assistant.",
      generateStreamFn: List[ChatMessage] => ZStream[Any, LangchainError, String] = null
  ): ZLayer[Any, Nothing, ZIOChatLanguageModel] =
    ZLayer.succeed(new MockChatLanguageModel(responses, defaultResponse, generateStreamFn))
