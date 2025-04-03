package com.agenticai.demo

import zio.*
import zio.stream.*
import com.google.cloud.aiplatform.v1.{
  PredictRequest,
  PredictionServiceClient,
  PredictionServiceSettings,
  LocationName
}
import com.google.api.gax.core.FixedCredentialsProvider
import com.google.auth.oauth2.GoogleCredentials
import com.google.protobuf.{Value, Struct}

import scala.jdk.CollectionConverters.*

/**
 * A completely standalone demo for Claude 3.7 via Vertex AI
 * This file has no dependencies on other parts of the framework
 * and can be used to test basic connectivity
 */
case class Config(
  projectId: String,
  modelName: String = "claude-3-sonnet-20240229-v1p0",
  publisher: String = "anthropic",
  temperature: Double = 0.2,
  maxOutputTokens: Int = 1024,
  topP: Double = 0.8,
  topK: Int = 40
)

object VertexAIClaudeDemoStandalone extends ZIOAppDefault {
  override def run = {
    // Configure your Google Cloud Project here
    val config = Config(
      projectId = "your-gcp-project-id" // Replace with your GCP Project ID
    )

    // Run the example
    for {
      _ <- ZIO.logInfo("Starting Claude 3.7 example")
      _ <- ZIO.logInfo(s"Project ID: ${config.projectId}")
      _ <- ZIO.logInfo(s"Model: ${config.publisher}/${config.modelName}")

      // Create client
      credentials <- ZIO.attempt(GoogleCredentials.getApplicationDefault())
      locationName <- ZIO.attempt(LocationName.of(config.projectId, "us-central1"))
      settings <- ZIO.attempt(
        PredictionServiceSettings.newBuilder()
          .setCredentialsProvider(FixedCredentialsProvider.create(credentials))
          .build()
      )
      client <- ZIO.attempt(PredictionServiceClient.create(settings))

      // Generate text
      prompt = "What is the meaning of life?"
      _ <- ZIO.logInfo(s"Sending prompt: $prompt")
      response <- generateText(client, config, prompt)
      _ <- ZIO.logInfo(s"Response: $response")

    } yield ()
  }

  private def generateText(client: PredictionServiceClient, config: Config, prompt: String): Task[String] = {
    ZIO.attempt {
      val modelEndpoint = s"projects/${config.projectId}/locations/us-central1/publishers/${config.publisher}/models/${config.modelName}"

      val instanceValue = Value.newBuilder()
        .setStructValue(
          Struct.newBuilder()
            .putFields("prompt", Value.newBuilder().setStringValue(prompt).build())
            .putFields("max_tokens", Value.newBuilder().setNumberValue(config.maxOutputTokens).build())
            .putFields("temperature", Value.newBuilder().setNumberValue(config.temperature).build())
            .putFields("top_p", Value.newBuilder().setNumberValue(config.topP).build())
            .putFields("top_k", Value.newBuilder().setNumberValue(config.topK).build())
            .build()
        )
        .build()

      val request = PredictRequest.newBuilder()
        .setEndpoint(modelEndpoint)
        .addInstances(instanceValue)
        .build()

      val response = client.predict(request)
      response.getPredictionsList.asScala.map(_.getStructValue.getFieldsMap.get("text").getStringValue).mkString
    }
  }
}