package com.agenticai.examples

import com.agenticai.core.BaseAgent
import com.agenticai.core.memory.*
import zio.*
import zio.stream.*

/** A simple chat agent that echoes back messages
  */
class ChatAgent extends BaseAgent[String, String]("ChatAgent"):

  override protected def processMessage(input: String): ZStream[Any, Throwable, String] =
    ZStream.succeed(s"Echo: $input")

/** Example runner for the ChatAgent
  */
object ChatAgentExample extends ZIOAppDefault:

  def run =
    val agent = new ChatAgent()

    for
      _ <- ZIO.logInfo("Starting ChatAgent example...")

      // Process a message
      result <- agent
        .process("Hello, world!")
        .runHead
        .someOrFail(new RuntimeException("No result produced"))
        .provideLayer(ZLayer.fromZIO(MemorySystem.make))

      // Display the result
      _ <- ZIO.logInfo(s"Agent response: $result")

      // Get the agent's state
      state <- agent.state
        .provideLayer(ZLayer.fromZIO(MemorySystem.make))

      _ <- ZIO.logInfo(s"Agent state: $state")
    yield ()
