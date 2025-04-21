package com.agenticai.core.llm.langchain.rag.document

import com.agenticai.core.llm.langchain.rag.RAGError
import dev.langchain4j.data.document.{Document, Metadata}
import zio.*

import java.nio.file.{Files, Paths}
import java.util.{Collections, HashMap, UUID}

/**
 * Processes documents for the RAG system. This trait is responsible for loading
 * and pre-processing documents before they are chunked and embedded.
 */
trait DocumentProcessor:
  /**
   * Processes a text string into a Document.
   *
   * @param text The text content to process
   * @param metadata Optional metadata to attach to the document
   * @return A ZIO effect that completes with the processed Document
   */
  def processText(text: String, metadata: Map[String, String] = Map.empty): ZIO[Any, RAGError.DocumentProcessingError, Document]
  
  /**
   * Processes a file into a Document.
   *
   * @param path The path to the file
   * @param metadata Optional metadata to attach to the document
   * @return A ZIO effect that completes with the processed Document
   */
  def processFile(path: String, metadata: Map[String, String] = Map.empty): ZIO[Any, RAGError.DocumentProcessingError, Document]

/**
 * Simple implementation of DocumentProcessor for text documents.
 * This implementation is compatible with the beta version of Langchain4j.
 */
case class TextDocumentProcessor() extends DocumentProcessor:
  // Convert Scala Map to Java-friendly Metadata object
  private def createMetadata(meta: Map[String, String]): Metadata =
    val javaMap = new HashMap[String, String]()
    meta.foreach { case (k, v) => javaMap.put(k, v) }
    Metadata.from(javaMap)
    
  override def processText(text: String, metadata: Map[String, String] = Map.empty): ZIO[Any, RAGError.DocumentProcessingError, Document] =
    ZIO.succeed {
      // For Langchain4j 1.0.0-beta2, use Document constructor with text and metadata
      Document.from(text, createMetadata(metadata))
    }
  
  override def processFile(path: String, metadata: Map[String, String] = Map.empty): ZIO[Any, RAGError.DocumentProcessingError, Document] =
    ZIO.attemptBlocking {
      val content = Files.readString(Paths.get(path))
      
      // Add the file path to metadata
      val meta = metadata + ("source" -> path)
      
      // For Langchain4j 1.0.0-beta2, use Document constructor with text and metadata
      Document.from(content, createMetadata(meta))
    }.mapError(e => RAGError.DocumentProcessingError(s"Failed to process file: ${e.getMessage}", Some(e)))