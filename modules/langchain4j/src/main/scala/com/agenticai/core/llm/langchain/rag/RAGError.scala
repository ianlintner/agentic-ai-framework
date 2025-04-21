package com.agenticai.core.llm.langchain.rag

/**
 * Error types for the RAG system.
 */
sealed trait RAGError extends Throwable:
  /**
   * Get the error message.
   *
   * @return The error message
   */
  def message: String
  
  /**
   * Get the cause of the error, if any.
   *
   * @return The cause of the error
   */
  def cause: Option[Throwable]
  
  override def getMessage: String = message
  override def getCause: Throwable = cause.orNull

object RAGError:
  /**
   * Error that occurs during document processing.
   *
   * @param message The error message
   * @param cause The cause of the error
   */
  case class ProcessingError(message: String, cause: Option[Throwable] = None) extends RAGError
  
  /**
   * Error that occurs during document chunking.
   *
   * @param message The error message
   * @param cause The cause of the error
   */
  case class ChunkingError(message: String, cause: Option[Throwable] = None) extends RAGError
  
  /**
   * Error that occurs during embedding generation.
   *
   * @param message The error message
   * @param cause The cause of the error
   */
  case class EmbeddingError(message: String, cause: Option[Throwable] = None) extends RAGError
  
  /**
   * Error that occurs during retrieval.
   *
   * @param message The error message
   * @param cause The cause of the error
   */
  case class RetrievalError(message: String, cause: Option[Throwable] = None) extends RAGError
  
  /**
   * Error that occurs during context building.
   *
   * @param message The error message
   * @param cause The cause of the error
   */
  case class ContextBuildingError(message: String, cause: Option[Throwable] = None) extends RAGError
  
  /**
   * Error that occurs during response generation.
   *
   * @param message The error message
   * @param cause The cause of the error
   */
  case class GenerationError(message: String, cause: Option[Throwable] = None) extends RAGError
  
  /**
   * Error that occurs during RAG system configuration.
   *
   * @param message The error message
   * @param cause The cause of the error
   */
  case class ConfigurationError(message: String, cause: Option[Throwable] = None) extends RAGError
  
  /**
   * Error that occurs during document processing.
   *
   * @param message The error message
   * @param cause The cause of the error
   */
  case class DocumentProcessingError(message: String, cause: Option[Throwable] = None) extends RAGError