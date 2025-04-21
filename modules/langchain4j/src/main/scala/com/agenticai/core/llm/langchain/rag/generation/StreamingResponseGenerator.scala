package com.agenticai.core.llm.langchain.rag.generation

import com.agenticai.core.llm.langchain.ZIOChatLanguageModel
import com.agenticai.core.llm.langchain.rag.RAGError
import dev.langchain4j.data.message.{ChatMessage, UserMessage}
import zio.*
import zio.stream.*

/**
 * Defines the interface for generating streaming responses in the RAG system.
 * This component is responsible for creating a response as a stream of text chunks
 * based on the provided context and query.
 */
trait StreamingResponseGenerator:
  /**
   * Generates a streaming response based on the provided context and query.
   *
   * @param context The context text from retrieved documents
   * @param query The user's original query
   * @return A stream of text chunks that form the complete response
   */
  def generateStreamingResponse(context: String, query: String): ZStream[Any, RAGError.GenerationError, String]

/**
 * Default implementation of StreamingResponseGenerator.
 * Uses a ZIOChatLanguageModel that supports streaming to generate responses.
 *
 * @param model The language model to use for generating streaming responses
 */
final case class DefaultStreamingResponseGenerator(model: ZIOChatLanguageModel) extends StreamingResponseGenerator:

  /**
   * Creates a prompt that combines the context and query for the language model.
   *
   * @param context The retrieved context
   * @param query The user's query
   * @return A formatted prompt for the language model
   */
  private def createPrompt(context: String, query: String): String =
    s"""You are a helpful assistant that provides accurate information based on the given context.
       |
       |Context:
       |$context
       |
       |Answer the following question based ONLY on the information provided in the context.
       |If the context doesn't contain relevant information, state that you don't have enough information.
       |
       |Question: $query
       |
       |Answer:""".stripMargin

  /**
   * Generates a streaming response based on the provided context and query.
   *
   * @param context The context text from retrieved documents
   * @param query The user's original query
   * @return A stream of text chunks that form the complete response
   */
  override def generateStreamingResponse(context: String, query: String): ZStream[Any, RAGError.GenerationError, String] =
    // Create the prompt
    val prompt = createPrompt(context, query)
    
    // Create a message for the model
    val userMessage = UserMessage.from(prompt)
    
    // Call the model's streaming interface
    model.generateStream(List(userMessage))
      .mapError(error => RAGError.GenerationError(s"Error generating streaming response: $error"))

/**
 * Companion object for StreamingResponseGenerator
 */
object StreamingResponseGenerator:
  /**
   * Creates a layer for a DefaultStreamingResponseGenerator.
   *
   * @param model The language model to use for generating streaming responses
   * @return A ZLayer that provides a StreamingResponseGenerator
   */
  def layer(model: ZIOChatLanguageModel): ZLayer[Any, Nothing, StreamingResponseGenerator] =
    ZLayer.succeed(DefaultStreamingResponseGenerator(model))