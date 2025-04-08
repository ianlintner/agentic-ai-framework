package com.agenticai.examples.mesh

import zio._
import com.agenticai.core.agent.Agent
import com.agenticai.core.mesh._
import com.agenticai.core.mesh.protocol._
import com.agenticai.core.mesh.discovery._
import com.agenticai.core.mesh.server.HttpServer
import scala.util.Random

/**
 * Example demonstrating capability-based agent discovery in the mesh.
 * 
 * This example shows how to register agents with specific capabilities
 * and then discover them based on those capabilities to create dynamic
 * workflows.
 */
object CapabilityBasedDiscoveryExample extends ZIOAppDefault {
  /**
   * Text processing agent with formatting capabilities.
   */
  class TextFormattingAgent extends Agent[String, String] {
    def process(input: String): Task[String] = ZIO.succeed {
      val tokens = input.split(" ")
      tokens.map(_.capitalize).mkString(" ")
    }
  }
  
  /**
   * Text summarization agent with text reduction capabilities.
   */
  class TextSummarizationAgent extends Agent[String, String] {
    def process(input: String): Task[String] = ZIO.succeed {
      // Very simple summarization by keeping first and last sentence
      // and truncating the middle
      val sentences = input.split("[.!?]\\s+")
      if (sentences.length <= 2) input
      else {
        s"${sentences.head}. ... ${sentences.last}."
      }
    }
  }
  
  /**
   * Calculator agent with math capabilities.
   */
  class CalculatorAgent extends Agent[String, Double] {
    def process(input: String): Task[Double] = ZIO.attempt {
      val tokens = input.split(" ")
      val operation = tokens(0)
      val a = tokens(1).toDouble
      val b = tokens(2).toDouble
      
      operation match {
        case "add" => a + b
        case "subtract" => a - b
        case "multiply" => a * b
        case "divide" => a / b
        case _ => throw new IllegalArgumentException(s"Unknown operation: $operation")
      }
    }
  }
  
  /**
   * Language translation agent with translation capabilities.
   */
  class TranslationAgent extends Agent[String, String] {
    private val englishToSpanish = Map(
      "hello" -> "hola",
      "world" -> "mundo",
      "good" -> "bueno",
      "morning" -> "mañana",
      "evening" -> "tarde",
      "day" -> "día",
      "night" -> "noche",
      "thank you" -> "gracias",
      "welcome" -> "bienvenido",
      "goodbye" -> "adiós"
    )
    
    def process(input: String): Task[String] = ZIO.succeed {
      val command = input.trim.toLowerCase
      if (command.startsWith("translate ")) {
        val text = command.substring("translate ".length)
        text.split(" ").map(word => 
          englishToSpanish.getOrElse(word.toLowerCase, word)
        ).mkString(" ")
      } else {
        "Unknown command. Start with 'translate ' followed by text."
      }
    }
  }
  
  /**
   * Sentiment analysis agent with text analysis capabilities.
   */
  class SentimentAnalysisAgent extends Agent[String, Double] {
    private val positiveWords = Set("good", "great", "excellent", "wonderful", "amazing", "fantastic", "happy", "joy", "love", "like")
    private val negativeWords = Set("bad", "terrible", "awful", "horrible", "sad", "hate", "dislike", "angry", "upset", "disappointed")
    
    def process(input: String): Task[Double] = ZIO.succeed {
      val words = input.toLowerCase.split("\\W+")
      val positiveCount = words.count(positiveWords.contains)
      val negativeCount = words.count(negativeWords.contains)
      
      if (positiveCount + negativeCount == 0) 0.0
      else (positiveCount - negativeCount).toDouble / (positiveCount + negativeCount)
    }
  }
  
  /**
   * Run the capability-based discovery example.
   */
  def run: Task[Unit] = {
    // Create serialization for the mesh
    val serialization = JsonSerialization()
    
    // Program that demonstrates capability-based agent discovery
    val program = for {
      // Start a local node
      _ <- Console.printLine("Starting local node...")
      localServer <- HttpServer(8080, serialization).start
      
      // Create mesh interface
      localLocation = AgentLocation.local(8080)
      protocol = HttpProtocol(serialization)
      mesh = AgentMesh(protocol, localLocation)
      
      // Create agent directory
      _ <- Console.printLine("Creating agent directory...")
      directory <- InMemoryAgentDirectory()
      
      // Create agent instances
      formattingAgent = new TextFormattingAgent()
      summarizationAgent = new TextSummarizationAgent()
      calculatorAgent = new CalculatorAgent()
      translationAgent = new TranslationAgent()
      sentimentAgent = new SentimentAnalysisAgent()
      
      // Deploy agents to the mesh
      _ <- Console.printLine("\nDeploying agents to the mesh...")
      formattingRef <- mesh.deploy(formattingAgent, localLocation)
      summarizationRef <- mesh.deploy(summarizationAgent, localLocation)
      calculatorRef <- mesh.deploy(calculatorAgent, localLocation)
      translationRef <- mesh.deploy(translationAgent, localLocation)
      sentimentRef <- mesh.deploy(sentimentAgent, localLocation)
      
      // Register agents with capabilities
      _ <- Console.printLine("Registering agents with capabilities...")
      _ <- directory.registerAgent(
        formattingRef,
        AgentMetadata(
          capabilities = Set("text-processing", "formatting", "capitalization"),
          inputType = "java.lang.String",
          outputType = "java.lang.String",
          properties = Map("language" -> "en", "description" -> "Text formatting agent")
        )
      )
      
      _ <- directory.registerAgent(
        summarizationRef,
        AgentMetadata(
          capabilities = Set("text-processing", "summarization", "text-reduction"),
          inputType = "java.lang.String",
          outputType = "java.lang.String",
          properties = Map("language" -> "en", "description" -> "Text summarization agent")
        )
      )
      
      _ <- directory.registerAgent(
        calculatorRef,
        AgentMetadata(
          capabilities = Set("math", "arithmetic", "calculation"),
          inputType = "java.lang.String",
          outputType = "java.lang.Double",
          properties = Map("operations" -> "add,subtract,multiply,divide")
        )
      )
      
      _ <- directory.registerAgent(
        translationRef,
        AgentMetadata(
          capabilities = Set("translation", "language", "multilingual"),
          inputType = "java.lang.String",
          outputType = "java.lang.String",
          properties = Map("sourceLang" -> "en", "targetLang" -> "es")
        )
      )
      
      _ <- directory.registerAgent(
        sentimentRef,
        AgentMetadata(
          capabilities = Set("analysis", "sentiment", "text-processing"),
          inputType = "java.lang.String",
          outputType = "java.lang.Double",
          properties = Map("language" -> "en", "description" -> "Sentiment analysis agent")
        )
      )
      
      // Update status of all agents to Active
      _ <- Console.printLine("Setting all agents to Active status...")
      _ <- directory.updateAgentStatus(formattingRef.id, AgentStatus.Active)
      _ <- directory.updateAgentStatus(summarizationRef.id, AgentStatus.Active)
      _ <- directory.updateAgentStatus(calculatorRef.id, AgentStatus.Active)
      _ <- directory.updateAgentStatus(translationRef.id, AgentStatus.Active)
      _ <- directory.updateAgentStatus(sentimentRef.id, AgentStatus.Active)
      
      // List all registered agents
      _ <- Console.printLine("\nListing all registered agents:")
      allAgents <- directory.getAllAgents()
      _ <- ZIO.foreach(allAgents) { agent =>
        Console.printLine(s"  - Agent ${agent.agentId}: ${agent.metadata.capabilities.mkString(", ")}")
      }
      
      // Discover agents by capability
      _ <- Console.printLine("\nDiscovering agents by capability:")
      
      // Find text processing agents
      _ <- Console.printLine("\n1. Finding text processing agents:")
      textAgents <- directory.discoverAgents(AgentQuery(
        capabilities = Set("text-processing"),
        limit = 10
      ))
      _ <- ZIO.foreach(textAgents) { agent =>
        Console.printLine(s"  - Found ${agent.agentId}: ${agent.metadata.capabilities.mkString(", ")}")
      }
      
      // Find translation agents
      _ <- Console.printLine("\n2. Finding translation agents:")
      translationAgents <- directory.discoverAgents(AgentQuery(
        capabilities = Set("translation"),
        limit = 10
      ))
      _ <- ZIO.foreach(translationAgents) { agent =>
        Console.printLine(s"  - Found ${agent.agentId}: ${agent.metadata.capabilities.mkString(", ")}")
      }
      
      // Find math agents
      _ <- Console.printLine("\n3. Finding math agents:")
      mathAgents <- directory.discoverAgents(AgentQuery(
        capabilities = Set("math"),
        limit = 10
      ))
      _ <- ZIO.foreach(mathAgents) { agent =>
        Console.printLine(s"  - Found ${agent.agentId}: ${agent.metadata.capabilities.mkString(", ")}")
      }
      
      // Create remote agent wrappers
      _ <- Console.printLine("\nCreating remote agent wrappers from discovered agents...")
      remoteAgents <- ZIO.foreach(textAgents) { info =>
        mesh.getRemoteAgent(info.ref.asInstanceOf[RemoteAgentRef[String, String]])
          .map(agent => (info.metadata.capabilities, agent))
      }
      
      // Use discovered agents in a workflow
      _ <- Console.printLine("\nUsing discovered agents in a workflow:")
      input = "This is a sample text that will be processed by multiple agents. " +
              "We will format it, analyze it, and summarize it. " +
              "It's a great example of dynamic agent discovery and composition."
      
      _ <- Console.printLine(s"\nOriginal input: $input")
      
      // Find a formatting agent
      val formattingAgents = remoteAgents.filter(_._1.contains("formatting")).map(_._2)
      _ <- ZIO.when(formattingAgents.nonEmpty) {
        for {
          agent <- ZIO.succeed(formattingAgents.head)
          _ <- Console.printLine("\nStep 1: Formatting text...")
          result <- agent.process(input)
          _ <- Console.printLine(s"  Result: $result")
        } yield ()
      }
      
      // Find a summarization agent
      val summarizationAgents = remoteAgents.filter(_._1.contains("summarization")).map(_._2)
      _ <- ZIO.when(summarizationAgents.nonEmpty) {
        for {
          agent <- ZIO.succeed(summarizationAgents.head)
          _ <- Console.printLine("\nStep 2: Summarizing text...")
          result <- agent.process(input)
          _ <- Console.printLine(s"  Result: $result")
        } yield ()
      }
      
      // Use translation agent
      translationRemoteAgents <- ZIO.when(translationAgents.nonEmpty) {
        mesh.getRemoteAgent(translationAgents.head.ref.asInstanceOf[RemoteAgentRef[String, String]])
      }
      
      _ <- ZIO.foreach(translationRemoteAgents) { agent =>
        for {
          _ <- Console.printLine("\nStep 3: Translating a phrase...")
          result <- agent.process("translate hello world good morning")
          _ <- Console.printLine(s"  Result: $result")
        } yield ()
      }
      
      // Use calculator agent
      calculatorRemoteAgents <- ZIO.when(mathAgents.nonEmpty) {
        mesh.getRemoteAgent(mathAgents.head.ref.asInstanceOf[RemoteAgentRef[String, Double]])
      }
      
      _ <- ZIO.foreach(calculatorRemoteAgents) { agent =>
        for {
          _ <- Console.printLine("\nStep 4: Performing a calculation...")
          result <- agent.process("multiply 7 6")
          _ <- Console.printLine(s"  Result: $result")
        } yield ()
      }
      
      // Final message
      _ <- Console.printLine("\nCapability-based discovery example completed successfully!")
      _ <- Console.printLine("The agents were discovered based on their capabilities and used in a workflow.")
      
      // Shutdown
      _ <- Console.printLine("\nShutting down...")
      _ <- localServer.interrupt
    } yield ()
    
    // Run the program with error handling
    program.catchAll { error =>
      Console.printLine(s"Error: ${error.getMessage}")
    }
  }
}