package com.agenticai.workflow.agent

import zio._
import zio.stream._

/**
 * Agent that simulates LLM summarization of text
 */
class SummarizationAgent extends Agent[String, String] {
  /**
   * Process the input text by simulating summarization
   *
   * @param input Input text to summarize
   * @return Summarized text
   */
  def process(input: String): ZIO[Any, Throwable, String] = {
    if (input.trim.isEmpty) {
      ZIO.succeed("Input text was empty.")
    } else {
      // Simulate LLM summarization with a simplified approach
      ZIO.succeed {
        val sentences = input.split("[.!?]\\s+")
        val keywordExtraction = extractKeywords(input)
        
        val summary = if (sentences.length <= 3) {
          // For very short text, just return it
          input
        } else {
          // For longer text, extract the first and last sentence and add keywords
          val firstSentence = sentences.headOption.getOrElse("")
          val lastSentence = sentences.lastOption.getOrElse("")
          
          s"""Summary: ${firstSentence.trim}. 
             |${keywordExtraction}. 
             |${lastSentence.trim}.""".stripMargin
        }
        
        summary
      }
    }
  }
  
  /**
   * Simple keyword extraction that finds most common words
   */
  private def extractKeywords(text: String): String = {
    // Split into words, filter common words, find most frequent
    val words = text.toLowerCase
      .replaceAll("[^a-z\\s]", "")
      .split("\\s+")
      .filter(_.length > 3)
      .groupBy(identity)
      .view
      .mapValues(_.length)
      .toList
      .sortBy(-_._2)
      .take(5)
      .map(_._1)
    
    if (words.isEmpty) "No keywords found"
    else s"Key concepts: ${words.mkString(", ")}"
  }
}

object SummarizationAgent {
  /**
   * Create a new summarization agent
   *
   * @return A new SummarizationAgent instance
   */
  def make(): SummarizationAgent = new SummarizationAgent()
  
  /**
   * Create a layer that provides a SummarizationAgent
   *
   * @return ZLayer that provides a SummarizationAgent
   */
  val live: ULayer[SummarizationAgent] = ZLayer.succeed(make())
}