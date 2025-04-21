package com.agenticai.examples

import com.agenticai.core.llm.langchain.ZIOChatModelFactory
import com.agenticai.core.llm.langchain.embedding.ZIOEmbeddingModelFactory
import com.agenticai.core.llm.langchain.rag.{RAGSystem, RAGSystemBuilder}
import com.agenticai.core.llm.langchain.vectorstore.ZIOVectorStore
import dev.langchain4j.data.document.Document
import zio.*

/**
 * Example demonstrating the usage of the RAG system with a simple knowledge base.
 */
object RAGExample extends ZIOAppDefault:

  val run = myProgram.exitCode

  val myProgram = for
    // Create embedding model (uses a mock for demonstration purposes)
    embeddingModel <- ZIO.succeed(MockEmbeddingModel())
    
    // Create chat model (uses a mock for demonstration purposes)
    chatModel <- ZIO.succeed(MockChatModel())
    
    // Create in-memory vector store
    vectorStore <- ZIOVectorStore.createInMemory()
    
    // Create in-memory RAG system
    builder = RAGSystem.builder
      .withVectorStore(vectorStore)
      .withEmbeddingModel(embeddingModel)
      .withDefaultDocumentChunker()
      .withDefaultRetriever()
      .withDefaultContextBuilder()
      .withDefaultResponseGenerator(chatModel)
    
    ragSystem <- builder.build
    
    // Create and index some sample documents
    documents = List(
      Document.from("Paris is the capital of France. It is known for the Eiffel Tower."),
      Document.from("Rome is the capital of Italy. It is known for the Colosseum."),
      Document.from("Berlin is the capital of Germany. It is known for the Brandenburg Gate.")
    )
    
    // Index all documents
    _ <- Console.printLine("Indexing documents...")
    _ <- ZIO.foreachDiscard(documents)(doc => ragSystem.indexDocument(doc))
    _ <- Console.printLine("Documents indexed successfully.")
    
    // Test queries
    queries = List(
      "What is the capital of France?",
      "What is Italy known for?",
      "Tell me about Germany."
    )
    
    // Process each query
    _ <- ZIO.foreach(queries) { query =>
      for
        _ <- Console.printLine(s"\nQuery: $query")
        response <- ragSystem.query(query)
        _ <- Console.printLine(s"Response: $response")
      yield ()
    }
  yield ()

/**
 * Simple mock embedding model for demonstration purposes.
 */
case class MockEmbeddingModel() extends com.agenticai.core.llm.langchain.embedding.ZIOEmbeddingModel:
  import dev.langchain4j.data.embedding.Embedding
  import com.agenticai.core.llm.langchain.LangchainError
  
  override def embed(text: String): ZIO[Any, LangchainError, Embedding] =
    // Create a simple embedding based on text length and first character
    ZIO.succeed(Embedding.from(Array(text.length.toFloat, text.headOption.map(_.toInt.toFloat).getOrElse(0f))))
    
  override def embedAll(texts: List[String]): ZIO[Any, LangchainError, List[Embedding]] =
    ZIO.succeed(texts.map(text =>
      Embedding.from(Array(text.length.toFloat, text.headOption.map(_.toInt.toFloat).getOrElse(0f)))
    ))

/**
 * Simple mock chat model for demonstration purposes.
 */
case class MockChatModel() extends com.agenticai.core.llm.langchain.ZIOChatLanguageModel:
  import com.agenticai.core.llm.langchain.LangchainError
  import dev.langchain4j.data.message.{AiMessage, ChatMessage}
  import zio.stream.ZStream
  
  override def generate(messages: List[ChatMessage]): ZIO[Any, LangchainError, AiMessage] =
    // Generate a fixed response for demonstration purposes
    ZIO.succeed(AiMessage.from("This is a mock response from the RAG system."))
  
  override def generateStream(messages: List[ChatMessage]): ZStream[Any, LangchainError, String] =
    // Generate a fixed streaming response for demonstration purposes
    ZStream.succeed("This is a mock streaming response from the RAG system.")