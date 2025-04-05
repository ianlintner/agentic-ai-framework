package com.agenticai.core.llm.langchain

import dev.langchain4j.data.message.{ChatMessage, UserMessage}
import zio._
import zio.stream._

/**
 * The main agent interface that defines the core interactions with an LLM.
 */
trait Agent {
  /**
   * Process a user input and return a response stream
   *
   * @param input The user input text
   * @return A stream of response tokens
   */
  def process(input: String): ZStream[Any, Throwable, String]
  
  /**
   * Process a user input and return a complete response
   *
   * @param input The user input text
   * @return A ZIO effect that resolves to the complete response text
   */
  def processSync(input: String): ZIO[Any, Throwable, String]
  
  /**
   * Clear the conversation history
   *
   * @return A ZIO effect that completes when the history is cleared
   */
  def clearHistory: ZIO[Any, Throwable, Unit]
  
  /**
   * Get the current conversation history
   *
   * @return A ZIO effect that resolves to the list of messages in the conversation
   */
  def getHistory: ZIO[Any, Throwable, List[ChatMessage]]
}

/**
 * Implementation of the Agent interface using Langchain4j
 *
 * @param chatModel The ZIO-wrapped chat model to use
 * @param memory The ZIO-wrapped memory system to use
 * @param name A name for the agent (useful for logging)
 */
final case class LangchainAgent(
  chatModel: ZIOChatLanguageModel,
  memory: ZIOChatMemory,
  name: String
) extends Agent {
  override def process(input: String): ZStream[Any, Throwable, String] = {
    // First, add the user message to memory
    val setup = for {
      _ <- memory.addUserMessage(input)
      messages <- memory.messages
      responseRef <- Ref.make("")
    } yield (messages, responseRef)
    
    // Create a stream from the setup
    ZStream.fromZIO(setup).flatMap { case (messages, responseRef) =>
      // Stream the response from the model
      val responseStream = chatModel.generateStream(messages)
        // Update the accumulated response with each chunk
        .tap(chunk => responseRef.update(_ + chunk))
      
      // When the stream completes, save the full response to memory
      responseStream.ensuring(
        responseRef.get.flatMap(fullResponse => 
          memory.addAssistantMessage(fullResponse).orDie
        )
      )
    }
  }
  
  override def processSync(input: String): ZIO[Any, Throwable, String] = {
    for {
      _ <- memory.addUserMessage(input)
      messages <- memory.messages
      response <- chatModel.generate(messages)
      responseText = response.text()
      _ <- memory.addAssistantMessage(responseText)
    } yield responseText
  }
  
  override def clearHistory: ZIO[Any, Throwable, Unit] = 
    memory.clear
    
  override def getHistory: ZIO[Any, Throwable, List[ChatMessage]] = 
    memory.messages
}

object LangchainAgent {
  import ZIOChatModelFactory.{ModelType, ModelConfig}
  
  /**
   * Creates an agent using the specified model type and configuration
   *
   * @param modelType The type of model to use (Claude, VertexAI, etc.)
   * @param config The configuration for the model
   * @param name A name for the agent
   * @param maxHistory The maximum number of conversation turns to keep in memory
   * @return A ZIO effect that resolves to an Agent
   */
  def make(
    modelType: ModelType,
    config: ModelConfig,
    name: String = "agent",
    maxHistory: Int = 10
  ): ZIO[Any, Throwable, Agent] = {
    for {
      model <- ZIOChatModelFactory.makeModel(modelType, config)
      memory <- ZIOChatMemory.createWindow(maxHistory * 2) // User + Assistant messages per turn
    } yield LangchainAgent(model, memory, name)
  }
  
  /**
   * Creates a ZLayer for an agent
   *
   * @param modelType The type of model to use
   * @param config The configuration for the model
   * @param name A name for the agent
   * @param maxHistory The maximum number of conversation turns to keep in memory
   * @return A ZLayer that provides an Agent
   */
  def layer(
    modelType: ModelType,
    config: ModelConfig,
    name: String = "agent",
    maxHistory: Int = 10
  ): ZLayer[Any, Throwable, Agent] = {
    ZLayer.fromZIO(make(modelType, config, name, maxHistory))
  }
}
