package com.agenticai.workflow.agent

import zio._

/**
 * Agent that transforms text based on configuration
 */
class TextTransformerAgent extends Agent[String, String] {
  /**
   * Process the input text by applying the specified transformation
   *
   * @param input Input text to transform
   * @return Transformed text
   */
  def process(input: String): ZIO[Any, Throwable, String] = {
    ZIO.succeed {
      // Default to capitalize if no transform specified
      val transform = getTransform()
      
      transform match {
        case "uppercase" => input.toUpperCase
        case "lowercase" => input.toLowerCase
        case "capitalize" => input.split("\\s+").map(_.capitalize).mkString(" ")
        case _ => input // No transformation
      }
    }
  }
  
  /**
   * Get the current transform configuration
   * In a real implementation, this would be externally configurable
   */
  private def getTransform(): String = "capitalize"
}

object TextTransformerAgent {
  /**
   * Create a new text transformer agent
   *
   * @return A new TextTransformerAgent instance
   */
  def make(): TextTransformerAgent = new TextTransformerAgent()
  
  /**
   * Create a layer that provides a TextTransformerAgent
   *
   * @return ZLayer that provides a TextTransformerAgent
   */
  val live: ULayer[TextTransformerAgent] = ZLayer.succeed(make())
}