package com.agenticai.core.llm.langchain.rag

import com.agenticai.core.llm.langchain.ZIOChatLanguageModel
import com.agenticai.core.llm.langchain.embedding.ZIOEmbeddingModel
import com.agenticai.core.llm.langchain.rag.context.{ContextBuilder, DefaultContextBuilder}
import com.agenticai.core.llm.langchain.rag.document.{DefaultDocumentChunker, DocumentChunker}
import com.agenticai.core.llm.langchain.rag.generation.{DefaultResponseGenerator, DefaultStreamingResponseGenerator, ResponseGenerator, StreamingResponseGenerator}
import com.agenticai.core.llm.langchain.rag.retrieval.{DefaultRetriever, Retriever}
import com.agenticai.core.llm.langchain.vectorstore.ZIOVectorStore
import zio.*

/**
 * Builder for constructing a RAGSystem with a fluent API.
 */
case class RAGSystemBuilder(
  documentChunker: Option[DocumentChunker] = None,
  vectorStore: Option[ZIOVectorStore] = None,
  embeddingModel: Option[ZIOEmbeddingModel] = None,
  retriever: Option[Retriever] = None,
  contextBuilder: Option[ContextBuilder] = None,
  responseGenerator: Option[ResponseGenerator] = None,
  streamingResponseGenerator: Option[StreamingResponseGenerator] = None
):

  /**
   * Set the document chunker.
   *
   * @param chunker The document chunker to use
   * @return An updated builder with the document chunker set
   */
  def withDocumentChunker(chunker: DocumentChunker): RAGSystemBuilder =
    copy(documentChunker = Some(chunker))
  
  /**
   * Set a default document chunker with the specified parameters.
   *
   * @param maxChunkSize The maximum size of each chunk in characters
   * @param chunkOverlap The number of characters to overlap between chunks
   * @return An updated builder with a default document chunker set
   */
  def withDefaultDocumentChunker(maxChunkSize: Int = 1000, chunkOverlap: Int = 200): RAGSystemBuilder =
    withDocumentChunker(DefaultDocumentChunker(maxChunkSize, chunkOverlap))
  
  /**
   * Set the vector store.
   *
   * @param store The vector store to use
   * @return An updated builder with the vector store set
   */
  def withVectorStore(store: ZIOVectorStore): RAGSystemBuilder =
    copy(vectorStore = Some(store))
  
  /**
   * Set the embedding model.
   *
   * @param model The embedding model to use
   * @return An updated builder with the embedding model set
   */
  def withEmbeddingModel(model: ZIOEmbeddingModel): RAGSystemBuilder =
    copy(embeddingModel = Some(model))
  
  /**
   * Set the retriever.
   *
   * @param ret The retriever to use
   * @return An updated builder with the retriever set
   */
  def withRetriever(ret: Retriever): RAGSystemBuilder =
    copy(retriever = Some(ret))
  
  /**
   * Set a default retriever using the specified vector store and embedding model.
   * Note: This requires the vector store and embedding model to be set first.
   *
   * @param minScore The minimum similarity score (0-1) for retrieval
   * @return An updated builder with a default retriever set
   */
  def withDefaultRetriever(minScore: Double = 0.7): RAGSystemBuilder =
    (vectorStore, embeddingModel) match
      case (Some(store), Some(model)) =>
        withRetriever(DefaultRetriever(store, model, minScore))
      case _ =>
        throw new RAGError.ConfigurationError("Vector store and embedding model must be set before creating a default retriever", None)
  
  /**
   * Set the context builder.
   *
   * @param builder The context builder to use
   * @return An updated builder with the context builder set
   */
  def withContextBuilder(builder: ContextBuilder): RAGSystemBuilder =
    copy(contextBuilder = Some(builder))
  
  /**
   * Set a default context builder with the specified parameters.
   *
   * @param maxContextLength The maximum length of the context in characters
   * @param template The template to use for formatting the context
   * @return An updated builder with a default context builder set
   */
  def withDefaultContextBuilder(
    maxContextLength: Int = 4000,
    template: String = "Answer the question based on the following context:\n\nContext:\n{context}\n\nQuestion: {query}"
  ): RAGSystemBuilder =
    withContextBuilder(DefaultContextBuilder(maxContextLength, template))
  
  /**
   * Set the response generator.
   *
   * @param generator The response generator to use
   * @return An updated builder with the response generator set
   */
  def withResponseGenerator(generator: ResponseGenerator): RAGSystemBuilder =
    copy(responseGenerator = Some(generator))
    
  /**
   * Set the streaming response generator.
   *
   * @param generator The streaming response generator to use
   * @return An updated builder with the streaming response generator set
   */
  def withStreamingResponseGenerator(generator: StreamingResponseGenerator): RAGSystemBuilder =
    copy(streamingResponseGenerator = Some(generator))
  
  /**
   * Set a default response generator using the specified LLM.
   *
   * @param model The LLM to use for generation
   * @return An updated builder with a default response generator set
   */
  def withDefaultResponseGenerator(model: ZIOChatLanguageModel): RAGSystemBuilder =
    withResponseGenerator(DefaultResponseGenerator(model))
    
  /**
   * Set a default streaming response generator using the specified LLM.
   *
   * @param model The LLM to use for generation
   * @return An updated builder with a default streaming response generator set
   */
  def withDefaultStreamingResponseGenerator(model: ZIOChatLanguageModel): RAGSystemBuilder =
    withStreamingResponseGenerator(DefaultStreamingResponseGenerator(model))
  
  /**
   * Build a RAGSystem with the configured components.
   *
   * @return A ZIO effect that completes with a RAGSystem
   */
  def build: ZIO[Any, RAGError.ConfigurationError, RAGSystem] =
    for
      chunker <- ZIO.fromOption(documentChunker)
        .orElseFail(RAGError.ConfigurationError("Document chunker not set", None))
      
      store <- ZIO.fromOption(vectorStore)
        .orElseFail(RAGError.ConfigurationError("Vector store not set", None))
      
      model <- ZIO.fromOption(embeddingModel)
        .orElseFail(RAGError.ConfigurationError("Embedding model not set", None))
      
      ret <- ZIO.fromOption(retriever)
        .orElseFail(RAGError.ConfigurationError("Retriever not set", None))
      
      ctxBuilder <- ZIO.fromOption(contextBuilder)
        .orElseFail(RAGError.ConfigurationError("Context builder not set", None))
      
      respGen <- ZIO.fromOption(responseGenerator)
        .orElseFail(RAGError.ConfigurationError("Response generator not set", None))
    yield
      RAGSystem(chunker, store, model, ret, ctxBuilder, respGen, streamingResponseGenerator)
  
  /**
   * Build a RAGSystem with default values for any components that have not been explicitly configured.
   * This requires at least a vector store, embedding model, and language model to be set.
   *
   * @param llm The language model to use for response generation
   * @return A ZIO effect that completes with a RAGSystem
   */
  def buildWithDefaults(llm: ZIOChatLanguageModel): ZIO[Any, RAGError.ConfigurationError, RAGSystem] =
    for
      store <- ZIO.fromOption(vectorStore)
        .orElseFail(RAGError.ConfigurationError("Vector store not set", None))
      
      model <- ZIO.fromOption(embeddingModel)
        .orElseFail(RAGError.ConfigurationError("Embedding model not set", None))
      
      builder = this
        .withDefaultDocumentChunker()
        .withDefaultRetriever()
        .withDefaultContextBuilder()
        .withDefaultResponseGenerator(llm)
        .withDefaultStreamingResponseGenerator(llm)
      
      system <- builder.build
    yield
      system

/**
* Companion object for RAGSystemBuilder to allow for easy instantiation.
*/
object RAGSystemBuilder:
 /**
  * Create a new RAGSystemBuilder with default empty values.
  *
  * @return A new RAGSystemBuilder instance
  */
 def apply(): RAGSystemBuilder = new RAGSystemBuilder()
 
 /**
  * Create a new RAGSystem with the provided vector store, embedding model, and chat model.
  * This is a convenience method that sets up a RAG system with sensible defaults.
  *
  * @param vectorStore The vector store to use for storing and retrieving embeddings
  * @param embeddingModel The embedding model to use for generating embeddings
  * @param chatModel The chat language model to use for generating responses
  * @return A ZIO effect that completes with a new RAGSystem
  */
 def create(
   vectorStore: ZIOVectorStore,
   embeddingModel: ZIOEmbeddingModel,
   chatModel: ZIOChatLanguageModel
 ): ZIO[Any, RAGError.ConfigurationError, RAGSystem] =
   apply()
     .withVectorStore(vectorStore)
     .withEmbeddingModel(embeddingModel)
     .withDefaultDocumentChunker()
     .withDefaultRetriever()
     .withDefaultContextBuilder()
     .withDefaultResponseGenerator(chatModel)
     .withDefaultStreamingResponseGenerator(chatModel)
     .build