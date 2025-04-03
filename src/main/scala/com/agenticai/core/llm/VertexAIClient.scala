package com.agenticai.core.llm

import zio._
import zio.stream._
import com.google.cloud.aiplatform.v1._
import com.google.api.gax.core.FixedCredentialsProvider
import com.google.auth.oauth2.GoogleCredentials
import com.google.protobuf.{Value, Struct}
import scala.jdk.CollectionConverters._

/**
 * Client for interacting with Google Vertex AI
 */
class VertexAIClient(config: VertexAIConfig) {
  
  private lazy val credentials = GoogleCredentials.getApplicationDefault()
  private lazy val locationName = LocationName.of(config.projectId, config.location)
  private lazy val settings = PredictionServiceSettings.newBuilder()
    .setCredentialsProvider(FixedCredentialsProvider.create(credentials))
    .build()
  private lazy val client = PredictionServiceClient.create(settings)
  
  def complete(prompt: String): ZIO[Any, Throwable, String] = {
    ZIO.attempt {
      val instance = Struct.newBuilder()
        .putFields("prompt", Value.newBuilder().setStringValue(prompt).build())
        .putFields("max_tokens", Value.newBuilder().setNumberValue(config.maxOutputTokens).build())
        .putFields("temperature", Value.newBuilder().setNumberValue(config.temperature).build())
        .putFields("top_p", Value.newBuilder().setNumberValue(config.topP).build())
        .putFields("top_k", Value.newBuilder().setNumberValue(config.topK).build())
        .build()
      
      val request = PredictRequest.newBuilder()
        .setEndpoint(s"projects/${config.projectId}/locations/${config.location}/publishers/${config.publisher}/models/${config.modelId}")
        .addInstances(Value.newBuilder().setStructValue(instance).build())
        .build()
      
      val response = client.predict(request)
      response.getPredictionsList.asScala.headOption
        .map(_.getStructValue.getFieldsMap.get("content").getStringValue)
        .getOrElse(throw new RuntimeException("No response from model"))
    }
  }
  
  def streamCompletion(prompt: String): ZStream[Any, Throwable, String] = {
    ZStream.fromZIO(complete(prompt))
      .flatMap(response => ZStream.fromIterable(response.split("(?<=\\s)|(?=\\s)")))
  }
}

object VertexAIClient {
  def create(config: VertexAIConfig): ZIO[Any, Throwable, VertexAIClient] = {
    ZIO.succeed(new VertexAIClient(config))
  }
} 