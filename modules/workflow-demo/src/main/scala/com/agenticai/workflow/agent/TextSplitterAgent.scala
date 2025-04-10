package com.agenticai.workflow.agent

import zio.*

/** Agent that splits text based on a delimiter
  */
class TextSplitterAgent extends Agent[String, String]:

  /** Process the input text by splitting it based on the specified delimiter
    *
    * @param input
    *   Input text to split
    * @return
    *   Text with split sections
    */
  def process(input: String): ZIO[Any, Throwable, String] =
    ZIO.succeed {
      val delimiter = getDelimiter()

      val splitText = input.split(delimiter, -1)

      // For presentation, join with HTML line breaks
      splitText.mkString("<br>")
    }

  /** Get the current delimiter configuration In a real implementation, this would be externally
    * configurable
    */
  private def getDelimiter(): String = "\\n" // Default to newline

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
