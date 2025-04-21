package com.agenticai.core.llm.langchain.rag

import com.agenticai.core.llm.langchain.LangchainError
import com.agenticai.core.llm.langchain.embedding.ZIOEmbeddingModel
import com.agenticai.core.llm.langchain.rag.context.ContextBuilder
import com.agenticai.core.llm.langchain.rag.document.DocumentChunker
import com.agenticai.core.llm.langchain.rag.generation.{ResponseGenerator, StreamingResponseGenerator}
import com.agenticai.core.llm.langchain.rag.retrieval.Retriever
import com.agenticai.core.llm.langchain.vectorstore.ZIOVectorStore
import dev.langchain4j.data.document.Document
import dev.langchain4j.data.segment.TextSegment
import zio.*
import zio.stream.*

/**
 * Main class for the Retrieval-Augmented Generation (RAG) system.
 *
 * @param documentChunker The document chunker for processing documents
 * @param vectorStore The vector store for storing embeddings
 * @param embeddingModel The embedding model for generating embeddings
 * @param retriever The retriever for finding relevant segments
 * @param contextBuilder The context builder for creating context
 * @param responseGenerator The response generator for generating responses
 */
class RAGSystem private (
  documentChunker: DocumentChunker,
  vectorStore: ZIOVectorStore,
  embeddingModel: ZIOEmbeddingModel,
  retriever: Retriever,
  contextBuilder: ContextBuilder,
  responseGenerator: ResponseGenerator,
  streamingResponseGenerator: Option[StreamingResponseGenerator] = None
):

  /**
   * Add a document to the RAG system.
   *
   * @param document The document to add
   * @return A ZIO effect that completes with the IDs of the added segments
   */
  def addDocument(document: Document): ZIO[Any, RAGError, List[String]] =
    for
      // 1. Chunk the document into segments
      segments <- documentChunker.chunkDocument(document)
        .tapError(e => ZIO.logError(s"Error chunking document: ${e.message}"))
      
      // 2. Add the segments to the vector store (which handles embedding internally)
      ids <- vectorStore.addTextSegments(segments, embeddingModel)
        .mapError(e => RAGError.ProcessingError(s"Failed to add segments to vector store: ${e.getMessage}", Some(e)))
    yield
      ids
  
  /**
   * Alias for addDocument to match the test expectation.
   *
   * @param document The document to index
   * @return A ZIO effect that completes with the IDs of the added segments
   */
  def indexDocument(document: Document): ZIO[Any, RAGError, List[String]] =
    addDocument(document)
  
  /**
   * Add multiple documents to the RAG system.
   *
   * @param documents The documents to add
   * @return A ZIO effect that completes with the IDs of the added segments
   */
  def addDocuments(documents: List[Document]): ZIO[Any, RAGError, List[String]] =
    ZIO.foreach(documents)(addDocument).map(_.flatten)
  
  /**
   * Generate a response to a query using the RAG approach.
   *
   * @param query The query to answer
   * @param maxResults The maximum number of segments to retrieve
   * @return A ZIO effect that completes with the generated response
   */
  def query(query: String, maxResults: Int = 5): ZIO[Any, RAGError, String] =
    for
      // 1. Retrieve relevant segments
      segments <- retriever.retrieve(query, maxResults)
        .tapError(e => ZIO.logError(s"Error retrieving segments: ${e.message}"))
      
      // 2. Build context for the query
      context <- contextBuilder.buildContext(segments, query)
        .tapError(e => ZIO.logError(s"Error building context: ${e.message}"))
      
      // 3. Generate a response using the context
      response <- responseGenerator.generateResponse(context, query)
        .tapError(e => ZIO.logError(s"Error generating response: ${e.message}"))
    yield
      response
  
  /**
   * Generate a streaming response to a query using the RAG approach.
   *
   * @param query The query to answer
   * @param maxResults The maximum number of segments to retrieve
   * @return A ZStream that emits chunks of the generated response
   */
  def queryStream(query: String, maxResults: Int = 5): ZStream[Any, RAGError, String] =
    streamingResponseGenerator match
      case Some(generator) =>
        for
          // 1. Retrieve relevant segments
          segments <- ZStream.fromZIO(
            retriever.retrieve(query, maxResults)
              .tapError(e => ZIO.logError(s"Error retrieving segments: ${e.message}"))
          )
          
          // 2. Build context for the query
          context <- ZStream.fromZIO(
            contextBuilder.buildContext(segments, query)
              .tapError(e => ZIO.logError(s"Error building context: ${e.message}"))
          )
          
          // 3. Generate a streaming response using the context
          chunk <- generator.generateStreamingResponse(context, query)
            .tapError(e => ZIO.logError(s"Error generating streaming response: ${e.message}"))
        yield
          chunk
      case None =>
        ZStream.fromZIO(
          ZIO.fail(RAGError.ConfigurationError("Streaming response generator not configured", None))
        )

object RAGSystem:
  /**
   * Create a new RAGSystemBuilder.
   *
   * @return A new RAGSystemBuilder
   */
  def builder: RAGSystemBuilder = RAGSystemBuilder()
  
  /**
   * Create a RAGSystem with the provided components.
   *
   * @param documentChunker The document chunker
   * @param vectorStore The vector store
   * @param embeddingModel The embedding model
   * @param retriever The retriever
   * @param contextBuilder The context builder
   * @param responseGenerator The response generator
   * @return A new RAGSystem
   */
  def apply(
    documentChunker: DocumentChunker,
    vectorStore: ZIOVectorStore,
    embeddingModel: ZIOEmbeddingModel,
    retriever: Retriever,
    contextBuilder: ContextBuilder,
    responseGenerator: ResponseGenerator,
    streamingResponseGenerator: Option[StreamingResponseGenerator] = None
  ): RAGSystem =
    new RAGSystem(
      documentChunker,
      vectorStore,
      embeddingModel,
      retriever,
      contextBuilder,
      responseGenerator,
      streamingResponseGenerator
    )