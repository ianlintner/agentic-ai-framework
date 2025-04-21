package com.agenticai.core.llm.langchain.rag.context

import com.agenticai.core.llm.langchain.rag.RAGError
import dev.langchain4j.data.segment.TextSegment
import zio.*

/**
 * Interface for building context from retrieved text segments.
 */
trait ContextBuilder:
  /**
   * Build a context from the provided text segments and query.
   *
   * @param segments The retrieved text segments
   * @param query The original user query
   * @return A ZIO effect that completes with the built context
   */
  def buildContext(segments: List[TextSegment], query: String): ZIO[Any, RAGError.ContextBuildingError, String]

/**
 * Default implementation of ContextBuilder.
 *
 * @param maxContextLength The maximum length of the context in characters
 * @param contextTemplate The template to use for formatting the context
 */
case class DefaultContextBuilder(
  maxContextLength: Int = 4000,
  contextTemplate: String = "Answer the question based on the following context:\n\nContext:\n{context}\n\nQuestion: {query}"
) extends ContextBuilder:

  override def buildContext(segments: List[TextSegment], query: String): ZIO[Any, RAGError.ContextBuildingError, String] =
    ZIO.attempt {
      // Join the segments into a single context
      val joinedSegments = joinSegments(segments, maxContextLength)
      
      // Format the context using the template
      contextTemplate
        .replace("{context}", joinedSegments)
        .replace("{query}", query)
    }.mapError(e => RAGError.ContextBuildingError(s"Failed to build context: ${e.getMessage}", Some(e)))
  
  /**
   * Join segments into a single context, respecting the maximum context length.
   *
   * @param segments The segments to join
   * @param maxLength The maximum length of the joined text
   * @return The joined text
   */
  private def joinSegments(segments: List[TextSegment], maxLength: Int): String =
    val builder = new StringBuilder()
    var remainingLength = maxLength
    
    segments.foreach { segment =>
      val text = segment.text()
      if text.length < remainingLength then
        if builder.nonEmpty then builder.append("\n\n")
        builder.append(text)
        remainingLength -= text.length
    }
    
    builder.toString()