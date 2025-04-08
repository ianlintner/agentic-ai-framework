package com.agenticai.mesh.protocol

import zio._
import zio.http._
import com.agenticai.core.agent.Agent
import java.util.UUID
import scala.util.{Success, Failure}

/**
 * HTTP-based implementation of the mesh protocol.
 * 
 * This protocol implementation enables communication between nodes in the mesh
 * using HTTP as the transport protocol.
 */
class HttpProtocol(serialization: Serialization) extends Protocol {
  private val client = HttpClient.default

  /**
   * Send an agent to a remote location.
   *
   * @param agent Agent to send
   * @param destination Destination location
   * @return A reference to the remote agent
   */
  def sendAgent[I, O](
    agent: Agent[I, O], 
    destination: AgentLocation
  ): Task[RemoteAgentRef[I, O]] = {
    for {
      // Serialize the agent
      serializedAgent <- serialization.serializeAgent(agent)
      
      // Create unique ID for the agent
      id = UUID.randomUUID()
      
      // Get type information
      inputType = serialization.getTypeName[I]
      outputType = serialization.getTypeName[O]
      
      // Create message with agent payload
      message = MessageEnvelope.create(
        MessageEnvelope.AGENT_DEPLOY,
        serializedAgent,
        Map(
          "inputType" -> inputType,
          "outputType" -> outputType
        )
      )
      
      // Create HTTP request
      request = Request.post(
        url = URL.decode(s"${destination.uri}/agents").getOrElse(
          throw new IllegalArgumentException(s"Invalid URL: ${destination.uri}")
        ),
        body = Body.fromArray(message.payload)
      ).addHeader(Header.ContentType(MediaType.application.`octet-stream`))
      
      // Send request
      response <- client.request(request)
      
      // Handle response
      result <- response.status match {
        case Status.Ok =>
          response.body.asString.flatMap { body =>
            ZIO.attempt {
              // Parse the JSON response
              val json = zio.json.JsonDecoder.decode(body)
              
              // Extract agent ID and location from response
              val agentId = UUID.fromString(json.get("agentId").asString)
              
              // Create remote agent reference
              RemoteAgentRef[I, O](
                agentId,
                destination,
                inputType,
                outputType
              )
            }
          }
        case status =>
          ZIO.fail(new RuntimeException(s"Failed to deploy agent: ${status.code} ${response.status.reason}"))
      }
    } yield result
  }
  
  /**
   * Call a remote agent with the given input.
   *
   * @param ref Remote agent reference
   * @param input Input value
   * @return Output from the remote agent
   */
  def callRemoteAgent[I, O](
    ref: RemoteAgentRef[I, O], 
    input: I
  ): Task[O] = {
    for {
      // Serialize the input
      serializedInput <- serialization.serialize(input)
      
      // Create message with input payload
      message = MessageEnvelope.create(
        MessageEnvelope.AGENT_CALL,
        serializedInput,
        Map(
          "agentId" -> ref.id.toString,
          "inputType" -> ref.inputType,
          "outputType" -> ref.outputType
        )
      )
      
      // Create HTTP request
      request = Request.post(
        url = URL.decode(s"${ref.location.uri}/agents/${ref.id}/call").getOrElse(
          throw new IllegalArgumentException(s"Invalid URL: ${ref.location.uri}/agents/${ref.id}/call")
        ),
        body = Body.fromArray(message.payload)
      ).addHeader(Header.ContentType(MediaType.application.`octet-stream`))
      
      // Send request
      response <- client.request(request)
      
      // Handle response
      result <- response.status match {
        case Status.Ok =>
          response.body.asArray.flatMap { bytes =>
            // Create response message
            val responseMessage = MessageEnvelope.create(
              MessageEnvelope.AGENT_RESPONSE, 
              bytes,
              Map("agentId" -> ref.id.toString)
            )
            
            // Deserialize the output
            serialization.deserialize[O](responseMessage.payload)
          }
        case Status.NotFound =>
          ZIO.fail(new NoSuchElementException(s"Agent not found: ${ref.id}"))
        case status =>
          ZIO.fail(new RuntimeException(s"Failed to call agent: ${status.code} ${response.status.reason}"))
      }
    } yield result
  }
  
  /**
   * Get an agent from a remote location.
   *
   * @param ref Remote agent reference
   * @return The agent if available
   */
  def getRemoteAgent[I, O](
    ref: RemoteAgentRef[I, O]
  ): Task[Option[Agent[I, O]]] = {
    // Create HTTP request
    val request = Request.get(
      url = URL.decode(s"${ref.location.uri}/agents/${ref.id}").getOrElse(
        throw new IllegalArgumentException(s"Invalid URL: ${ref.location.uri}/agents/${ref.id}")
      )
    )
    
    // Send request
    client.request(request).flatMap { response =>
      response.status match {
        case Status.Ok =>
          // Agent exists, but we don't actually transfer it with the HTTP protocol
          // That's an implementation choice - we could retrieve the agent bytes if needed
          ZIO.succeed(None)
        case Status.NotFound =>
          ZIO.succeed(None)
        case status =>
          ZIO.fail(new RuntimeException(s"Failed to get agent: ${status.code} ${response.status.reason}"))
      }
    }
  }
  
  /**
   * Send a message to a location and await a response.
   *
   * @param location Destination location
   * @param message Message to send
   * @return Response message
   */
  def sendAndReceive(
    location: AgentLocation,
    message: MessageEnvelope
  ): Task[MessageEnvelope] = {
    // Create HTTP request
    val request = Request.post(
      url = URL.decode(s"${location.uri}/messages").getOrElse(
        throw new IllegalArgumentException(s"Invalid URL: ${location.uri}/messages")
      ),
      body = Body.fromArray(message.payload)
    )
    .addHeader(Header.ContentType(MediaType.application.`octet-stream`))
    .addHeader("X-Message-Type", message.messageType)
    
    // Add metadata as headers
    val requestWithMetadata = message.metadata.foldLeft(request) { case (req, (key, value)) =>
      req.addHeader(s"X-Metadata-$key", value)
    }
    
    // Send request
    client.request(requestWithMetadata).flatMap { response =>
      response.status match {
        case Status.Ok =>
          response.body.asArray.map { bytes =>
            // Extract message type and metadata from headers
            val messageType = response.header("X-Message-Type").getOrElse("RESPONSE")
            
            // Extract metadata from headers
            val metadataHeaders = response.headers.toList.filter(_._1.startsWith("X-Metadata-"))
            val metadata = metadataHeaders.map { case (key, value) =>
              (key.drop("X-Metadata-".length), value)
            }.toMap
            
            // Create response message
            MessageEnvelope(
              UUID.randomUUID(),
              messageType,
              bytes,
              metadata
            )
          }
        case status =>
          ZIO.fail(new RuntimeException(s"Failed to send message: ${status.code} ${response.status.reason}"))
      }
    }
  }
  
  /**
   * Send a message to a location without awaiting a response.
   *
   * @param location Destination location
   * @param message Message to send
   * @return Confirmation of send
   */
  def send(
    location: AgentLocation,
    message: MessageEnvelope
  ): Task[Unit] = {
    // Create HTTP request
    val request = Request.post(
      url = URL.decode(s"${location.uri}/messages").getOrElse(
        throw new IllegalArgumentException(s"Invalid URL: ${location.uri}/messages")
      ),
      body = Body.fromArray(message.payload)
    )
    .addHeader(Header.ContentType(MediaType.application.`octet-stream`))
    .addHeader("X-Message-Type", message.messageType)
    
    // Add metadata as headers
    val requestWithMetadata = message.metadata.foldLeft(request) { case (req, (key, value)) =>
      req.addHeader(s"X-Metadata-$key", value)
    }
    
    // Send request
    client.request(requestWithMetadata).flatMap { response =>
      response.status match {
        case Status.Ok =>
          ZIO.unit
        case status =>
          ZIO.fail(new RuntimeException(s"Failed to send message: ${status.code} ${response.status.reason}"))
      }
    }
  }
}

object HttpProtocol {
  /**
   * Create a new HTTP protocol with the given serialization.
   *
   * @param serialization Serialization for agent protocol
   * @return HTTP protocol instance
   */
  def apply(serialization: Serialization): Protocol = new HttpProtocol(serialization)
}