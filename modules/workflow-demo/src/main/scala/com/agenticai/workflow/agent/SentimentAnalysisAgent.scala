package com.agenticai.workflow.agent

import zio.*

/** Agent that performs sentiment analysis on text
  */
class SentimentAnalysisAgent extends Agent[String, String]:
  private var modeConfig: String = "basic" // Default to basic mode

  /** Set the sentiment analysis mode
    *
    * @param mode
    *   The mode to use for sentiment analysis (basic, detailed)
    */
  def setMode(mode: String): Unit =
    modeConfig = mode

  /** Process the input text by performing sentiment analysis
    *
    * @param input
    *   Input text to analyze
    * @return
    *   Sentiment analysis result
    */
  def process(input: String): ZIO[Any, Throwable, String] =
    if (input.trim.isEmpty) ZIO.succeed("Input text was empty.")
    else ZIO.succeed {

      // Simple sentiment analysis based on positive and negative word counts
      val positiveWords = Set(
        "good", "great", "excellent", "wonderful", "amazing", "fantastic", 
        "happy", "joy", "love", "like", "best", "positive", "beautiful"
      )

      val negativeWords = Set(
        "bad", "terrible", "awful", "horrible", "disappointing", "sad", 
        "hate", "dislike", "worst", "negative", "ugly"
      )

      val words = input.toLowerCase.split("\\W+")
      val positiveCount = words.count(word => positiveWords.contains(word))
      val negativeCount = words.count(word => negativeWords.contains(word))

      val sentimentScore = positiveCount - negativeCount

      modeConfig match {
        case "detailed" =>
          val sentiment = 
            if (sentimentScore > 3) "Very Positive"
            else if (sentimentScore > 0) "Positive"
            else if (sentimentScore == 0) "Neutral"
            else if (sentimentScore > -3) "Negative"
            else "Very Negative"

          s"""Sentiment Analysis Results:
             |Sentiment: $sentiment
             |Score: $sentimentScore
             |Positive words: $positiveCount
             |Negative words: $negativeCount
             |Total words analyzed: ${words.length}
             |""".stripMargin

        case _ => // basic mode
          val sentiment = 
            if (sentimentScore > 0) "Positive"
            else if (sentimentScore == 0) "Neutral"
            else "Negative"

          s"Sentiment Analysis: The text appears to be $sentiment (score: $sentimentScore)"
      }
    }

object SentimentAnalysisAgent:
  /** Create a new sentiment analysis agent
    *
    * @return
    *   A new SentimentAnalysisAgent instance
    */
  def make(): SentimentAnalysisAgent = new SentimentAnalysisAgent()

  /** Create a layer that provides a SentimentAnalysisAgent
    *
    * @return
    *   ZLayer that provides a SentimentAnalysisAgent
    */
  val live: ULayer[SentimentAnalysisAgent] = ZLayer.succeed(make())
