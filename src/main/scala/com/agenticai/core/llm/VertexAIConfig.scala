package com.agenticai.core.llm

/**
 * Configuration for Google Vertex AI client
 */
case class VertexAIConfig(
  projectId: String,
  location: String,
  publisher: String,
  modelId: String,
  maxOutputTokens: Int = 1024,
  temperature: Double = 0.2,
  topP: Double = 0.8,
  topK: Int = 40
)

object VertexAIConfig {
  val claudeDefault = VertexAIConfig(
    projectId = "your-project-id",
    location = "us-central1",
    publisher = "anthropic",
    modelId = "claude-3-sonnet-20240229",
    maxOutputTokens = 1024,
    temperature = 0.2,
    topP = 0.8,
    topK = 40
  )
} 