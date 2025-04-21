package com.agenticai.core.llm.langchain.embedding

import zio.*

/**
 * Factory object for creating ZIOEmbeddingModel instances.
 */
object ZIOEmbeddingModelFactory:
  /**
   * Creates a mock ZIOEmbeddingModel for testing.
   *
   * @return A mock ZIOEmbeddingModel
   */
  def apply(): ZIOEmbeddingModel = MockZIOEmbeddingModel()
  
  /**
   * Creates a mock ZIOEmbeddingModel for testing.
   *
   * @return A mock ZIOEmbeddingModel
   */
  def default(): ZIOEmbeddingModel = MockZIOEmbeddingModel()
  
  /**
   * Creates a ZLayer that provides a mock ZIOEmbeddingModel.
   *
   * @return A ZLayer that provides a mock ZIOEmbeddingModel
   */
  def layer(): ZLayer[Any, Nothing, ZIOEmbeddingModel] =
    ZLayer.succeed(MockZIOEmbeddingModel())
  
  /**
   * Creates a ZLayer that provides a mock ZIOEmbeddingModel.
   *
   * @return A ZLayer that provides a mock ZIOEmbeddingModel
   */
  def defaultLayer(): ZLayer[Any, Nothing, ZIOEmbeddingModel] =
    ZLayer.succeed(MockZIOEmbeddingModel())