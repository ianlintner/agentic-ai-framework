package com.agenticai.examples.workflow.agent

import zio.*

/** Agent that simulates running a build process
  */
class BuildAgent extends Agent[String, String]:

  /** Process the input by simulating a build operation
    *
    * @param input
    *   Input to build
    * @return
    *   Build result
    */
  def process(input: String): ZIO[Any, Throwable, String] =
    for
      // Simulate build steps with delays
      _ <- ZIO.logInfo(s"Build started for: $input")
      _ <- ZIO.sleep(java.time.Duration.ofMillis(500))
      _ <- ZIO.logInfo("Compiling...")
      _ <- ZIO.sleep(java.time.Duration.ofMillis(500))
      _ <- ZIO.logInfo("Running tests...")
      _ <- ZIO.sleep(java.time.Duration.ofMillis(500))
      _ <- ZIO.logInfo("Bundling artifacts...")
      _ <- ZIO.sleep(java.time.Duration.ofMillis(500))
      _ <- ZIO.logInfo("Build completed")
      
      // For simulation, always succeed
      result <- ZIO.succeed(s"Build successful for: $input")
    yield result

object BuildAgent:
  /** Create a new build agent
    *
    * @return
    *   A new BuildAgent instance
    */
  def make(): BuildAgent = new BuildAgent()

  /** Create a layer that provides a BuildAgent
    *
    * @return
    *   ZLayer that provides a BuildAgent
    */
  val live: ULayer[BuildAgent] = ZLayer.succeed(make())