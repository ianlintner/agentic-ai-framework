package com.agenticai.workflow.agent

import zio.*

/** Agent that splits text based on a delimiter
  */
class TextSplitterAgent extends Agent[String, String]:
  private var delimiterConfig: String = "\\n" // Default to newline

  /** Set the delimiter configuration
    *
    * @param delimiter
    *   The delimiter to use for splitting text
    */
  def setDelimiter(delimiter: String): Unit =
    delimiterConfig = delimiter

  /** Process the input text by splitting it based on the specified delimiter
    *
    * @param input
    *   Input text to split
    * @return
    *   Text with split sections
    */
  def process(input: String): ZIO[Any, Throwable, String] =
    ZIO.succeed {
      val splitText = input.split(delimiterConfig, -1)

      // For presentation, join with HTML line breaks
      splitText.mkString("<br>")
    }

object TextSplitterAgent:
  /** Create a new text splitter agent
    *
    * @return
    *   A new TextSplitterAgent instance
    */
  def make(): TextSplitterAgent = new TextSplitterAgent()

  /** Create a layer that provides a TextSplitterAgent
    *
    * @return
    *   ZLayer that provides a TextSplitterAgent
    */
  val live: ULayer[TextSplitterAgent] = ZLayer.succeed(make())
