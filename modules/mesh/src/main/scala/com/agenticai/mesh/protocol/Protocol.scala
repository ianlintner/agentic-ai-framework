package com.agenticai.mesh.protocol

import zio._
import com.agenticai.core.agent.Agent
import java.util.UUID

/**
 * Core protocol for agent communication in the mesh.
 */
trait Protocol {
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
  ): Task[RemoteAgentRef[I, O]]
  
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
  ): Task[O]
  
  /**
   * Get an agent from a remote location.
   *
   * @param ref Remote agent reference
   * @return The agent if available
   */
  def getRemoteAgent[I, O](
    ref: RemoteAgentRef[I, O]
  ): Task[Option[Agent[I, O]]]
  
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
  ): Task[MessageEnvelope]
  
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
  ): Task[Unit]
}

object Protocol {
  /**
   * Create an in-memory protocol for testing.
   *
   * @return In-memory protocol
   */
  def inMemory: Protocol = {
    val serialization = Serialization.test
    new InMemoryProtocol(serialization)
  }
  
  /**
   * In-memory implementation of the protocol.
   */
  private class InMemoryProtocol(serialization: Serialization) extends Protocol {
    // In-memory state for agent registry
    private val agents = scala.collection.concurrent.TrieMap[UUID, Agent[_, _]]()
    
    def sendAgent[I, O](
      agent: Agent[I, O], 
      destination: AgentLocation
    ): Task[RemoteAgentRef[I, O]] = {
      for {
        // Create a reference with a new ID
        id <- ZIO.succeed(UUID.randomUUID())
        ref = RemoteAgentRef[I, O](
          id,
          destination,
          agent.getClass.getName + "_Input",
          agent.getClass.getName + "_Output"
        )
        
        // Store the agent in the registry
        _ <- ZIO.succeed(agents.put(id, agent))
      } yield ref
    }
    
    def callRemoteAgent[I, O](
      ref: RemoteAgentRef[I, O], 
      input: I
    ): Task[O] = {
      ZIO.attempt {
        // Find the agent in the registry
        val agent = agents.getOrElse(
          ref.id, 
          throw new NoSuchElementException(s"No agent found with ID ${ref.id}")
        ).asInstanceOf[Agent[I, O]]
        
        // Process the input
        agent.process(input)
      }.flatten
    }
    
    def getRemoteAgent[I, O](
      ref: RemoteAgentRef[I, O]
    ): Task[Option[Agent[I, O]]] = {
      ZIO.succeed {
        agents.get(ref.id).map(_.asInstanceOf[Agent[I, O]])
      }
    }
    
    def sendAndReceive(
      location: AgentLocation,
      message: MessageEnvelope
    ): Task[MessageEnvelope] = {
      message.messageType match {
        case MessageEnvelope.AGENT_CALL =>
          val agentId = UUID.fromString(message.getMetadata("agentId").getOrElse(
            throw new IllegalArgumentException("Missing agentId in message metadata")
          ))
          
          val inputType = message.getMetadata("inputType").getOrElse(
            throw new IllegalArgumentException("Missing inputType in message metadata")
          )
          
          val outputType = message.getMetadata("outputType").getOrElse(
            throw new IllegalArgumentException("Missing outputType in message metadata")
          )
          
          for {
            // Find the agent in the registry
            maybeAgent <- ZIO.succeed(agents.get(agentId))
            agent = maybeAgent.getOrElse(
              throw new NoSuchElementException(s"No agent found with ID $agentId")
            )
            
            // Deserialize the input - using explicit type Any to avoid ClassTag issues
            input <- serialization.deserialize[Any](message.payload)
            
            // Process the input
            output <- agent.asInstanceOf[Agent[Any, Any]].process(input)
            
            // Serialize the output
            serializedOutput <- serialization.serialize(output)
            
            // Create response message
            response = MessageEnvelope(
              UUID.randomUUID(),
              MessageEnvelope.AGENT_RESPONSE,
              serializedOutput,
              Map(
                "agentId" -> agentId.toString,
                "requestId" -> message.id.toString
              )
            )
          } yield response
          
        case MessageEnvelope.AGENT_GET =>
          val agentId = UUID.fromString(message.getMetadata("agentId").getOrElse(
            throw new IllegalArgumentException("Missing agentId in message metadata")
          ))
          
          ZIO.succeed {
            agents.get(agentId) match {
              case Some(agent) =>
                // Create response for found agent
                MessageEnvelope(
                  UUID.randomUUID(),
                  MessageEnvelope.AGENT_FOUND,
                  Array.emptyByteArray, // We don't actually serialize here in the in-memory implementation
                  Map(
                    "agentId" -> agentId.toString,
                    "requestId" -> message.id.toString
                  )
                )
              case None =>
                // Create response for not found
                MessageEnvelope(
                  UUID.randomUUID(),
                  MessageEnvelope.AGENT_NOT_FOUND,
                  Array.emptyByteArray,
                  Map(
                    "agentId" -> agentId.toString,
                    "requestId" -> message.id.toString
                  )
                )
            }
          }
          
        case _ =>
          // Echo back for other messages
          ZIO.succeed(
            MessageEnvelope(
              UUID.randomUUID(),
              "ECHO",
              message.payload,
              message.metadata + ("requestId" -> message.id.toString)
            )
          )
      }
    }
    
    def send(
      location: AgentLocation,
      message: MessageEnvelope
    ): Task[Unit] = {
      message.messageType match {
        case MessageEnvelope.AGENT_DEPLOY =>
          // Just store the agent for in-memory
          // In a real implementation, we would actually send it to the remote location
          ZIO.succeed(())
          
        case _ => ZIO.succeed(())
      }
    }
  }
}