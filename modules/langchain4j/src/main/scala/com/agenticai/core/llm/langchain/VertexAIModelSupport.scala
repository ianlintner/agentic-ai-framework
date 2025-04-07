package com.agenticai.core.llm.langchain

import dev.langchain4j.model.vertexai.VertexAiChatModel
import zio._

/**
 * Support for Vertex AI models in the ZIOChatModelFactory.
 */
object VertexAIModelSupport {
  /**
   * Creates a Vertex AI model for Gemini.
   *
   * @param projectId The Google Cloud project ID
   * @param location The Google Cloud location (e.g., "us-central1")
   * @param modelName The model name to use (defaults to "gemini-1.5-pro")
   * @param temperature The temperature parameter for the model
   * @param maxTokens The maximum number of tokens to generate
   * @return A ZIO effect that resolves to a ZIOChatLanguageModel
   */
  def makeVertexAIGeminiModel(
    projectId: String,
    location: String = "us-central1",
    modelName: String = "gemini-1.5-pro",
    temperature: Option[Double] = None,
    maxTokens: Option[Integer] = None
  ): ZIO[Any, Throwable, ZIOChatLanguageModel] = {
    ZIO.attempt {
      // Create a builder with the required parameters
      val builder = VertexAiChatModel.builder()
        .project(projectId)
        .location(location)
        .modelName(modelName)
      
      // Add optional parameters if provided
      temperature.foreach(builder.temperature(_))
      maxTokens.foreach(builder.maxOutputTokens(_))
      
      // Build the model
      val model = builder.build()
      
      // Wrap the model in a ZIOChatLanguageModel
      ZIOChatLanguageModel(model)
    }
  }
  
  /**
   * Updates the ZIOChatModelFactory to support additional model types.
   * This method adds the necessary logic to the makeModel method
   * to handle VertexAI model types.
   *
   * @param modelType The type of model to create
   * @param config The configuration for the model
   * @return A ZIO effect that resolves to a ZIOChatLanguageModel
   */
  def makeModel(
    modelType: ZIOChatModelFactory.ModelType,
    config: ZIOChatModelFactory.ModelConfig
  ): ZIO[Any, Throwable, ZIOChatLanguageModel] = modelType match {
    case ZIOChatModelFactory.ModelType.VertexAIGemini =>
      (config.projectId, config.location) match {
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
          ZIO.fail(new IllegalArgumentException("VertexAI Gemini model requires a project ID"))
      }
    case _ => ZIO.fail(new UnsupportedOperationException(s"Unsupported model type: $modelType"))
  }
}
