# RAG System WebSocket Support

This document describes the WebSocket support added to the RAG (Retrieval Augmented Generation) system. WebSockets enable real-time streaming of responses from LLMs, providing a more responsive user experience.

## Overview

The WebSocket implementation for the RAG system enables:

1. **Streaming Responses**: Generate text responses in real-time as chunks become available
2. **Document Addition**: Add documents to the RAG system via WebSockets
3. **Error Handling**: Proper error communication and management

## Key Components

### 1. StreamingResponseGenerator

The `StreamingResponseGenerator` is an extension of the response generation capability that produces a stream of text chunks:

```scala
trait StreamingResponseGenerator:
  /**
   * Generate a streaming response based on the provided context and query.
   */
  def generateStreamingResponse(context: String, query: String): ZStream[Any, RAGError.GenerationError, String]
```

The default implementation (`DefaultStreamingResponseGenerator`) uses a `ZIOChatLanguageModel` to generate streaming responses.

### 2. WebSocket Protocol

The WebSocket communication protocol is defined in `WebSocketProtocol`, which specifies message formats for both client-to-server and server-to-client communication:

```scala
// Client messages
ClientMessage.QueryRequest        // Request to query the RAG system
ClientMessage.AddDocumentRequest  // Request to add a document

// Server messages
ServerMessage.QueryResponse       // Complete response (non-streaming)
ServerMessage.QueryResponseChunk  // Chunk of a streaming response
ServerMessage.AddDocumentResponse // Response to document addition
ServerMessage.ErrorMessage        // Error notification
```

### 3. RAG WebSocket Handler

The `RAGWebSocketHandler` manages WebSocket connections and routes messages to the appropriate handlers:

- `/rag/ws`: Main WebSocket endpoint for non-streaming requests
- `/rag/ws/stream?query=...`: Endpoint dedicated to streaming responses

## Usage Examples

### Client-Side Usage

#### Regular Query Example

```javascript
// Connect to the WebSocket
const socket = new WebSocket('ws://localhost:8080/rag/ws');

// When connection opens, send a query
socket.onopen = () => {
  const message = {
    messageType: 'query_request',
    query: 'What is retrieval-augmented generation?',
    maxResults: 3,
    streamResponse: false
  };
  socket.send(JSON.stringify(message));
};

// Handle the response
socket.onmessage = (event) => {
  const response = JSON.parse(event.data);
  console.log('Response:', response);
};
```

#### Streaming Query Example

```javascript
// Connect to the streaming endpoint with the query as a parameter
const query = 'What is retrieval-augmented generation?';
const socket = new WebSocket(`ws://localhost:8080/rag/ws/stream?query=${encodeURIComponent(query)}`);

// Accumulate the response
let fullResponse = '';

// Handle incoming message chunks
socket.onmessage = (event) => {
  const chunk = JSON.parse(event.data);
  
  if (chunk.messageType === 'query_response_chunk') {
    if (chunk.isComplete) {
      console.log('Stream complete');
      console.log('Full response:', fullResponse);
    } else {
      // Append to the accumulated response
      fullResponse += chunk.chunk;
      // Display progress in real-time
      console.log('Chunk received:', chunk.chunk);
    }
  } else if (chunk.messageType === 'error') {
    console.error('Error:', chunk.error);
  }
};
```

### Server-Side Configuration

To enable WebSocket support in your RAG system:

1. Create a streaming-capable RAG system:

```scala
val ragSystem = RAGSystem.builder
  .withVectorStore(vectorStore)
  .withEmbeddingModel(embeddingModel)
  .withDefaultDocumentChunker()
  .withDefaultRetriever()
  .withDefaultContextBuilder()
  .withDefaultResponseGenerator(chatModel)
  .withDefaultStreamingResponseGenerator(chatModel) // Add this line for streaming support
  .build
```

2. Create and expose the WebSocket handler:

```scala
val handler = new RAGWebSocketHandler(ragSystem)
val httpApp = handler.webSocketApp.withDefaultErrorResponse
  
Server.serve(httpApp.withDefaultErrorResponse)
```

## Implementation Details

### Streaming Response Flow

1. Client connects to the streaming endpoint with a query
2. Server uses `RAGSystem.queryStream` to generate a stream of response chunks
3. Each chunk is sent as a `QueryResponseChunk` message as it becomes available
4. A final chunk with `isComplete = true` signals the end of the stream

### Error Handling

Errors are communicated as `ErrorMessage` objects with:
- `error`: Descriptive error message
- `code`: Optional error code for programmatic handling

## Testing

The WebSocket functionality includes comprehensive tests:
- Unit tests for the `StreamingResponseGenerator`
- Integration tests for the `RAGWebSocketHandler`
- Example clients demonstrating usage patterns

## Performance Considerations

- Streaming responses reduce time-to-first-token and improve perceived responsiveness
- WebSockets maintain a persistent connection, reducing overhead for multiple interactions
- For very large documents or high-traffic scenarios, consider connection pooling

## Future Enhancements

Potential improvements to consider:
- Progress tracking for document processing
- Connection authentication and authorization
- Client libraries for common frameworks
- Custom streaming parameters (chunk size, etc.)