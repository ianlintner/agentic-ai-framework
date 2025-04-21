package com.agenticai.core.llm.langchain.embedding

import com.agenticai.core.llm.langchain.LangchainError
import dev.langchain4j.data.embedding.Embedding
import dev.langchain4j.data.segment.TextSegment
import dev.langchain4j.model.embedding.EmbeddingModel
import zio.*

import scala.jdk.CollectionConverters.*

/**
 * A ZIO wrapper for Langchain4j's EmbeddingModel. This trait provides ZIO-based methods for
 * generating embeddings.
 */
trait ZIOEmbeddingModel:
  /**
   * Generates an embedding for a single text.
   *
   * @param text The text to embed
   * @return A ZIO effect that completes with the embedding
   */
  def embed(text: String): ZIO[Any, LangchainError, Embedding]

  /**
   * Generates embeddings for multiple texts.
   *
   * @param texts The texts to embed
   * @return A ZIO effect that completes with the list of embeddings
   */
  def embedAll(texts: List[String]): ZIO[Any, LangchainError, List[Embedding]]


/**
 * Companion object for ZIOEmbeddingModel.
 */
object ZIOEmbeddingModel:
  /**
   * Creates a mock ZIOEmbeddingModel for testing.
   *
   * @return A ZIOEmbeddingModel that creates simple embeddings based on text length
   */
  def mock(): UIO[ZIOEmbeddingModel] =
    ZIO.succeed(MockZIOEmbeddingModel())

  /**
   * Creates a ZLayer for a mock ZIOEmbeddingModel.
   *
   * @return A ZLayer that provides a mock ZIOEmbeddingModel
   */
  def mockLayer(): ZLayer[Any, Nothing, ZIOEmbeddingModel] =
    ZLayer.succeed(MockZIOEmbeddingModel())
    
  /**
   * Creates a mock ZIOEmbeddingModel instance directly.
   *
   * @return A mock ZIOEmbeddingModel
   */
  def apply(): ZIOEmbeddingModel =
    MockZIOEmbeddingModel()