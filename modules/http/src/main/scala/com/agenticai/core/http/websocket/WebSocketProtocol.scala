package com.agenticai.core.http.websocket

import zio.json.*

/**
 * Defines the WebSocket protocol for the RAG system.
 * This includes message types for both client-to-server (requests)
 * and server-to-client (responses) communication.
 */
object WebSocketProtocol:
  /**
   * Base trait for all WebSocket messages
   */
  sealed trait WebSocketMessage:
    /**
     * Type of the message, used for routing
     */
    def messageType: String

  /**
   * Client-to-server messages (requests)
   */
  object ClientMessage:
    /**
     * Request to query the RAG system
     *
     * @param query The query string
     * @param maxResults Maximum number of results to return
     * @param streamResponse Whether to stream the response
     */
    final case class QueryRequest(
      query: String,
      maxResults: Int = 5,
      streamResponse: Boolean = false
    ) extends WebSocketMessage:
      override val messageType: String = "query_request"

    /**
     * Request to add a document to the RAG system
     *
     * @param content The document content
     * @param title Optional document title
     * @param metadata Optional document metadata as key-value pairs
     */
    final case class AddDocumentRequest(
      content: String,
      title: Option[String] = None,
      metadata: Map[String, String] = Map.empty
    ) extends WebSocketMessage:
      override val messageType: String = "add_document_request"

  /**
   * Server-to-client messages (responses)
   */
  object ServerMessage:
    /**
     * Response to a query request
     *
     * @param response The complete response text
     * @param sources List of document IDs used as sources (optional)
     */
    final case class QueryResponse(
      response: String,
      sources: List[String] = List.empty
    ) extends WebSocketMessage:
      override val messageType: String = "query_response"

    /**
     * Chunk of a streaming response
     *
     * @param chunk The text chunk
     * @param isComplete Whether this is the final chunk
     */
    final case class QueryResponseChunk(
      chunk: String,
      isComplete: Boolean = false
    ) extends WebSocketMessage:
      override val messageType: String = "query_response_chunk"

    /**
     * Response to an add document request
     *
     * @param documentIds List of document IDs that were added
     */
    final case class AddDocumentResponse(
      documentIds: List[String]
    ) extends WebSocketMessage:
      override val messageType: String = "add_document_response"

    /**
     * Error message
     *
     * @param error The error message
     * @param code Optional error code
     */
    final case class ErrorMessage(
      error: String,
      code: Option[String] = None
    ) extends WebSocketMessage:
      override val messageType: String = "error"

  /**
   * JSON codecs for WebSocket messages
   */
  object Codecs:
    // ClientMessage encoders/decoders
    implicit val queryRequestEncoder: JsonEncoder[ClientMessage.QueryRequest] =
      DeriveJsonEncoder.gen[ClientMessage.QueryRequest]
    implicit val queryRequestDecoder: JsonDecoder[ClientMessage.QueryRequest] =
      DeriveJsonDecoder.gen[ClientMessage.QueryRequest]

    implicit val addDocumentRequestEncoder: JsonEncoder[ClientMessage.AddDocumentRequest] =
      DeriveJsonEncoder.gen[ClientMessage.AddDocumentRequest]
    implicit val addDocumentRequestDecoder: JsonDecoder[ClientMessage.AddDocumentRequest] =
      DeriveJsonDecoder.gen[ClientMessage.AddDocumentRequest]

    // ServerMessage encoders/decoders
    implicit val queryResponseEncoder: JsonEncoder[ServerMessage.QueryResponse] =
      DeriveJsonEncoder.gen[ServerMessage.QueryResponse]
    implicit val queryResponseDecoder: JsonDecoder[ServerMessage.QueryResponse] =
      DeriveJsonDecoder.gen[ServerMessage.QueryResponse]

    implicit val queryResponseChunkEncoder: JsonEncoder[ServerMessage.QueryResponseChunk] =
      DeriveJsonEncoder.gen[ServerMessage.QueryResponseChunk]
    implicit val queryResponseChunkDecoder: JsonDecoder[ServerMessage.QueryResponseChunk] =
      DeriveJsonDecoder.gen[ServerMessage.QueryResponseChunk]

    implicit val addDocumentResponseEncoder: JsonEncoder[ServerMessage.AddDocumentResponse] =
      DeriveJsonEncoder.gen[ServerMessage.AddDocumentResponse]
    implicit val addDocumentResponseDecoder: JsonDecoder[ServerMessage.AddDocumentResponse] =
      DeriveJsonDecoder.gen[ServerMessage.AddDocumentResponse]

    implicit val errorMessageEncoder: JsonEncoder[ServerMessage.ErrorMessage] =
      DeriveJsonEncoder.gen[ServerMessage.ErrorMessage]
    implicit val errorMessageDecoder: JsonDecoder[ServerMessage.ErrorMessage] =
      DeriveJsonDecoder.gen[ServerMessage.ErrorMessage]

    // Extension methods for WebSocketMessage
    extension (message: WebSocketMessage)
      /**
       * Convert the message to a JSON string
       */
      def toJson: String = 
        message match
          case req: ClientMessage.QueryRequest => queryRequestEncoder.encodeJson(req, None).toString
          case req: ClientMessage.AddDocumentRequest => addDocumentRequestEncoder.encodeJson(req, None).toString
          case res: ServerMessage.QueryResponse => queryResponseEncoder.encodeJson(res, None).toString
          case res: ServerMessage.QueryResponseChunk => queryResponseChunkEncoder.encodeJson(res, None).toString
          case res: ServerMessage.AddDocumentResponse => addDocumentResponseEncoder.encodeJson(res, None).toString
          case res: ServerMessage.ErrorMessage => errorMessageEncoder.encodeJson(res, None).toString

    extension (json: String)
      /**
       * Parse a JSON string to a WebSocketMessage
       */
      def fromJson[A: JsonDecoder]: Either[String, A] = 
        JsonDecoder[A].decodeJson(json)