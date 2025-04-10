package com.agenticai.examples.workflow

import com.agenticai.examples.workflow.agent.*
import com.agenticai.examples.workflow.engine.*
import com.agenticai.examples.workflow.model.*
import com.agenticai.examples.workflow.server.WorkflowHttpServer

import zio.*

/** Runner for the Workflow Example
  */
object WorkflowExampleRunner extends ZIOAppDefault:

  /** Run the workflow example
    */
  override def run: ZIO[Any, Any, ExitCode] =
    // Print intro message
    val intro = ZIO.succeed {
      println("===== Agentic AI Framework - Workflow Example =====")
      println("This example demonstrates how to create a workflow of specialized agents")
      println("that work together to process complex tasks.")
      println("\nOptions:")
      println("1. Run basic workflow with output to console")
      println("2. Run HTTP server (simulated)")
      println("3. Exit")
      print("\nEnter your choice (1-3): ")
      val choice = scala.io.StdIn.readLine()
      choice
    }

    // Handle user choice
    for
      choice <- intro
      exitCode <- choice match
        case "1" => runSimpleWorkflow
        case "2" => WorkflowHttpServer.run
        case _   => ZIO.succeed(ExitCode.success)
    yield exitCode

  /** Run a simple workflow in the console
    */
  private def runSimpleWorkflow: ZIO[Any, Throwable, ExitCode] =
    // Create workflow components
    val transformer = TextTransformerAgent.make()
    val splitter = TextSplitterAgent.make()
    val summarizer = SummarizationAgent.make()
    val buildAgent = BuildAgent.make()
    val engine = new WorkflowEngine(transformer, splitter, summarizer, buildAgent)

    // Create a workflow
    val workflow = engine.createTextProcessingWorkflow()

    // Sample input
    val input = """
    |The Agentic AI Framework is designed to build distributed, autonomous agent systems.
    |It uses modern Scala 3 features and ZIO for building robust, concurrent applications.
    |The framework enables composable agent behaviors through functional programming concepts.
    |Agents can discover each other based on capabilities and form dynamic workflows.
    |This example shows how specialized agents can be connected to process complex tasks.
    """.stripMargin.trim

    // Execute workflow
    for
      _ <- Console.printLine("\n------ Running Workflow ------")
      _ <- Console.printLine(s"Input text: $input")
      
      _ <- Console.printLine("\n--- Transformer Output ---")
      transformedText <- transformer.process(input)
      _ <- Console.printLine(transformedText)
      
      _ <- Console.printLine("\n--- Splitter Output ---")
      splitText <- splitter.process(transformedText)
      _ <- Console.printLine(splitText.mkString("\n• "))
      
      _ <- Console.printLine("\n--- Summarizer Output ---")
      summary <- summarizer.process(splitText)
      _ <- Console.printLine(summary)
      
      _ <- Console.printLine("\n--- Build Output ---")
      buildResult <- buildAgent.process(summary).catchAll(e => ZIO.succeed(s"Build failed: ${e.getMessage}"))
      _ <- Console.printLine(buildResult)
      
      // Complete workflow
      _ <- Console.printLine("\n--- Complete Workflow Result ---")
      result <- engine.executeWorkflow[String, String](workflow.id, input)
      _ <- Console.printLine(result)
      
      // Wait for user to continue
      _ <- Console.printLine("\nPress Enter to continue...")
      _ <- ZIO.succeed(scala.io.StdIn.readLine())
    yield ExitCode.success