package com.agenticai.examples.workflow.agent

import zio.*

/** Agent that summarizes text input
  */
class SummarizationAgent extends Agent[List[List[String]], String]:

  /** Process the input text chunks to produce a summary
    *
    * @param input
    *   List of chunks, where each chunk is a list of sentences
    * @return
    *   Summarized text
    */
  def process(input: List[List[String]]): ZIO[Any, Throwable, String] =
    ZIO.succeed {
      // Simple mock summarization - in a real agent, this would call an LLM API
      val concatenated = input.flatten.mkString(" ")
      
      if concatenated.length > 100 then
        // Crude summarization by truncation
        concatenated.take(100) + "..."
      else
        // Short enough text, no summarization needed
        concatenated
    }

object SummarizationAgent:
  /** Create a new summarization agent
    *
    * @return
    *   A new SummarizationAgent instance
    */
  def make(): SummarizationAgent = new SummarizationAgent()

  /** Create a layer that provides a SummarizationAgent
    *
    * @return
    *   ZLayer that provides a SummarizationAgent
    */
  val live: ULayer[SummarizationAgent] = ZLayer.succeed(make())