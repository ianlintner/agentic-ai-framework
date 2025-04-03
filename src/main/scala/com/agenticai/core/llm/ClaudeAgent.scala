package com.agenticai.core.llm

import zio._
import zio.stream._
import com.agenticai.core.memory._

/**
 * Agent for interacting with Claude via Vertex AI
 */
class ClaudeAgent(
  val name: String,
  val client: VertexAIClient,
  val memory: MemorySystem
) {
  def process(input: String): ZStream[Any, Throwable, String] = {
    client.streamCompletion(input)
  }
  
  def generateStream(input: String): ZStream[Any, Throwable, String] = {
    client.streamCompletion(input)
  }
} 