package com.agenticai.core.llm.langchain.rag.retrieval

import com.agenticai.core.llm.langchain.embedding.ZIOEmbeddingModel
import com.agenticai.core.llm.langchain.rag.RAGError
import com.agenticai.core.llm.langchain.vectorstore.ZIOVectorStore
import dev.langchain4j.data.segment.TextSegment
import zio.*

/**
 * Default implementation of Retriever that uses a vector store to retrieve segments.
 *
 * @param vectorStore The vector store to retrieve from
 * @param embeddingModel The embedding model to use for query embedding
 * @param minScore The minimum similarity score (0-1) for results
 */
case class DefaultRetriever(
  vectorStore: ZIOVectorStore,
  embeddingModel: ZIOEmbeddingModel,
  minScore: Double = 0.7
) extends Retriever:

  /**
   * Retrieve relevant text segments based on a query.
   *
   * @param query The query to use for retrieval
   * @param maxResults The maximum number of results to return
   * @return A ZIO effect that completes with a list of relevant text segments
   */
  override def retrieve(query: String, maxResults: Int): ZIO[Any, RAGError.RetrievalError, List[TextSegment]] =
    vectorStore.findSimilar(query, embeddingModel, maxResults, minScore)
      .map(matches => matches.map(_.embedded()))
      .mapError(e => RAGError.RetrievalError(s"Failed to retrieve segments: ${e.getMessage}", Some(e)))