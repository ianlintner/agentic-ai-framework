package com.agenticai.workflow.server

import com.agenticai.workflow.model.*
import com.agenticai.workflow.engine.*
import com.agenticai.workflow.agent.*
import zio.*

import java.util.UUID

/** Simplified demo to showcase workflow functionality
  */
object WorkflowServer extends ZIOAppDefault:

  // Create a default workflow
  private val sampleWorkflow = Workflow(
    id = UUID.randomUUID().toString,
    name = "Text Processing, Sentiment Analysis, and Build Demo",
    description = "A demo workflow that transforms, summarizes text, analyzes sentiment, and performs build operations",
    nodes = List(
      WorkflowNode(
        id = "node-1",
        nodeType = "text-transformer",
        label = "Capitalize Text",
        configuration = Map("transform" -> "capitalize"),
        position = NodePosition(150, 100)
      ),
      WorkflowNode(
        id = "node-2",
        nodeType = "summarizer",
        label = "Summarize Text",
        configuration = Map(),
        position = NodePosition(450, 100)
      ),
      WorkflowNode(
        id = "node-3",
        nodeType = "sentiment-analysis",
        label = "Analyze Sentiment",
        configuration = Map("mode" -> "detailed"),
        position = NodePosition(600, 100)
      ),
      WorkflowNode(
        id = "node-4",
        nodeType = "build",
        label = "Build Project",
        configuration = Map("target" -> "release"),
        position = NodePosition(750, 100)
      )
    ),
    connections = List(
      NodeConnection(
        id = "conn-1",
        sourceNodeId = "node-1",
        targetNodeId = "node-2"
      ),
      NodeConnection(
        id = "conn-2",
        sourceNodeId = "node-2",
        targetNodeId = "node-3"
      ),
      NodeConnection(
        id = "conn-3",
        sourceNodeId = "node-3",
        targetNodeId = "node-4"
      )
    )
  )

  /** Run the demo
    */
  def run =
    val sampleText = """
      |The Agentic AI Framework is a sophisticated platform for developing autonomous AI agents.
      |It provides tools for agent composition, memory systems, and workflow orchestration.
      |Agents can be connected in a workflow to perform complex tasks, such as text processing,
      |data analysis, and content generation. The framework is built on functional programming
      |principles and leverages Scala's type system for safety and composability.
      """.stripMargin

    for
      // Welcome message
      _ <- Console.printLine("=== Agentic AI Workflow Demo ===")
      _ <- Console.printLine(
        "This demo shows how workflows connect specialized agents to perform a complex task."
      )
      _ <- Console.printLine(
        "In a web implementation, you would be able to visually build these workflows."
      )
      _ <- Console.printLine("")

      // Print the workflow definition
      _ <- Console.printLine("Workflow Definition:")
      _ <- Console.printLine(s"Name: ${sampleWorkflow.name}")
      _ <- Console.printLine(s"Description: ${sampleWorkflow.description}")
      _ <- Console.printLine("Nodes:")
      _ <- ZIO.foreach(sampleWorkflow.nodes) { node =>
        Console.printLine(s"  - ${node.label} (${node.nodeType})")
      }
      _ <- Console.printLine("Connections:")
      _ <- ZIO.foreach(sampleWorkflow.connections) { conn =>
        val sourceNode =
          sampleWorkflow.nodes.find(_.id == conn.sourceNodeId).map(_.label).getOrElse("Unknown")
        val targetNode =
          sampleWorkflow.nodes.find(_.id == conn.targetNodeId).map(_.label).getOrElse("Unknown")
        Console.printLine(s"  - $sourceNode → $targetNode")
      }
      _ <- Console.printLine("")

      // Print sample input
      _ <- Console.printLine("Sample Input:")
      _ <- Console.printLine(sampleText)
      _ <- Console.printLine("")

      // Create the workflow engine and agents
      textTransformer   <- ZIO.succeed(TextTransformerAgent.make())
      textSplitter      <- ZIO.succeed(TextSplitterAgent.make())
      summarizer        <- ZIO.succeed(SummarizationAgent.make())
      buildAgent        <- ZIO.succeed(BuildAgent.make())
      sentimentAnalyzer <- ZIO.succeed(SentimentAnalysisAgent.make())
      engine <- ZIO.succeed(
        new WorkflowEngine(textTransformer, textSplitter, summarizer, buildAgent, sentimentAnalyzer)
      )

      // Execute the first node (capitalize)
      _ <- Console.printLine("=== Executing Workflow ===")
      _ <- Console.printLine("Step 1: Executing 'Capitalize Text' node")

      // Show processing
      firstNodeResult <- textTransformer.process(sampleText)
      _               <- Console.printLine("Result after capitalization:")
      _               <- Console.printLine(firstNodeResult)
      _               <- Console.printLine("")

      // Execute the second node (summarize)
      _                <- Console.printLine("Step 2: Executing 'Summarize Text' node")
      secondNodeResult <- summarizer.process(firstNodeResult)
      _                <- Console.printLine("Final result (summarization):")
      _                <- Console.printLine(secondNodeResult)
      _                <- Console.printLine("")

      // Execute the sentiment analysis node
      _               <- Console.printLine("Step 3: Executing 'Analyze Sentiment' node")
      thirdNodeResult <- sentimentAnalyzer.process(secondNodeResult)
      _               <- Console.printLine("Result after sentiment analysis:")
      _               <- Console.printLine(thirdNodeResult)
      _               <- Console.printLine("")

      // Execute the build node
      _               <- Console.printLine("Step 4: Executing 'Build Project' node")
      fourthNodeResult <- buildAgent.process(thirdNodeResult)
      _               <- Console.printLine("Final result (build):")
      _               <- Console.printLine(fourthNodeResult)
      _               <- Console.printLine("")

      // Execute the entire workflow
      _              <- Console.printLine("=== Complete Workflow Execution ===")
      completeResult <- engine.executeWorkflow(sampleWorkflow, sampleText)
      _              <- Console.printLine("Complete workflow result:")
      _              <- Console.printLine(completeResult)
      _              <- Console.printLine("")

      // Final message
      _ <- Console.printLine("=== Demo Completed ===")
      _ <- Console.printLine("In a full web implementation, you would be able to:")
      _ <- Console.printLine("1. Create custom workflows with a visual editor")
      _ <- Console.printLine(
        "2. Add various agent types (transformers, summarizers, splitters, build agents, etc.)"
      )
      _ <- Console.printLine("3. Connect agents in complex topologies")
      _ <- Console.printLine("4. Execute workflows and see results in real-time")
      _ <- Console.printLine("5. Monitor build and deployment processes through the same interface")
      _ <- Console.printLine("")
      _ <- Console.printLine(
        "The workflow demo now demonstrates a complete end-to-end process from"
      )
      _ <- Console.printLine("text transformation to summarization to build operations.")
    yield ()
