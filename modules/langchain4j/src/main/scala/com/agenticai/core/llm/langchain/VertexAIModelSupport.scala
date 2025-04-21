package com.agenticai.core.llm.langchain

import dev.langchain4j.data.message.{AiMessage, ChatMessage}
import dev.langchain4j.model.chat.*
import zio.*
import zio.stream.ZStream

/** Support for Vertex AI models in the ZIOChatModelFactory.
  */
object VertexAIModelSupport:

  /** Creates a Vertex AI model for Gemini.
    *
    * @param projectId
    *   The Google Cloud project ID
    * @param location
    *   The Google Cloud location (e.g., "us-central1")
    * @param modelName
    *   The model name to use (defaults to "gemini-1.5-pro")
    * @param temperature
    *   The temperature parameter for the model
    * @param maxTokens
    *   The maximum number of tokens to generate
    * @return
    *   A ZIO effect that resolves to a ZIOChatLanguageModel
    */
  def makeVertexAIGeminiModel(
      projectId: String,
      location: String = "us-central1",
      modelName: String = "gemini-1.5-pro",
      temperature: Option[Double] = None,
      maxTokens: Option[Integer] = None
  ): ZIO[Any, LangchainError, ZIOChatLanguageModel] =
    // For now, we'll use a mock implementation
    ZIO.succeed(new MockVertexAIModel(projectId, location, modelName))

  /** Creates a non-streaming Vertex AI model for Gemini.
    *
    * @param projectId
    *   The Google Cloud project ID
    * @param location
    *   The Google Cloud location (e.g., "us-central1")
    * @param modelName
    *   The model name to use (defaults to "gemini-1.5-pro")
    * @param temperature
    *   The temperature parameter for the model
    * @param maxTokens
    *   The maximum number of tokens to generate
    * @return
    *   A ZIO effect that resolves to a ZIOChatLanguageModel
    */
  def makeNonStreamingVertexAIGeminiModel(
      projectId: String,
      location: String = "us-central1",
      modelName: String = "gemini-1.5-pro",
      temperature: Option[Double] = None,
      maxTokens: Option[Integer] = None
  ): ZIO[Any, LangchainError, ZIOChatLanguageModel] =
    // For now, we'll use a mock implementation
    ZIO.succeed(new MockVertexAIModel(projectId, location, modelName + " (Non-streaming)"))

  // A simple mock implementation of ZIOChatLanguageModel
  private class MockVertexAIModel(projectId: String, location: String, modelName: String)
      extends ZIOChatLanguageModel:

    override def generate(messages: List[ChatMessage]): ZIO[Any, LangchainError, AiMessage] =
      ZIO.succeed(
        AiMessage.from(
          s"Mock response from VertexAI Gemini model $modelName in project $projectId at location $location"
        )
      )

    override def generateStream(messages: List[ChatMessage]): ZStream[Any, LangchainError, String] =
      ZStream.succeed(
        s"Mock streaming response from VertexAI Gemini model $modelName in project $projectId at location $location"
      )

  /** Updates the ZIOChatModelFactory to support additional model types. This method adds the
    * necessary logic to the makeModel method to handle VertexAI model types.
    *
    * @param modelType
    *   The type of model to create
    * @param config
    *   The configuration for the model
    * @return
    *   A ZIO effect that resolves to a ZIOChatLanguageModel
    */
  def makeModel(
      modelType: ZIOChatModelFactory.ModelType,
      config: ZIOChatModelFactory.ModelConfig
  ): ZIO[Any, LangchainError, ZIOChatLanguageModel] = modelType match
    case ZIOChatModelFactory.ModelType.VertexAIGemini =>
      (config.projectId, config.location) match
        case (Some(projectId), Some(location)) =>
          makeVertexAIGeminiModel(
            projectId,
            location,
            config.modelName.getOrElse("gemini-1.5-pro"),
            config.temperature,
            config.maxTokens.map(Integer.valueOf)
          )
        case (Some(projectId), None) =>
          makeVertexAIGeminiModel(
            projectId,
            "us-central1",
            config.modelName.getOrElse("gemini-1.5-pro"),
            config.temperature,
            config.maxTokens.map(Integer.valueOf)
          )
        case _ =>
          ZIO.fail(InvalidRequestError("VertexAI Gemini model requires a project ID"))
    case _ => ZIO.fail(InvalidRequestError(s"Unsupported model type: $modelType"))
