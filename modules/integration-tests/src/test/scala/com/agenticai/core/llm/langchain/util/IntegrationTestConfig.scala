package com.agenticai.core.llm.langchain.util

import com.agenticai.core.llm.langchain.ZIOChatModelFactory

/**
 * Configuration utilities for integration tests.
 * This object provides access to environment variables and helper methods
 * for creating test configurations.
 */
object IntegrationTestConfig {
  // API keys and credentials from environment variables
  val claudeApiKey: Option[String] = sys.env.get("CLAUDE_API_KEY")
  val vertexProjectId: Option[String] = sys.env.get("GOOGLE_CLOUD_PROJECT")
  val vertexLocation: String = sys.env.getOrElse("VERTEX_LOCATION", "us-central1")
  val googleApiKey: Option[String] = sys.env.get("GOOGLE_API_KEY")
  
  // Model names - use smaller/cheaper models for testing
  val claudeModelName: String = sys.env.getOrElse("CLAUDE_MODEL_NAME", "claude-3-haiku-20240307")
  val vertexModelName: String = sys.env.getOrElse("VERTEX_MODEL_NAME", "gemini-1.0-pro")
  val googleModelName: String = sys.env.getOrElse("GOOGLE_MODEL_NAME", "gemini-1.0-pro")
  
  // Helper methods to create test configurations
  def claudeTestConfig: Option[ZIOChatModelFactory.ModelConfig] = 
    claudeApiKey.map(key => ZIOChatModelFactory.ModelConfig(
      apiKey = Some(key),
      modelName = Some(claudeModelName)
    ))
    
  def vertexTestConfig: Option[ZIOChatModelFactory.ModelConfig] =
    vertexProjectId.map(id => ZIOChatModelFactory.ModelConfig(
      projectId = Some(id),
      location = Some(vertexLocation),
      modelName = Some(vertexModelName)
    ))
    
  def googleTestConfig: Option[ZIOChatModelFactory.ModelConfig] =
    googleApiKey.map(key => ZIOChatModelFactory.ModelConfig(
      apiKey = Some(key),
      modelName = Some(googleModelName)
    ))
    
  // Check if a specific integration test should be run
  def shouldRunClaudeTests: Boolean = claudeApiKey.isDefined
  def shouldRunVertexTests: Boolean = vertexProjectId.isDefined
  def shouldRunGoogleTests: Boolean = googleApiKey.isDefined
  
  // Test prompts
  val simplePrompt: String = "What is the capital of France?"
  val countingPrompt: String = "Count from 1 to 5 briefly."
  val reasoningPrompt: String = "If a train travels at 60 mph for 2 hours, how far does it go?"
}
