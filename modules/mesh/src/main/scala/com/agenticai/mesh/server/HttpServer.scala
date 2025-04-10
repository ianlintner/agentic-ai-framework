package com.agenticai.mesh.server

import zio.*
import com.agenticai.mesh.protocol.*
import com.agenticai.core.agent.Agent
import java.util.UUID

/** HTTP server for the agent mesh network.
  */
trait HttpServer:
  /** Start the server.
    *
    * @return
    *   Server fiber that can be interrupted to stop the server
    */
  def start: Task[Fiber[Throwable, Nothing]]

  /** Get the server's local location.
    *
    * @return
    *   Server location
    */
  def location: AgentLocation

object HttpServer:

  /** Create a new HTTP server for agent mesh.
    *
    * @param port
    *   Port to listen on
    * @param serialization
    *   Serialization for agent protocol
    * @return
    *   HTTP server instance
    */
  def apply(port: Int, serialization: Serialization): HttpServer =
    new HttpServerImpl(port, serialization)

  /** Implementation of HTTP server.
    */
  private class HttpServerImpl(port: Int, serialization: Serialization) extends HttpServer:
    // Store registered agents
    private val agents = scala.collection.concurrent.TrieMap[UUID, Agent[_, _]]()

    // This is a minimal implementation that will be replaced when zio-http is properly configured
    def start: Task[Fiber[Throwable, Nothing]] =
      // Just create a dummy fiber that never completes
      Console.printLine(s"Mock HTTP server started on port $port").ignore *>
        ZIO.never.fork

    def location: AgentLocation = AgentLocation.local(port)
