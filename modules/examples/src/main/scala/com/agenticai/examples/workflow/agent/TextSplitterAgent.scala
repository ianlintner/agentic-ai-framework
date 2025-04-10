package com.agenticai.examples.workflow.agent

import zio.*

/** Agent that splits text into chunks based on configuration
  */
class TextSplitterAgent extends Agent[String, List[List[String]]]:

  /** Process the input text by splitting it into chunks
    *
    * @param input
    *   Input text to split
    * @return
    *   List of text chunks, where each chunk is a list of sentences
    */
  def process(input: String): ZIO[Any, Throwable, List[List[String]]] =
    ZIO.succeed {
      // Split into sentences (crude approximation)
      val sentenceDelimiters = """[.!?]"""
      val sentences = input.split(sentenceDelimiters).map(_.trim).filter(_.nonEmpty).toList

      // Group sentences into chunks
      val chunkSize = getChunkSize()
      sentences.grouped(chunkSize).toList
    }

  /** Get the configured chunk size
    *
    * In a real implementation, this would be externally configurable
    */
  private def getChunkSize(): Int = 3

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