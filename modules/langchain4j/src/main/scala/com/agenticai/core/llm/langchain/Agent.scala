package com.agenticai.core.llm.langchain

import zio.*
import zio.stream.*
import dev.langchain4j.data.message.{AiMessage, ChatMessage, UserMessage}

/** A trait representing an agent that can process user input and generate responses.
  */
trait Agent:
  /** Processes a user input and returns a streaming response.
    *
    * @param input
    *   The user's input
    * @return
    *   A ZStream of response tokens as they are generated
    */
  def process(input: String): ZStream[Any, LangchainError, String]

  /** Processes a user input and returns a complete response.
    *
    * @param input
    *   The user's input
    * @return
    *   A ZIO effect that completes with the response
    */
  def processSync(input: String): ZIO[Any, LangchainError, String]

  /** Gets the name of this agent.
    *
    * @return
    *   The agent's name
    */
  def name: String

  /** Clears the agent's conversation history.
    *
    * @return
    *   A ZIO effect that completes when the history is cleared
    */
  def clearHistory(): ZIO[Any, LangchainError, Unit]

  /** Gets the conversation history.
    *
    * @return
    *   A ZIO effect that completes with the list of messages
    */
  def getHistory: ZIO[Any, LangchainError, List[ChatMessage]]

/** An implementation of Agent that uses Langchain4j and ZIO.
  *
  * @param chatModel
  *   The language model to use
  * @param memory
  *   The memory system to use for conversation history
  * @param agentName
  *   The name of this agent
  */
final case class LangchainAgent(
    chatModel: ZIOChatLanguageModel,
    memory: ZIOChatMemory,
    agentName: String
) extends Agent:

  /** Processes a user input and returns a streaming response.
    *
    * @param input
    *   The user's input
    * @return
    *   A ZStream of response tokens as they are generated
    */
  override def process(input: String): ZStream[Any, LangchainError, String] =
    ZStream.fromZIO(
      // Add the user message to memory
      memory.addUserMessage(input).mapError(LangchainError.fromThrowable) *>
      // Get all messages from memory
      memory.messages.mapError(LangchainError.fromThrowable)
    ).flatMap { messages =>
      // Use the chat model to generate a response
      val responseStream = chatModel.generateStream(messages)
      
      // Collect all response chunks to save as a single message at the end
      val saveResponseEffect = responseStream.runCollect.flatMap { chunks =>
        val fullResponse = chunks.mkString
        memory.addAssistantMessage(fullResponse).mapError(LangchainError.fromThrowable)
      }
      
      // Run the save effect in the background after the stream completes
      ZStream.fromZIO(saveResponseEffect.fork).drain ++
      // Return the original response stream
      responseStream
    }

  /** Processes a user input and returns a complete response.
    *
    * @param input
    *   The user's input
    * @return
    *   A ZIO effect that completes with the response
    */
  override def processSync(input: String): ZIO[Any, LangchainError, String] =
    for
      // Add the user message to memory
      _ <- memory.addUserMessage(input).mapError(LangchainError.fromThrowable)

      // Get all messages from memory
      messages <- memory.messages.mapError(LangchainError.fromThrowable)

      // Use the chat model to generate a response
      response <- chatModel.generate(messages)
      responseText = response.text()

      // Add the assistant message to memory
      _ <- memory.addAssistantMessage(responseText).mapError(LangchainError.fromThrowable)
    yield responseText

  /** Gets the name of this agent.
    *
    * @return
    *   The agent's name
    */
  override def name: String = agentName

  /** Clears the agent's conversation history.
    *
    * @return
    *   A ZIO effect that completes when the history is cleared
    */
  override def clearHistory(): ZIO[Any, LangchainError, Unit] =
    memory.clear().mapError(LangchainError.fromThrowable)

  /** Gets the conversation history.
    *
    * @return
    *   A ZIO effect that completes with the list of messages
    */
  override def getHistory: ZIO[Any, LangchainError, List[ChatMessage]] =
    memory.messages.mapError(LangchainError.fromThrowable)

object LangchainAgent:

  /** Creates a new LangchainAgent with the specified model type and configuration.
    *
    * @param modelType
    *   The type of model to use
    * @param config
    *   The configuration for the model
    * @param name
    *   The name of the agent
    * @param maxHistory
    *   The maximum number of messages to keep in history
    * @return
    *   A ZIO effect that completes with a new Agent
    */
  def make(
      modelType: ZIOChatModelFactory.ModelType,
      config: ZIOChatModelFactory.ModelConfig,
      name: String = "agent",
      maxHistory: Int = 10
  ): ZIO[Any, LangchainError, Agent] =
    for
      model <- ZIOChatModelFactory.makeModel(modelType, config)
      memory <- ZIOChatMemory.createInMemory(
        maxHistory * 2
      ) // Double because we store user + assistant messages
    yield LangchainAgent(model, memory, name)

  /** Creates a new LangchainAgent with the specified model and memory.
    *
    * @param model
    *   The language model to use
    * @param memory
    *   The memory system to use
    * @param name
    *   The name of the agent
    * @return
    *   A new Agent
    */
  def create(
      model: ZIOChatLanguageModel,
      memory: ZIOChatMemory,
      name: String = "agent"
  ): Agent =
    new LangchainAgent(model, memory, name)

  /** Creates a layer that provides an Agent.
    *
    * @param name
    *   The name of the agent
    * @return
    *   A ZLayer that provides an Agent
    */
  def layer(
      name: String = "agent"
  ): ZLayer[ZIOChatLanguageModel with ZIOChatMemory, Nothing, Agent] =
    ZLayer.fromFunction { (model: ZIOChatLanguageModel, memory: ZIOChatMemory) =>
      create(model, memory, name)
    }
