package com.agenticai.core.llm.langchain.rag.document

import com.agenticai.core.llm.langchain.rag.RAGError
import dev.langchain4j.data.document.Document
import dev.langchain4j.data.segment.TextSegment
import zio.*

/**
 * Interface for chunking documents into smaller segments.
 */
trait DocumentChunker:
  /**
   * Chunk a document into segments.
   *
   * @param document The document to chunk
   * @return A ZIO effect that completes with a list of text segments
   */
  def chunkDocument(document: Document): ZIO[Any, RAGError.ChunkingError, List[TextSegment]]
  
  /**
   * Chunk multiple documents into segments.
   *
   * @param documents The documents to chunk
   * @return A ZIO effect that completes with a list of text segments
   */
  def chunkDocuments(documents: List[Document]): ZIO[Any, RAGError.ChunkingError, List[TextSegment]] =
    ZIO.foreach(documents)(chunkDocument).map(_.flatten)