package com.agenticai.workflow.agent

import zio.*

/** Agent that transforms text based on configuration
  */
class TextTransformerAgent extends Agent[String, String]:
  private var transformConfig: String = "capitalize" // Default value

  /** Set the transform configuration
    *
    * @param transform
    *   The transformation to apply (uppercase, lowercase, capitalize, reverse)
    */
  def setTransform(transform: String): Unit =
    transformConfig = transform

  /** Process the input text by applying the specified transformation
    *
    * @param input
    *   Input text to transform
    * @return
    *   Transformed text
    */
  def process(input: String): ZIO[Any, Throwable, String] =
    ZIO.succeed {
      transformConfig match
        case "uppercase"  => input.toUpperCase
        case "lowercase"  => input.toLowerCase
        case "capitalize" => input.split("\\s+").map(_.capitalize).mkString(" ")
        case "reverse"    => input.reverse
        case _            => input // No transformation
    }

object TextTransformerAgent:
  /** Create a new text transformer agent
    *
    * @return
    *   A new TextTransformerAgent instance
    */
  def make(): TextTransformerAgent = new TextTransformerAgent()

  /** Create a layer that provides a TextTransformerAgent
    *
    * @return
    *   ZLayer that provides a TextTransformerAgent
    */
  val live: ULayer[TextTransformerAgent] = ZLayer.succeed(make())
