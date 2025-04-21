package com.agenticai.core.llm.langchain.embedding

import com.agenticai.core.llm.langchain.LangchainError
import dev.langchain4j.data.embedding.Embedding
import zio.*

/**
 * A mock implementation of ZIOEmbeddingModel for testing purposes.
 */
class MockZIOEmbeddingModel extends ZIOEmbeddingModel:
  
  /**
   * Embeds a text into a fixed mock embedding.
   *
   * @param text The text to embed
   * @return A ZIO effect that completes with a fixed mock Embedding
   */
  override def embed(text: String): ZIO[Any, LangchainError, Embedding] =
    // Create a 3-dimensional embedding as expected by the tests
    ZIO.succeed(Embedding.from(Array(text.length.toFloat, text.length.toFloat / 2, text.length.toFloat / 3)))
    
  /**
   * Embeds multiple texts into fixed mock embeddings.
   *
   * @param texts The texts to embed
   * @return A ZIO effect that completes with a list of fixed mock Embeddings
   */
  override def embedAll(texts: List[String]): ZIO[Any, LangchainError, List[Embedding]] =
    ZIO.succeed(texts.map(text =>
      // Create a 3-dimensional embedding as expected by the tests
      Embedding.from(Array(text.length.toFloat, text.length.toFloat / 2, text.length.toFloat / 3))
    ))

/**
 * Companion object for MockZIOEmbeddingModel.
 */
object MockZIOEmbeddingModel:
  /**
   * Creates a new MockZIOEmbeddingModel.
   *
   * @return A new MockZIOEmbeddingModel
   */
  def apply(): MockZIOEmbeddingModel = new MockZIOEmbeddingModel()