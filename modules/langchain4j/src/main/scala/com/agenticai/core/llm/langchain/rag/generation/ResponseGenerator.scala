package com.agenticai.core.llm.langchain.rag.generation

import com.agenticai.core.llm.langchain.ZIOChatLanguageModel
import com.agenticai.core.llm.langchain.rag.RAGError
import dev.langchain4j.data.message.{AiMessage, ChatMessage, UserMessage}
import zio.*

/**
 * Interface for generating responses based on context and a query.
 */
trait ResponseGenerator:
  /**
   * Generate a response based on the provided context and query.
   *
   * @param context The context to use for generation (usually contains retrieved information)
   * @param query The original user query
   * @return A ZIO effect that completes with the generated response
   */
  def generateResponse(context: String, query: String): ZIO[Any, RAGError.GenerationError, String]

/**
 * Default implementation of ResponseGenerator that uses a ZIOChatLanguageModel.
 *
 * @param model The chat language model to use for generation
 */
case class DefaultResponseGenerator(model: ZIOChatLanguageModel) extends ResponseGenerator:

  /**
   * Generate a response based on the provided context and query.
   * The context is sent as a user message to the model.
   *
   * @param context The context to use for generation (usually contains retrieved information)
   * @param query The original user query
   * @return A ZIO effect that completes with the generated response
   */
  override def generateResponse(context: String, query: String): ZIO[Any, RAGError.GenerationError, String] =
    // Create a formatted prompt that combines context and query
    val prompt = s"""
      |Context information:
      |$context
      |
      |Based on the above context, please answer the following question:
      |$query
      |""".stripMargin
    
    // The UserMessage is a subtype of ChatMessage
    val message: ChatMessage = UserMessage.from(prompt)
    
    model.generate(List(message))
      .map(ai => ai.text())
      .mapError(e => RAGError.GenerationError(s"Failed to generate response: ${e.getMessage}", Some(e)))