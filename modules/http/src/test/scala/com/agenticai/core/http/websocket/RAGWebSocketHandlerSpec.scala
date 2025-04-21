package com.agenticai.core.http.websocket

import com.agenticai.core.http.websocket.WebSocketProtocol.{ClientMessage, ServerMessage, WebSocketMessage}
import com.agenticai.core.http.websocket.WebSocketProtocol.Codecs._
import com.agenticai.core.llm.langchain.test.MockChatLanguageModel
import com.agenticai.core.llm.langchain.rag.{RAGError, RAGSystem, RAGSystemBuilder}
import com.agenticai.core.llm.langchain.embedding.{MockZIOEmbeddingModel, ZIOEmbeddingModel}
import com.agenticai.core.llm.langchain.vectorstore.ZIOVectorStore
import com.agenticai.core.llm.langchain.ZIOChatLanguageModel
import dev.langchain4j.data.document.Document
import zio.*
import zio.http.*
import zio.json.*
import zio.stream.*
import zio.test.*

object RAGWebSocketHandlerSpec extends ZIOSpecDefault {

  def spec = suite("RAGWebSocketHandlerSpec")(
    // Test for handling regular query requests
    test("WebSocket handler should process regular query requests") {
      for {
        // Create components
        vectorStore <- ZIOVectorStore.createInMemory()
        chatModel <- ZIO.succeed(new MockChatLanguageModel(defaultResponse = "This is a test response"))
        embeddingModel = MockZIOEmbeddingModel()
        
        // Create RAG system
        ragSystem <- RAGSystemBuilder.create(vectorStore, embeddingModel, chatModel)
        
        // Create handler
        handler = new RAGWebSocketHandler(ragSystem)
        
        // Create a query request
        queryRequest = ClientMessage.QueryRequest(
          query = "Test query",
          maxResults = 3,
          streamResponse = false
        )
        
        // Test the handler directly with the query request
        response <- handler.handleClientMessage(queryRequest)
        
        // Parse the response
        queryResponse = response.asInstanceOf[ServerMessage.QueryResponse]
      } yield {
        // Assert the response contains the expected text
        assertTrue(
          queryResponse.response == "This is a test response",
          queryResponse.messageType == "query_response"
        )
      }
    },
    
    // Test for streaming query responses
    test("WebSocket handler should stream query responses") {
      for {
        // Create components
        vectorStore <- ZIOVectorStore.createInMemory()
        embeddingModel = MockZIOEmbeddingModel()
        
        // Create a mock model that will return chunks
        chatModel = new MockChatLanguageModel(
          generateStreamFn = _ => ZStream.fromIterable(List("This", " is", " a", " streaming", " response"))
        )
        
        // Create RAG system
        ragSystem <- RAGSystemBuilder.create(vectorStore, embeddingModel, chatModel)
        
        // Create handler
        handler = new RAGWebSocketHandler(ragSystem)
        
        // Create a streaming query request
        queryRequest = ClientMessage.QueryRequest(
          query = "Test streaming query",
          maxResults = 3,
          streamResponse = true
        )
        
        // Test the streaming response
        responseStream <- handler.streamQueryResponse(queryRequest).runCollect
        
        // Verify we received all the expected chunks
        chunks = responseStream.collect {
          case chunk: ServerMessage.QueryResponseChunk if !chunk.isComplete => chunk.chunk
        }
        
        // And a final completion message
        completions = responseStream.collect {
          case chunk: ServerMessage.QueryResponseChunk if chunk.isComplete => chunk
        }
        
        // Concatenate the chunks to verify the full response
        fullResponse = chunks.foldLeft("")(_ + _)
      } yield {
        assertTrue(
          chunks == List("This", " is", " a", " streaming", " response"),
          fullResponse == "This is a streaming response",
          completions.size == 1,
          completions.head.isComplete
        )
      }
    },
    
    // Test for document addition functionality
    test("WebSocket handler should process document addition requests") {
      for {
        // Create components
        vectorStore <- ZIOVectorStore.createInMemory()
        chatModel <- ZIO.succeed(new MockChatLanguageModel(defaultResponse = "Test response"))
        embeddingModel = MockZIOEmbeddingModel()
        
        // Create RAG system
        ragSystem <- RAGSystemBuilder.create(vectorStore, embeddingModel, chatModel)
        
        // Create handler
        handler = new RAGWebSocketHandler(ragSystem)
        
        // Create a document addition request
        addDocRequest = ClientMessage.AddDocumentRequest(
          content = "Test document content",
          title = Some("Test Document"),
          metadata = Map("author" -> "Test Author", "category" -> "Test")
        )
        
        // Test the handler directly with the document request
        response <- handler.handleClientMessage(addDocRequest)
        
        // Parse the response
        docResponse = response.asInstanceOf[ServerMessage.AddDocumentResponse]
      } yield {
        // Assert the response contains document IDs
        assertTrue(
          docResponse.documentIds.nonEmpty,
          docResponse.messageType == "add_document_response"
        )
      }
    },
    
    // Test for handling streaming requests in regular endpoint
    test("WebSocket handler should reject streaming requests in regular endpoint") {
      for {
        // Create components
        vectorStore <- ZIOVectorStore.createInMemory()
        chatModel <- ZIO.succeed(new MockChatLanguageModel(defaultResponse = "Test response"))
        embeddingModel = MockZIOEmbeddingModel()
        
        // Create RAG system
        ragSystem <- RAGSystemBuilder.create(vectorStore, embeddingModel, chatModel)
        
        // Create handler
        handler = new RAGWebSocketHandler(ragSystem)
        
        // Create a streaming query request sent to the regular endpoint
        queryRequest = ClientMessage.QueryRequest(
          query = "Test query",
          maxResults = 3,
          streamResponse = true
        )
        
        // Test the handler directly with the streaming request
        response <- handler.handleClientMessage(queryRequest)
        
        // Parse the response
        errorResponse = response.asInstanceOf[ServerMessage.ErrorMessage]
      } yield {
        // Assert the response is an error message about using the streaming endpoint
        assertTrue(
          errorResponse.error.contains("Streaming requests should use the dedicated streaming endpoint"),
          errorResponse.messageType == "error"
        )
      }
    },
    
    // Test for streaming endpoint with missing query parameter
    test("WebSocket app should handle missing query parameter in streaming endpoint") {
      for {
        // Create components
        vectorStore <- ZIOVectorStore.createInMemory()
        chatModel <- ZIO.succeed(new MockChatLanguageModel(defaultResponse = "Test response"))
        embeddingModel = MockZIOEmbeddingModel()
        
        // Create RAG system
        ragSystem <- RAGSystemBuilder.create(vectorStore, embeddingModel, chatModel)
        
        // Create handler
        handler = new RAGWebSocketHandler(ragSystem)
        
        // Create a request to the streaming endpoint without a query parameter
        request = Request.get(URL(Root / "rag" / "ws" / "stream"))
        
        // Process the request
        response <- handler.webSocketApp.runZIO(request).catchAllCause { cause =>
          // In case of an error, return a response that we can check
          ZIO.succeed(Response.text(s"Error: ${cause.prettyPrint}").withStatus(Status.InternalServerError))
        }
        
        // Get the response body as a string
        bodyString <- response.body.asString
      } yield {
        // Assert the response contains an error about the missing query parameter
        assertTrue(
          response.status == Status.InternalServerError,
          bodyString.contains("None")
        )
      }
    }
  )
}