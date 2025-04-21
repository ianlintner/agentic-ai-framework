package com.agenticai.core.llm.langchain.rag.retrieval

import com.agenticai.core.llm.langchain.rag.RAGError
import dev.langchain4j.data.segment.TextSegment
import zio.*

/**
 * Interface for retrieving relevant text segments based on a query.
 */
trait Retriever:
  /**
   * Retrieve relevant text segments based on a query.
   *
   * @param query The query to use for retrieval
   * @param maxResults The maximum number of results to return
   * @return A ZIO effect that completes with a list of relevant text segments
   */
  def retrieve(query: String, maxResults: Int): ZIO[Any, RAGError.RetrievalError, List[TextSegment]]