package com.agenticai.mesh.server

import zio._
import zio.http._
import com.agenticai.mesh.protocol._
import com.agenticai.core.agent.Agent
import java.util.UUID

/**
 * HTTP server for the agent mesh network.
 */
trait HttpServer {
  /**
   * Start the server.
   *
   * @return Server fiber that can be interrupted to stop the server
   */
  def start: Task[Fiber[Throwable, Nothing]]
  
  /**
   * Get the server's local location.
   *
   * @return Server location
   */
  def location: AgentLocation
}

object HttpServer {
  /**
   * Create a new HTTP server for agent mesh.
   *
   * @param port Port to listen on
   * @param serialization Serialization for agent protocol
   * @return HTTP server instance
   */
  def apply(port: Int, serialization: Serialization): HttpServer = 
    new HttpServerImpl(port, serialization)
  
  /**
   * Implementation of HTTP server.
   */
  private class HttpServerImpl(port: Int, serialization: Serialization) extends HttpServer {
    // Store registered agents
    private val agents = scala.collection.concurrent.TrieMap[UUID, Agent[_, _]]()
    
    // HTTP routes for the server
    private val routes = Http.collectZIO[Request] {
      // Deploy a new agent
      case req @ Method.POST -> Root / "agents" =>
        for {
          message <- req.body.asArray.map(MessageEnvelope.create(MessageEnvelope.AGENT_DEPLOY, _))
          agentId = UUID.randomUUID()
          agentBytes = message.payload
          inputType = message.getMetadata("inputType").getOrElse("java.lang.Object")
          outputType = message.getMetadata("outputType").getOrElse("java.lang.Object")
          
          agent <- serialization.deserializeAgent(agentBytes)
          _ <- ZIO.succeed(agents.put(agentId, agent))
          
          responseMap = Map(
            "agentId" -> agentId.toString,
            "inputType" -> inputType,
            "outputType" -> outputType,
            "location" -> location.uri
          )
          
          response = Response.json(
            zio.json.DeriveJsonEncoder.gen[Map[String, String]].encodeJson(responseMap, None).toString
          )
        } yield response
      
      // Get agent by ID
      case Method.GET -> Root / "agents" / UUIDSegment(agentId) =>
        ZIO.succeed(agents.get(agentId)) flatMap {
          case Some(_) =>
            // Just confirm it exists - we don't actually transfer the agent
            Response.json(s"""{"exists": true, "agentId": "$agentId"}""").succeed
          case None =>
            Response.notFound.succeed
        }
      
      // Call an agent
      case req @ Method.POST -> Root / "agents" / UUIDSegment(agentId) / "call" =>
        for {
          message <- req.body.asArray.map(MessageEnvelope.create(MessageEnvelope.AGENT_CALL, _))
          
          maybeAgent <- ZIO.succeed(agents.get(agentId))
          agent <- ZIO.fromOption(maybeAgent).orElseFail(
            new NoSuchElementException(s"Agent not found with ID: $agentId")
          )
          
          // Deserialize input
          input <- serialization.deserialize(message.payload)
          
          // Process with the agent
          output <- agent.asInstanceOf[Agent[Any, Any]].process(input)
          
          // Serialize output
          serializedOutput <- serialization.serialize(output)
          
          // Create response
          responseMessage = MessageEnvelope.create(
            MessageEnvelope.AGENT_RESPONSE,
            serializedOutput,
            Map("agentId" -> agentId.toString)
          )
          
          response = Response.fromArray(responseMessage.payload)
        } yield response
        
      // Delete an agent
      case Method.DELETE -> Root / "agents" / UUIDSegment(agentId) =>
        ZIO.succeed {
          agents.remove(agentId)
          Response.ok
        }
        
      // Default handler for other cases
      case _ =>
        Response.notFound.succeed
    }
    
    private val httpApp = routes.withDefaultErrorResponse
    
    def start: Task[Fiber[Throwable, Nothing]] = {
      Server.install(httpApp.withDefaultErrorResponse).flatMap { port =>
        Console.printLine(s"Server started on port $port").ignore *>
          ZIO.never
      }.fork
    }
    
    def location: AgentLocation = AgentLocation.local(port)
  }
  
  /**
   * UUID path segment extractor.
   */
  private object UUIDSegment {
    def unapply(str: String): Option[UUID] = 
      try Some(UUID.fromString(str)) 
      catch { case _: Exception => None }
  }
}