package com.agenticai.core.http.websocket

import com.agenticai.core.http.websocket.WebSocketProtocol.{ClientMessage, ServerMessage, WebSocketMessage}
import com.agenticai.core.http.websocket.WebSocketProtocol.Codecs._
import com.agenticai.core.llm.langchain.rag.{RAGError, RAGSystem}
import dev.langchain4j.data.document.Document
import dev.langchain4j.data.document.Metadata
import zio._
import zio.http._
import zio.stream.ZStream

import scala.jdk.CollectionConverters._

/**
 * Handles WebSocket connections for the RAG system.
 * This class provides endpoints for both regular and streaming interactions with the RAG system.
 *
 * @param ragSystem The RAG system instance to use for processing requests
 */
class RAGWebSocketHandler(ragSystem: RAGSystem) {

  /**
   * Creates an HTTP app with all RAG WebSocket endpoints.
   *
   * @return An HTTP app with RAG WebSocket endpoints
   */
  def webSocketApp: HttpApp[Any, Throwable] = {
    // Create a simple app with WebSocket endpoints
    val app = Http.collectZIO[Request] {
      // Regular WebSocket endpoint
      case req if req.method == Method.GET && req.url.path.segments == List("rag", "ws") =>
        ZIO.succeed(
          Response.text("WebSocket endpoint not implemented")
            .withStatus(Status.NotImplemented)
        )
      
      // Streaming WebSocket endpoint
      case req if req.method == Method.GET && req.url.path.segments == List("rag", "ws", "stream") =>
        val queryParam = req.url.queryParams.get("query")
        
        queryParam match {
          case Some(query) =>
            ZIO.succeed(
              Response.text("Streaming WebSocket endpoint not implemented")
                .withStatus(Status.NotImplemented)
            )
            
          case None =>
            // Return a BadRequest response when the query parameter is missing
            ZIO.succeed(Response.text("Missing query parameter").withStatus(Status.BadRequest))
        }
    }
    
    // Convert to an HttpApp
    app.catchAllCauseZIO { cause =>
      ZIO.succeed(Response.text(s"Error: ${cause.prettyPrint}").withStatus(Status.InternalServerError))
    }
  }

  /**
   * Handles a client message and returns a response.
   *
   * @param message The client message to handle
   * @return A ZIO effect that completes with a response message
   */
  def handleClientMessage(message: WebSocketMessage): ZIO[Any, Throwable, WebSocketMessage] = {
    message match {
      case queryRequest: ClientMessage.QueryRequest =>
        if (queryRequest.streamResponse) {
          // Return an error - streaming should use the dedicated endpoint
          ZIO.succeed(ServerMessage.ErrorMessage(
            "Streaming requests should use the dedicated streaming endpoint: ws://host/rag/ws/stream?query=..."
          ))
        } else {
          // Process a regular query
          ragSystem.query(queryRequest.query, queryRequest.maxResults)
            .map(response => ServerMessage.QueryResponse(response))
            .catchAll(error => ZIO.succeed(ServerMessage.ErrorMessage(s"Query error: ${error.message}")))
        }
      
      case addDocRequest: ClientMessage.AddDocumentRequest =>
        // Create a document from the request
        val metadata = new Metadata(addDocRequest.metadata.asJava)
        val document = Document.from(addDocRequest.content, metadata)
        
        // Add the document to the RAG system
        ragSystem.addDocument(document)
          .map(ids => ServerMessage.AddDocumentResponse(ids))
          .catchAll(error => ZIO.succeed(ServerMessage.ErrorMessage(s"Document error: ${error.message}")))
      
      case _ =>
        ZIO.succeed(ServerMessage.ErrorMessage(s"Unsupported message type: ${message.messageType}"))
    }
  }

  /**
   * Streams a query response.
   *
   * @param queryRequest The query request
   * @return A ZStream that emits response chunks
   */
  def streamQueryResponse(queryRequest: ClientMessage.QueryRequest): ZStream[Any, Nothing, WebSocketMessage] = {
    ragSystem.queryStream(queryRequest.query, queryRequest.maxResults)
      .map(chunk => ServerMessage.QueryResponseChunk(chunk, isComplete = false))
      .catchAll(error => ZStream.succeed(ServerMessage.ErrorMessage(s"Streaming error: ${error.message}")))
      // Add a completion message at the end
      .concat(ZStream.succeed(ServerMessage.QueryResponseChunk("", isComplete = true)))
  }
}