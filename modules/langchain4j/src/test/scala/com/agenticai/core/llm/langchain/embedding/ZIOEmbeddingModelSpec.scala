package com.agenticai.core.llm.langchain.embedding

import com.agenticai.core.llm.langchain.LangchainError
import dev.langchain4j.data.embedding.Embedding
import zio.*
import zio.test.*
import zio.test.Assertion.*

object ZIOEmbeddingModelSpec extends ZIOSpecDefault:

  override def spec: Spec[TestEnvironment, Any] =
    suite("ZIOEmbeddingModel")(
      test("embed should generate an embedding for a single text") {
        // Given
        val model = ZIOEmbeddingModel()
        val text = "Hello, world!"

        // When
        val result = model.embed(text)

        // Then
        assertZIO(result.map(_.vectorAsList().size()))(equalTo(3)) &&
        assertZIO(result.map(_.vectorAsList().get(0)))(equalTo(text.length.toFloat))
      },

      test("embedAll should generate embeddings for multiple texts") {
        // Given
        val model = ZIOEmbeddingModel()
        val texts = List("Hello", "World", "Testing")

        // When
        val result = model.embedAll(texts)

        // Then
        assertZIO(result.map(_.size))(equalTo(3)) &&
        assertZIO(result.map(_.map(_.vectorAsList().size())))(equalTo(List(3, 3, 3))) &&
        assertZIO(result.map(_.map(_.vectorAsList().get(0))))(
          equalTo(texts.map(_.length.toFloat))
        )
      },

      test("ZIOEmbeddingModelFactory should create a working model") {
        // Given
        val model = ZIOEmbeddingModelFactory()
        val text = "Factory test"

        // When
        val result = model.embed(text)

        // Then
        assertZIO(result.map(_.vectorAsList().size()))(equalTo(3)) &&
        assertZIO(result.map(_.vectorAsList().get(0)))(equalTo(text.length.toFloat))
      },

      test("ZIOEmbeddingModelFactory layer should provide a working model") {
        // Given
        val text = "Layer test"

        // When
        val result = ZIO.serviceWithZIO[ZIOEmbeddingModel](_.embed(text))

        // Then
        assertZIO(result.map(_.vectorAsList().size()))(equalTo(3)) &&
        assertZIO(result.map(_.vectorAsList().get(0)))(equalTo(text.length.toFloat))
      }.provideLayer(ZIOEmbeddingModelFactory.layer())
    )