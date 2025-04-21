# RAG WebSocket Usage Guide

## Introduction

This guide demonstrates how to use the WebSocket-based Retrieval Augmented Generation (RAG) system in the Agentic AI Framework. The WebSocket interface provides real-time, bidirectional communication between clients and the RAG system, enabling both standard request-response patterns and streaming responses.

The RAG WebSocket functionality offers several advantages:
- **Real-time streaming responses**: Receive generated text as it's produced, improving perceived responsiveness
- **Persistent connections**: Maintain a single connection for multiple interactions
- **Bidirectional communication**: Both client and server can send messages at any time
- **Reduced overhead**: Lower latency compared to HTTP request-response cycles

## WebSocket Endpoints

The RAG system exposes two primary WebSocket endpoints:

1. **Regular WebSocket Endpoint**: `ws://host:port/rag/ws`
   - Used for standard request-response interactions
   - Supports query requests and document addition

2. **Streaming WebSocket Endpoint**: `ws://host:port/rag/ws/stream?query=your+query+here`
   - Dedicated to streaming responses
   - Query is provided as a URL parameter

## Message Protocol

Communication with the RAG WebSocket server uses a JSON-based protocol defined in `WebSocketProtocol`. All messages include a `messageType` field that identifies the message type.

### Client-to-Server Messages

1. **Query Request**
   ```json
   {
     "messageType": "query_request",
     "query": "What is retrieval-augmented generation?",
     "maxResults": 5,
     "streamResponse": false
   }
   ```
   - `query`: The query string
   - `maxResults`: Maximum number of results to return (default: 5)
   - `streamResponse`: Whether to stream the response (default: false)

2. **Add Document Request**
   ```json
   {
     "messageType": "add_document_request",
     "content": "Document content goes here...",
     "title": "Optional document title",
     "metadata": {
       "author": "John Doe",
       "date": "2025-04-21"
     }
   }
   ```
   - `content`: The document content
   - `title`: Optional document title
   - `metadata`: Optional key-value pairs as document metadata

### Server-to-Client Messages

1. **Query Response** (non-streaming)
   ```json
   {
     "messageType": "query_response",
     "response": "Complete response text...",
     "sources": ["doc1", "doc2"]
   }
   ```
   - `response`: The complete response text
   - `sources`: List of document IDs used as sources (optional)

2. **Query Response Chunk** (streaming)
   ```json
   {
     "messageType": "query_response_chunk",
     "chunk": "Partial response text...",
     "isComplete": false
   }
   ```
   - `chunk`: A portion of the response text
   - `isComplete`: Whether this is the final chunk

3. **Add Document Response**
   ```json
   {
     "messageType": "add_document_response",
     "documentIds": ["doc1", "doc2"]
   }
   ```
   - `documentIds`: List of document IDs that were added

4. **Error Message**
   ```json
   {
     "messageType": "error",
     "error": "Error message",
     "code": "ERROR_CODE"
   }
   ```
   - `error`: The error message
   - `code`: Optional error code

## Client Implementation Examples

### JavaScript Client

#### Setting Up a WebSocket Connection

```javascript
// Connect to the regular WebSocket endpoint
const socket = new WebSocket('ws://localhost:8080/rag/ws');

// Set up event handlers
socket.onopen = () => {
  console.log('Connection established');
};

socket.onclose = (event) => {
  console.log(`Connection closed: ${event.code} ${event.reason}`);
};

socket.onerror = (error) => {
  console.error('WebSocket error:', error);
};

socket.onmessage = (event) => {
  const message = JSON.parse(event.data);
  console.log('Received message:', message);
  
  // Handle different message types
  switch (message.messageType) {
    case 'query_response':
      handleQueryResponse(message);
      break;
    case 'add_document_response':
      handleDocumentResponse(message);
      break;
    case 'error':
      handleError(message);
      break;
    default:
      console.warn('Unknown message type:', message.messageType);
  }
};
```

#### Sending a Query Request

```javascript
function sendQuery(query, maxResults = 5) {
  const queryRequest = {
    messageType: 'query_request',
    query: query,
    maxResults: maxResults,
    streamResponse: false
  };
  
  socket.send(JSON.stringify(queryRequest));
}

// Example usage
sendQuery('What is retrieval-augmented generation?');
```

#### Adding a Document

```javascript
function addDocument(content, title = null, metadata = {}) {
  const addDocumentRequest = {
    messageType: 'add_document_request',
    content: content,
    title: title,
    metadata: metadata
  };
  
  socket.send(JSON.stringify(addDocumentRequest));
}

// Example usage
addDocument(
  'Retrieval-augmented generation (RAG) is an AI framework that combines retrieval-based and generation-based approaches...',
  'RAG Overview',
  { author: 'AI Research Team', category: 'AI Techniques' }
);
```

#### Handling Responses

```javascript
function handleQueryResponse(message) {
  console.log('Query response:', message.response);
  if (message.sources && message.sources.length > 0) {
    console.log('Sources:', message.sources);
  }
}

function handleDocumentResponse(message) {
  console.log('Documents added with IDs:', message.documentIds);
}

function handleError(message) {
  console.error('Error:', message.error);
  if (message.code) {
    console.error('Error code:', message.code);
  }
}
```

### Streaming Responses

For streaming responses, connect directly to the streaming endpoint:

```javascript
function connectToStreamingEndpoint(query) {
  const encodedQuery = encodeURIComponent(query);
  const streamingSocket = new WebSocket(`ws://localhost:8080/rag/ws/stream?query=${encodedQuery}`);
  
  let fullResponse = '';
  
  streamingSocket.onopen = () => {
    console.log('Streaming connection established');
  };
  
  streamingSocket.onmessage = (event) => {
    const message = JSON.parse(event.data);
    
    if (message.messageType === 'query_response_chunk') {
      // Append the chunk to the accumulated response
      fullResponse += message.chunk;
      
      // Display the chunk (for real-time updates)
      process.stdout.write(message.chunk);
      
      // Check if this is the final chunk
      if (message.isComplete) {
        console.log('\nStream complete');
        console.log('Full response:', fullResponse);
        streamingSocket.close();
      }
    } else if (message.messageType === 'error') {
      console.error('Error:', message.error);
      streamingSocket.close();
    }
  };
  
  streamingSocket.onclose = () => {
    console.log('Streaming connection closed');
  };
  
  return streamingSocket;
}

// Example usage
const streamingSocket = connectToStreamingEndpoint('What is retrieval-augmented generation?');
```

### Scala Client

#### Setting Up a WebSocket Client

```scala
import zio.*
import zio.http.*
import zio.json.*
import com.agenticai.core.http.websocket.WebSocketProtocol
import com.agenticai.core.http.websocket.WebSocketProtocol.{ClientMessage, ServerMessage, WebSocketMessage}
import com.agenticai.core.http.websocket.WebSocketProtocol.Codecs.*

object RAGWebSocketClient extends ZIOAppDefault:
  // URL for the WebSocket server
  private val serverUrl = "ws://localhost:8080/rag/ws"
  
  // Create the WebSocket client
  private def createClient: ZIO[Any, Throwable, WebSocketClient] =
    val clientUrl = URL.decode(serverUrl).getOrElse(URL.empty)
    Client.websocket(clientUrl)
  
  // Send a query and handle the response
  private def sendQuery(query: String, maxResults: Int = 5): ZIO[Any, Throwable, Unit] =
    createClient.flatMap { client =>
      for
        _ <- Console.printLine(s"Connecting to $serverUrl...")
        _ <- Console.printLine(s"Sending query: $query")
        
        // Create the query message
        queryMessage = ClientMessage.QueryRequest(
          query = query,
          maxResults = maxResults,
          streamResponse = false
        ).asInstanceOf[WebSocketMessage]
        
        // Convert to JSON
        jsonMessage = queryMessage.toJson
        
        // Send the message and wait for the response
        response <- client.sendText(jsonMessage) *>
          client.receiveText
        
        // Parse and handle the response
        _ <- ZIO.attempt {
          val parsedResponse = response.fromJson[WebSocketMessage]
          parsedResponse match
            case Right(serverMessage) =>
              serverMessage match
                case queryResponse: ServerMessage.QueryResponse =>
                  println(s"Received response: ${queryResponse.response}")
                  if (queryResponse.sources.nonEmpty)
                    println(s"Sources: ${queryResponse.sources.mkString(", ")}")
                case error: ServerMessage.ErrorMessage =>
                  println(s"Received error: ${error.error}")
                case _ =>
                  println(s"Received unexpected message type: ${serverMessage.messageType}")
            case Left(error) =>
              println(s"Error parsing response: $error")
        }
        
        // Close the connection
        _ <- client.close
      yield ()
    }
  
  // Add a document and handle the response
  private def addDocument(
    content: String, 
    title: Option[String] = None, 
    metadata: Map[String, String] = Map.empty
  ): ZIO[Any, Throwable, Unit] =
    createClient.flatMap { client =>
      for
        _ <- Console.printLine(s"Connecting to $serverUrl...")
        _ <- Console.printLine(s"Adding document${title.map(t => s" '$t'").getOrElse("")}...")
        
        // Create the add document message
        addDocMessage = ClientMessage.AddDocumentRequest(
          content = content,
          title = title,
          metadata = metadata
        ).asInstanceOf[WebSocketMessage]
        
        // Convert to JSON
        jsonMessage = addDocMessage.toJson
        
        // Send the message and wait for the response
        response <- client.sendText(jsonMessage) *>
          client.receiveText
        
        // Parse and handle the response
        _ <- ZIO.attempt {
          val parsedResponse = response.fromJson[WebSocketMessage]
          parsedResponse match
            case Right(serverMessage) =>
              serverMessage match
                case addDocResponse: ServerMessage.AddDocumentResponse =>
                  println(s"Document added with IDs: ${addDocResponse.documentIds.mkString(", ")}")
                case error: ServerMessage.ErrorMessage =>
                  println(s"Received error: ${error.error}")
                case _ =>
                  println(s"Received unexpected message type: ${serverMessage.messageType}")
            case Left(error) =>
              println(s"Error parsing response: $error")
        }
        
        // Close the connection
        _ <- client.close
      yield ()
    }
  
  // Main entry point
  override def run: ZIO[Any, Throwable, Unit] =
    for
      _ <- sendQuery("What is retrieval-augmented generation?")
    yield ()
```

#### Streaming Client in Scala

```scala
import zio.*
import zio.http.*
import zio.json.*
import com.agenticai.core.http.websocket.WebSocketProtocol
import com.agenticai.core.http.websocket.WebSocketProtocol.{ServerMessage, WebSocketMessage}
import com.agenticai.core.http.websocket.WebSocketProtocol.Codecs.*

object RAGWebSocketStreamingClient extends ZIOAppDefault:
  // Base URL for the WebSocket server
  private val serverBaseUrl = "ws://localhost:8080/rag/ws/stream"
  
  // Send a streaming query and handle the response chunks
  private def sendStreamingQuery(query: String): ZIO[Any, Throwable, Unit] =
    // Encode the query parameter
    val encodedQuery = java.net.URLEncoder.encode(query, "UTF-8")
    val serverUrl = s"$serverBaseUrl?query=$encodedQuery"
    
    // Create the WebSocket client
    val clientUrl = URL.decode(serverUrl).getOrElse(URL.empty)
    val webSocketClient = Client.websocket(clientUrl)
    
    // Connect and handle the response stream
    webSocketClient.flatMap { client =>
      for
        _ <- Console.printLine(s"Connecting to $serverUrl...")
        _ <- Console.printLine(s"Streaming query: $query\n")
        
        // Print response chunks as they arrive
        _ <- client.receiveTextStream
          .mapZIO { text =>
            ZIO.attempt {
              val parsedMessage = text.fromJson[WebSocketMessage]
              parsedMessage match
                case Right(serverMessage) =>
                  serverMessage match
                    case chunk: ServerMessage.QueryResponseChunk =>
                      if chunk.isComplete then
                        println("\nStream complete")
                      else
                        // Print the chunk without a newline to simulate streaming
                        print(chunk.chunk)
                    case error: ServerMessage.ErrorMessage =>
                      println(s"\nReceived error: ${error.error}")
                    case _ =>
                      println(s"\nReceived unexpected message type: ${serverMessage.messageType}")
                case Left(error) =>
                  println(s"\nError parsing response: $error")
            }
          }
          .runDrain
          .timeout(Duration.fromSeconds(60))
          .flatMap {
            case Some(_) => ZIO.unit
            case None => Console.printLine("\nStream timeout after 60 seconds")
          }
        
        // Close the connection
        _ <- client.close
      yield ()
    }
  
  // Main entry point
  override def run: ZIO[Any, Throwable, Unit] =
    sendStreamingQuery("What is retrieval-augmented generation?")
```

## Server-Side Configuration

### Setting Up the RAG WebSocket Server

To set up a RAG WebSocket server, you need to:

1. Create a RAG system with streaming support
2. Create a WebSocket handler for the RAG system
3. Create an HTTP app with the WebSocket endpoints
4. Start the server

Here's a complete example:

```scala
import com.agenticai.core.http.websocket.RAGWebSocketHandler
import com.agenticai.core.llm.langchain.ZIOChatLanguageModel
import com.agenticai.core.llm.langchain.embedding.ZIOEmbeddingModel
import com.agenticai.core.llm.langchain.rag.RAGSystem
import com.agenticai.core.llm.langchain.vectorstore.ZIOVectorStore
import com.agenticai.core.llm.langchain.ZIOChatModelFactory
import com.agenticai.core.llm.langchain.embedding.ZIOEmbeddingModelFactory
import com.agenticai.core.llm.langchain.vectorstore.InMemoryZIOVectorStore
import dev.langchain4j.data.document.Document
import zio.*
import zio.http.*

object RAGWebSocketServer extends ZIOAppDefault:
  
  // Create a RAG system with streaming support
  private def createRAGSystem: ZIO[Any, Throwable, RAGSystem] =
    for
      // Create an in-memory vector store
      vectorStore <- ZIO.succeed(InMemoryZIOVectorStore())
      
      // Create an embedding model
      embeddingModel <- ZIOEmbeddingModelFactory.createAllMiniLmL6V2EmbeddingModel
      
      // Create a chat model
      chatModel <- ZIOChatModelFactory.createOpenAIChatModel(
        apiKey = System.getenv("OPENAI_API_KEY"),
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
        .withDefaultStreamingResponseGenerator(chatModel) // Add this for streaming support
        .build
      
      // Add some example documents
      _ <- ragSystem.addDocument(Document.from(
        "Retrieval-augmented generation (RAG) is an AI framework that combines retrieval-based and generation-based approaches...",
        "RAG Overview"
      ))
      
    yield ragSystem
  
  // Create the HTTP server with WebSocket support
  private def createServer(ragSystem: RAGSystem): ZIO[Any, Throwable, Nothing] =
    // Create the WebSocket handler
    val handler = new RAGWebSocketHandler(ragSystem)
    
    // Create the HTTP app with WebSocket endpoints
    val httpApp = handler.webSocketApp.withDefaultErrorResponse
    
    // Start the server
    Server.serve(httpApp)
      .provide(Server.defaultWithPort(8080))
  
  // Main entry point
  override def run: ZIO[Any, Throwable, Nothing] =
    for
      _ <- Console.printLine("Starting RAG WebSocket server...")
      ragSystem <- createRAGSystem
      _ <- Console.printLine("RAG system created, starting server on port 8080...")
      server <- createServer(ragSystem)
    yield server
```

## Error Handling

The RAG WebSocket system provides comprehensive error handling through the `ErrorMessage` type. Common errors include:

1. **Query Errors**
   - Invalid query format
   - Empty query
   - Query processing failures

2. **Document Addition Errors**
   - Invalid document format
   - Document processing failures
   - Storage failures

3. **Connection Errors**
   - Connection timeouts
   - Authentication failures
   - Rate limiting

### Client-Side Error Handling

```javascript
// JavaScript example
socket.onmessage = (event) => {
  const message = JSON.parse(event.data);
  
  if (message.messageType === 'error') {
    console.error(`Error: ${message.error}`);
    
    if (message.code) {
      // Handle specific error codes
      switch (message.code) {
        case 'QUERY_EMPTY':
          console.error('Query cannot be empty');
          break;
        case 'DOC_PROCESSING_FAILED':
          console.error('Document processing failed');
          break;
        default:
          console.error(`Unknown error code: ${message.code}`);
      }
    }
    
    // Implement retry logic if appropriate
    if (isRetryableError(message)) {
      retryOperation();
    }
  }
};

function isRetryableError(errorMessage) {
  // Define which errors are retryable
  const retryableCodes = ['TIMEOUT', 'RATE_LIMIT', 'TEMPORARY_FAILURE'];
  return errorMessage.code && retryableCodes.includes(errorMessage.code);
}
```

### Server-Side Error Handling

The server handles errors by converting exceptions to appropriate `ErrorMessage` responses:

```scala
// Inside RAGWebSocketHandler
private def handleClientMessage(message: WebSocketMessage): ZIO[Any, Throwable, WebSocketMessage] = {
  message match {
    case queryRequest: ClientMessage.QueryRequest =>
      // Process a regular query
      ragSystem.query(queryRequest.query, queryRequest.maxResults)
        .map(response => ServerMessage.QueryResponse(response))
        .catchAll(error => ZIO.succeed(ServerMessage.ErrorMessage(
          s"Query error: ${error.message}",
          Some(errorCodeFromRAGError(error))
        )))
    
    // Other message types...
  }
}

private def errorCodeFromRAGError(error: RAGError): String = 
  error match {
    case RAGError.GenerationError(_, _) => "GENERATION_FAILED"
    case RAGError.RetrievalError(_, _) => "RETRIEVAL_FAILED"
    case RAGError.DocumentProcessingError(_, _) => "DOC_PROCESSING_FAILED"
    // Other error types...
  }
```

## Best Practices

### Connection Management

1. **Connection Pooling**: For high-traffic applications, implement connection pooling to manage multiple client connections efficiently.

2. **Reconnection Strategy**: Implement an exponential backoff strategy for reconnecting after connection failures:

   ```javascript
   function connectWithRetry() {
     let retryCount = 0;
     const maxRetries = 5;
     
     function connect() {
       const socket = new WebSocket('ws://localhost:8080/rag/ws');
       
       socket.onopen = () => {
         console.log('Connection established');
         retryCount = 0;
       };
       
       socket.onclose = (event) => {
         if (retryCount < maxRetries) {
           const delay = Math.pow(2, retryCount) * 1000; // Exponential backoff
           retryCount++;
           console.log(`Connection closed. Retrying in ${delay}ms...`);
           setTimeout(connect, delay);
         } else {
           console.error('Max retries reached. Connection failed.');
         }
       };
       
       return socket;
     }
     
     return connect();
   }
   
   const socket = connectWithRetry();
   ```

3. **Heartbeat Mechanism**: Implement a heartbeat to detect and recover from zombie connections:

   ```javascript
   // Client-side heartbeat
   function setupHeartbeat(socket, intervalMs = 30000) {
     const heartbeatInterval = setInterval(() => {
       if (socket.readyState === WebSocket.OPEN) {
         socket.send(JSON.stringify({ messageType: 'heartbeat' }));
       } else {
         clearInterval(heartbeatInterval);
       }
     }, intervalMs);
     
     socket.addEventListener('close', () => {
       clearInterval(heartbeatInterval);
     });
   }
   ```

### Performance Optimization

1. **Message Batching**: For multiple small operations, batch them into a single message when possible.

2. **Compression**: Enable WebSocket compression for large messages:

   ```javascript
   // Client-side
   const socket = new WebSocket('ws://localhost:8080/rag/ws', {
     perMessageDeflate: true
   });
   ```

3. **Streaming Thresholds**: Use streaming for long responses and regular WebSockets for short responses:

   ```javascript
   function sendQuery(query, maxResults = 5) {
     // Estimate if the response will be large
     const isLikelyLargeResponse = query.length > 100 || isComplexQuery(query);
     
     if (isLikelyLargeResponse) {
       // Use streaming endpoint
       return connectToStreamingEndpoint(query);
     } else {
       // Use regular endpoint
       const queryRequest = {
         messageType: 'query_request',
         query: query,
         maxResults: maxResults,
         streamResponse: false
       };
       
       socket.send(JSON.stringify(queryRequest));
     }
   }
   ```

### Security Considerations

1. **Authentication**: Implement authentication for WebSocket connections:

   ```javascript
   // Client-side with authentication token
   const socket = new WebSocket(`ws://localhost:8080/rag/ws?token=${authToken}`);
   ```

2. **Input Validation**: Always validate input on both client and server sides:

   ```javascript
   function validateQuery(query) {
     if (!query || query.trim().length === 0) {
       throw new Error('Query cannot be empty');
     }
     
     if (query.length > 1000) {
       throw new Error('Query too long (max 1000 characters)');
     }
     
     // Check for other validation rules
     return query;
   }
   ```

3. **Rate Limiting**: Implement rate limiting to prevent abuse:

   ```scala
   // Server-side rate limiting example
   private val rateLimiter = RateLimiter.create(10) // 10 requests per second
   
   private def handleClientMessage(message: WebSocketMessage): ZIO[Any, Throwable, WebSocketMessage] = {
     if (!rateLimiter.tryAcquire()) {
       return ZIO.succeed(ServerMessage.ErrorMessage(
         "Rate limit exceeded. Please try again later.",
         Some("RATE_LIMIT_EXCEEDED")
       ))
     }
     
     // Process message normally...
   }
   ```

## Troubleshooting

### Common Issues and Solutions

1. **Connection Refused**
   - **Symptom**: WebSocket connection fails with "Connection refused"
   - **Possible Causes**: Server not running, incorrect host/port, firewall blocking
   - **Solution**: Verify server is running, check URL, check firewall settings

2. **Unexpected Close**
   - **Symptom**: WebSocket connection closes unexpectedly
   - **Possible Causes**: Server timeout, network issues, server error
   - **Solution**: Implement reconnection logic, check server logs, check network

3. **Message Parsing Errors**
   - **Symptom**: Error when parsing received messages
   - **Possible Causes**: Incorrect message format, protocol mismatch
   - **Solution**: Verify message format matches protocol, check for protocol version mismatch

4. **Streaming Timeout**
   - **Symptom**: Streaming response stops before completion
   - **Possible Causes**: Network issues, server processing time, client timeout
   - **Solution**: Increase timeout settings, implement chunking with progress indicators

### Debugging Tips

1. **Enable WebSocket Logging**:

   ```javascript
   // Client-side logging
   socket.onmessage = (event) => {
     console.log('Raw message received:', event.data);
     // Normal message processing...
   };
   ```

2. **Use Browser DevTools**:
   - Chrome/Firefox Network tab shows WebSocket frames
   - Filter by "WS" to see only WebSocket traffic
   - Inspect message contents and timing

3. **Server-Side Logging**:

   ```scala
   // Add logging to RAGWebSocketHandler
   private def handleClientMessage(message: WebSocketMessage): ZIO[Any, Throwable, WebSocketMessage] = {
     ZIO.logDebug(s"Received message: ${message.messageType}") *>
     // Normal message processing...
   }
   ```

## Conclusion

The RAG WebSocket interface provides a powerful, real-time communication channel between clients and the RAG system. By following the guidelines in this document, you can effectively implement clients that leverage both standard and streaming interactions with the RAG system.

For further assistance or to report issues, please contact the development team or open an issue in the project repository.