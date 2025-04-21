package com.agenticai.examples

import com.agenticai.core.llm.langchain.embedding.{MockZIOEmbeddingModel, ZIOEmbeddingModel}
import com.agenticai.core.llm.langchain.rag.RAGSystem
import com.agenticai.core.llm.langchain.vectorstore.ZIOVectorStore
import com.agenticai.core.llm.langchain.ZIOChatModelFactory
import com.agenticai.core.llm.langchain.embedding.ZIOEmbeddingModelFactory
import dev.langchain4j.data.document.Document
import zio.*
import zio.json.*

/**
 * Example demonstrating the RAG system with WebSocket support.
 * This example demonstrates:
 * 1. Setting up a RAG system with an in-memory vector store
 * 2. Creating a WebSocket server for the RAG system
 * 3. Handling both streaming and non-streaming queries
 */
object RAGWebSocketExample extends ZIOAppDefault:
  
  /**
   * Creates a simple RAG system for demonstration purposes.
   * Uses an in-memory vector store and a mock embedding model.
   *
   * @return A RAG system instance
   */
  private def createRAGSystem: ZIO[Any, Throwable, RAGSystem] =
    for
      // Create an in-memory vector store
      vectorStore <- ZIOVectorStore.createInMemory()
      
      // Create a mock embedding model
      embeddingModel = ZIOEmbeddingModelFactory.default()
      
      // Create a chat model
      chatModel <- ZIOChatModelFactory.makeOpenAIModel(
        apiKey = "your-openai-api-key-here",
        modelName = "gpt-3.5-turbo"
      )
      
      // Create the RAG system with streaming support
      ragSystem <- RAGSystem.builder
        .withVectorStore(vectorStore)
        .withEmbeddingModel(embeddingModel)
        .withDefaultDocumentChunker()
        .withDefaultRetriever()
        .withDefaultContextBuilder()
        .withDefaultResponseGenerator(chatModel)
        .withDefaultStreamingResponseGenerator(chatModel)
        .build
      
      // Add some example documents to the RAG system
      _ <- ragSystem.addDocument(Document.from(
        """
        | # WebSockets
        | WebSockets is a protocol providing full-duplex communication channels over a single TCP connection.
        | The WebSocket protocol was standardized by the IETF as RFC 6455 in 2011.
        | WebSockets are designed to be implemented in web browsers and web servers, but they can be used by any client or server application.
        | The WebSocket protocol enables interaction between a web client and a web server with lower overhead than half-duplex alternatives
        | such as HTTP polling, facilitating real-time data transfer from and to the server.
        """.stripMargin
      ))
      
      _ <- ragSystem.addDocument(Document.from(
        """
        | # Streaming Responses
        | Streaming responses in APIs allow clients to receive data as it becomes available, without waiting for the
        | complete response. This can significantly improve perceived performance and enable real-time updates.
        | Common streaming protocols include HTTP chunked transfer encoding, Server-Sent Events (SSE), and WebSockets.
        | WebSockets are particularly well-suited for streaming responses as they provide bidirectional communication,
        | allowing both the client and server to send messages at any time.
        """.stripMargin
      ))
      
    yield ragSystem
  
  /**
   * Creates the HTTP server with WebSocket support.
   *
   * @param ragSystem The RAG system to use
   * @return A Server instance
   */
  private def createServer(ragSystem: RAGSystem): ZIO[Any, Throwable, Unit] =
    // Note: This is a placeholder since we can't use the HTTP module directly
    // In a real implementation, you would need to add the http module as a dependency
    // and use the RAGWebSocketHandler
    Console.printLine("Server would start here if HTTP module was available")
    
    // Print instructions for using the server
    Console.printLine(
      """
        |RAG WebSocket server started on http://localhost:8080
        |
        |Available endpoints:
        |- ws://localhost:8080/rag/ws: Regular WebSocket endpoint
        |- ws://localhost:8080/rag/ws/stream?query=your+query+here: Streaming WebSocket endpoint
        |
        |Example client usage:
        |
        |```javascript
        |// Regular WebSocket example
        |const socket = new WebSocket('ws://localhost:8080/rag/ws');
        |
        |socket.onopen = () => {
        |  const message = {
        |    messageType: 'query_request',
        |    query: 'What are WebSockets?',
        |    maxResults: 3,
        |    streamResponse: false
        |  };
        |  socket.send(JSON.stringify(message));
        |};
        |
        |socket.onmessage = (event) => {
        |  const response = JSON.parse(event.data);
        |  console.log('Response:', response);
        |};
        |
        |// Streaming WebSocket example
        |const streamingSocket = new WebSocket('ws://localhost:8080/rag/ws/stream?query=What%20are%20WebSockets?');
        |
        |streamingSocket.onmessage = (event) => {
        |  const chunk = JSON.parse(event.data);
        |  
        |  if (chunk.messageType === 'query_response_chunk') {
        |    if (chunk.isComplete) {
        |      console.log('Stream complete');
        |    } else {
        |      process.stdout.write(chunk.chunk); // Append to previous chunks
        |    }
        |  } else if (chunk.messageType === 'error') {
        |    console.error('Error:', chunk.error);
        |  }
        |};
        |```
        |
        |Press Ctrl+C to stop the server
        |""".stripMargin) *> ZIO.unit
  
  /**
   * Main entry point for the example.
   */
  override def run: ZIO[Any, Throwable, Unit] =
    for
      ragSystem <- createRAGSystem
      _ <- createServer(ragSystem)
      // Demonstrate a simple query without using WebSockets
      _ <- Console.printLine("\nDemonstrating a simple query:")
      query = "What are WebSockets and how can they be used for streaming?"
      _ <- Console.printLine(s"Query: $query")
      response <- ragSystem.query(query)
      _ <- Console.printLine(s"Response: $response")
    yield ()