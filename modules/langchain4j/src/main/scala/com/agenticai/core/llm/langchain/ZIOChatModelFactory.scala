package com.agenticai.core.llm.langchain

import dev.langchain4j.model.anthropic.AnthropicChatModel
import zio._

/**
 * Factory methods for creating various LLM providers through Langchain4j.
 * This object provides convenient ways to create ZIO-wrapped Langchain4j chat models.
 */
object ZIOChatModelFactory {
  /**
   * Creates a Claude model (from Anthropic).
   *
   * @param apiKey The Anthropic API key
   * @param modelName The model name to use (defaults to Claude 3 Opus)
   * @param temperature The temperature parameter for the model
   * @param maxTokens The maximum number of tokens to generate
   * @return A ZIO effect that resolves to a ZIOChatLanguageModel
   */
  def makeClaudeModel(
    apiKey: String, 
    modelName: String = "claude-3-opus-20240229",
    temperature: Option[Double] = None,
    maxTokens: Option[Int] = None
  ): ZIO[Any, Throwable, ZIOChatLanguageModel] = {
    ZIO.attempt {
      // Create a builder with the required parameters
      val builder = AnthropicChatModel.builder()
        .apiKey(apiKey)
        .modelName(modelName)
      
      // Build the model
      val model = builder.build()
      
      // Wrap the model in a ZIOChatLanguageModel
      ZIOChatLanguageModel(model)
    }
  }
  
  // Type definitions for model configurations
  sealed trait ModelType
  
  object ModelType {
    case object Claude extends ModelType
    case object VertexAIGemini extends ModelType
    case object GoogleAIGemini extends ModelType
  }
  
  case class ModelConfig(
    apiKey: Option[String] = None,
    projectId: Option[String] = None,
    location: Option[String] = None,
    modelName: Option[String] = None,
    temperature: Option[Double] = None,
    maxTokens: Option[Int] = None
  )
  
  /**
   * Creates a model based on the provided model type and configuration.
   * This is a convenience method for creating different types of models with a unified interface.
   *
   * @param modelType The type of model to create (Claude, VertexAI, etc.)
   * @param config The configuration for the model
   * @return A ZIO effect that resolves to a ZIOChatLanguageModel
   */
  def makeModel(
    modelType: ModelType,
    config: ModelConfig
  ): ZIO[Any, Throwable, ZIOChatLanguageModel] = {
    modelType match {
      case ModelType.Claude => 
        config.apiKey match {
          case Some(apiKey) => 
            makeClaudeModel(
              apiKey, 
              config.modelName.getOrElse("claude-3-opus-20240229"),
              config.temperature,
              config.maxTokens
            )
          case None => 
            ZIO.fail(new IllegalArgumentException("Claude model requires an API key"))
        }
      
      case ModelType.VertexAIGemini =>
        ZIO.fail(new UnsupportedOperationException("VertexAIGemini model is not yet implemented"))
        
      case ModelType.GoogleAIGemini =>
        ZIO.fail(new UnsupportedOperationException("GoogleAIGemini model is not yet implemented"))
    }
  }
}
