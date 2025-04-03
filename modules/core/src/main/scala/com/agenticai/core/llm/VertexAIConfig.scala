package com.agenticai.core.llm

/**
 * Configuration for Google Vertex AI client - simplified version with no external dependencies
 * To use Google Cloud services in production, proper dependencies need to be added to build.sbt
 */
final case class VertexAIConfig(
  projectId: String,
  location: String = "us-central1",
  modelId: String = "claude-3-sonnet-20240229",
  modelName: String = "claude-3-sonnet-20240229-v1p0",
  publisher: String = "anthropic",
  credentialsPath: Option[String] = None,
  maxTokens: Int = 1024,
  maxOutputTokens: Int = 2048,
  temperature: Double = 0.7,
  topP: Double = 0.95,
  topK: Int = 40
)

/**
 * Companion object for VertexAIConfig
 */
object VertexAIConfig {
  // Default Claude 3 Sonnet configuration
  val claudeDefault: VertexAIConfig = VertexAIConfig(
    projectId = "your-project-id",
    modelId = "claude-3-sonnet-20240229"
  )

  // Get location as a formatted string
  def getLocationString(config: VertexAIConfig): String =
    s"projects/${config.projectId}/locations/${config.location}"

  // Simple credential validation that doesn't rely on external libraries
  def validateCredentialsPath(path: Option[String]): Boolean = {
    path match {
      case None => true
      case Some(p) => new java.io.File(p).exists()
    }
  }
}