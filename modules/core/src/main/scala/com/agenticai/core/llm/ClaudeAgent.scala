package com.agenticai.core.llm

import zio._
import zio.stream._
import com.agenticai.core.memory._
import com.agenticai.core._
import java.util.UUID

/**
 * An Agent implementation that uses the Claude model via Vertex AI
 */
class ClaudeAgent(
  val name: String,
  val client: VertexAIClient,
  val memory: MemorySystem
) extends Agent[String, String] {

  private val agentId = UUID.randomUUID().toString
  private var currentState = AgentState(agentId, AgentStatus.Idle)

  def generateStream(input: String): ZStream[Any, Throwable, String] = {
    client.streamCompletion(input)
  }

  override def process(input: String): ZStream[MemorySystem, Throwable, String] = {
    ZStream.unwrap {
      for {
        _ <- updateStatus(AgentStatus.Processing)
        userCell <- memory.createCell[String]("user_input").orDie
        _ <- userCell.write(input).orDie
        responseCell <- memory.createCell[String]("assistant_response").orDie
        response <- client.complete(input)
        _ <- responseCell.write(response).orDie
        _ <- updateStatus(AgentStatus.Idle)
      } yield ZStream.succeed(response)
    }
  }

  override def processStream(inputs: ZStream[Any, Throwable, String]): ZStream[MemorySystem, Throwable, String] = {
    inputs.flatMap { input =>
      ZStream.unwrap {
        for {
          _ <- updateStatus(AgentStatus.Processing)
          userCell <- memory.createCell[String]("user_input").orDie
          _ <- userCell.write(input).orDie
          responseCell <- memory.createCell[String]("assistant_response").orDie
        } yield {
          client.streamCompletion(input)
            .tap { chunk =>
              responseCell.read.orDie.flatMap { current =>
                responseCell.write(current.getOrElse("") + chunk).orDie
              }
            }
            .concat(ZStream.fromZIO(updateStatus(AgentStatus.Idle)).drain)
        }
      }
    }
  }

  override def state: UIO[AgentState] = ZIO.succeed(currentState)

  override def updateStatus(status: AgentStatus, metadata: Map[String, String] = Map.empty): UIO[Unit] = {
    ZIO.succeed {
      currentState = currentState.copy(status = status, metadata = metadata)
    }
  }
}

object ClaudeAgent {
  /**
   * Create a new Claude agent
   */
  def make(config: VertexAIConfig): RIO[Scope with MemorySystem, Agent[String, String]] = {
    for {
      memory <- ZIO.service[MemorySystem]
      client <- VertexAIClient.make(config)
      agent = new ClaudeAgent("Claude", client, memory)
    } yield agent
  }
}