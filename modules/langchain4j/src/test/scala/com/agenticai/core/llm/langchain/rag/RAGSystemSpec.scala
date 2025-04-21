package com.agenticai.core.llm.langchain.rag

import com.agenticai.core.llm.langchain.LangchainError
import com.agenticai.core.llm.langchain.embedding.ZIOEmbeddingModel
import com.agenticai.core.llm.langchain.test.MockChatLanguageModel
import com.agenticai.core.llm.langchain.vectorstore.ZIOVectorStore
import dev.langchain4j.data.document.Document
import dev.langchain4j.data.embedding.Embedding
import zio.*
import zio.test.*
import zio.test.Assertion.*

object RAGSystemSpec extends ZIOSpecDefault:

  private val mockEmbeddingModel = new ZIOEmbeddingModel:
    override def embed(text: String): ZIO[Any, LangchainError, Embedding] =
      // Create a simple embedding based on text length
      ZIO.succeed(Embedding.from(Array(text.length.toFloat)))
      
    override def embedAll(texts: List[String]): ZIO[Any, LangchainError, List[Embedding]] =
      ZIO.succeed(texts.map(text => Embedding.from(Array(text.length.toFloat))))

  def spec = suite("RAGSystem")(
    test("should index documents and answer queries") {
      for
        // Create components
        vectorStore <- ZIOVectorStore.createInMemory()
        chatModel <- ZIO.succeed(new MockChatLanguageModel(defaultResponse = "I'm answering based on the provided context."))
        
        // Create RAG system
        ragSystem <- RAGSystemBuilder.create(vectorStore, mockEmbeddingModel, chatModel)
        
        // Create test document
        document = Document.from("Paris is the capital of France.")
        
        // Index document
        ids <- ragSystem.indexDocument(document)
        
        // Query the system
        response <- ragSystem.query("What is the capital of France?")
      yield
        assertTrue(ids.nonEmpty) &&
        assertTrue(response.contains("context"))
    }
  )