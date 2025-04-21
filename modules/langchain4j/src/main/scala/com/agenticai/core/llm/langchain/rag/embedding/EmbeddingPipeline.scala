package com.agenticai.core.llm.langchain.rag.embedding

import com.agenticai.core.llm.langchain.embedding.ZIOEmbeddingModel
import com.agenticai.core.llm.langchain.rag.RAGError
import com.agenticai.core.llm.langchain.rag.document.DocumentChunker
import com.agenticai.core.llm.langchain.vectorstore.ZIOVectorStore
import dev.langchain4j.data.document.Document
import dev.langchain4j.data.segment.TextSegment
import zio.*

import java.util.UUID

/**
 * Handles the conversion of text segments to embeddings and storage in the vector store.
 */
trait EmbeddingPipeline:
  /**
   * Embeds and stores a list of text segments.
   *
   * @param segments The text segments to embed and store
   * @return A ZIO effect that completes with the IDs of the stored segments
   */
  def embedAndStore(segments: List[TextSegment]): ZIO[Any, RAGError.EmbeddingError, List[String]]
  
  /**
   * Embeds and stores a document.
   *
   * @param document The document to embed and store
   * @return A ZIO effect that completes with the IDs of the stored segments
   */
  def embedAndStoreDocument(document: Document): ZIO[Any, RAGError.EmbeddingError, List[String]]

/**
 * Standard implementation of the EmbeddingPipeline.
 *
 * @param vectorStore The vector store to use for storage
 * @param embeddingModel The embedding model to use for generating embeddings
 * @param documentChunker The document chunker to use for chunking documents
 */
case class StandardEmbeddingPipeline(
  vectorStore: ZIOVectorStore,
  embeddingModel: ZIOEmbeddingModel,
  documentChunker: DocumentChunker
) extends EmbeddingPipeline:
  
  override def embedAndStore(segments: List[TextSegment]): ZIO[Any, RAGError.EmbeddingError, List[String]] = {
    // Use the vectorStore's addTextSegments method which handles embedding generation internally
    vectorStore.addTextSegments(segments, embeddingModel)
      .mapError(e => RAGError.EmbeddingError(s"Failed to store embeddings: ${e.getMessage}", Some(e)))
  }
  
  override def embedAndStoreDocument(document: Document): ZIO[Any, RAGError.EmbeddingError, List[String]] = {
    for {
      // Call chunkDocument directly from the DocumentChunker instance
      chunks <- documentChunker.chunkDocument(document)
        .mapError(e => RAGError.EmbeddingError(s"Failed to chunk document: ${e.getMessage}", Some(e)))
      ids <- embedAndStore(chunks)
    } yield ids
  }