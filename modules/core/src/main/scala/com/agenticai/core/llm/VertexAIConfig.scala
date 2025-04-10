package com.agenticai.core.llm

import scala.concurrent.duration.*

/** Configuration for Google Vertex AI client
  */
case class VertexAIConfig(
    projectId: String,
    location: String,
    publisher: String = "anthropic",
    modelId: String,
    maxOutputTokens: Int = 1024,
    temperature: Double = 0.2,
    topP: Double = 0.8,
    topK: Int = 40,
    credentialsPath: Option[String] = None
):
  // Derived property for model name
  def modelName: String = modelId

  // Get the location string in the format used by the API
  def getLocationString: String = s"projects/$projectId/locations/$location"

object VertexAIConfig:

  /** Validate the credentials path
    * @param path
    *   Optional path to credentials file
    * @return
    *   Boolean indicating if the path is valid or None
    */
  def validateCredentialsPath(path: Option[String]): Boolean =
    path match
      case Some(p) =>
        val file = new java.io.File(p)
        file.exists() && file.canRead()
      case None => true

  /** Default configuration for Claude models via Vertex AI
    */
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

  /** Higher throughput configuration with more aggressive rate limiting Use this for batch
    * processing or high-volume scenarios where you have increased quota limits configured in GCP
    */
  val highThroughput = VertexAIConfig(
    projectId = "your-project-id",
    location = "us-central1",
    publisher = "anthropic",
    modelId = "claude-3-sonnet-20240229",
    maxOutputTokens = 1024,
    temperature = 0.2,
    topP = 0.8,
    topK = 40
  )

  /** Low latency configuration for real-time interactive applications Uses Claude Haiku which is
    * optimized for faster responses
    */
  val lowLatency = VertexAIConfig(
    projectId = "your-project-id",
    location = "us-central1",
    publisher = "anthropic",
    modelId = "claude-3-haiku-20240307",
    maxOutputTokens = 512,
    temperature = 0.2,
    topP = 0.8,
    topK = 40
  )
