package com.agenticai.core.llm

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class VertexAIConfigSpec extends AnyFlatSpec with Matchers:

  "VertexAIConfig.claudeDefault" should "have expected values" in {
    val config = VertexAIConfig.claudeDefault
    config.projectId shouldBe "your-project-id"
    config.modelName shouldBe "claude-3-sonnet-20240229"
    config.temperature shouldBe 0.2
    config.maxOutputTokens shouldBe 1024
  }

  "Custom config" should "override defaults" in {
    val custom = VertexAIConfig(
      projectId = "custom-project",
      location = "us-central1",
      modelId = "custom-model",
      temperature = 0.5,
      maxOutputTokens = 2048
    )

    custom.projectId shouldBe "custom-project"
    custom.modelName shouldBe "custom-model"
    custom.temperature shouldBe 0.5
    custom.maxOutputTokens shouldBe 2048
  }

  "getLocationString" should "return valid location string" in {
    val config = VertexAIConfig(
      projectId = "test-project",
      location = "us-central1",
      modelId = "test-model"
    )
    val locationString = config.getLocationString
    locationString shouldBe "projects/test-project/locations/us-central1"
  }

  "validateCredentialsPath" should "return false with invalid path" in {
    val result = VertexAIConfig.validateCredentialsPath(Some("/bad/path"))
    result shouldBe false
  }

  "validateCredentialsPath" should "return true with None" in {
    val result = VertexAIConfig.validateCredentialsPath(None)
    result shouldBe true
  }
