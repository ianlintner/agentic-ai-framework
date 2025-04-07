package com.agenticai.examples.mesh

import zio._
import com.agenticai.core.agent.Agent
import java.time.Instant
import java.util.UUID
import scala.util.Random
import scala.collection.concurrent.TrieMap

/**
 * Simplified example demonstrating agent discovery concepts.
 * 
 * This is a standalone example that shows agent discovery principles without
 * relying on the full mesh implementation. It implements a basic in-memory
 * directory system that enables capability-based agent discovery.
 */
object SimpleAgentDiscoveryExample extends ZIOAppDefault {

  // ===== Basic data structures for agent discovery =====
  
  /**
   * Represents metadata about an agent's capabilities.
   */
  case class AgentMetadata(
    capabilities: Set[String],
    inputType: String,
    outputType: String,
    properties: Map[String, String] = Map.empty
  )
  
  /**
   * Represents an agent's status.
   */
  sealed trait AgentStatus
  object AgentStatus {
    case object Active extends AgentStatus
    case object Unavailable extends AgentStatus
  }
  
  /**
   * Complete information about a registered agent.
   */
  case class AgentInfo(
    agentId: UUID,
    agent: Agent[_, _],
    metadata: AgentMetadata,
    status: AgentStatus,
    registeredAt: Instant
  ) {
    def hasCapability(capability: String): Boolean =
      metadata.capabilities.contains(capability)
  }
  
  /**
   * Criteria for discovering agents.
   */
  case class AgentQuery(
    capabilities: Set[String] = Set.empty,
    properties: Map[String, String] = Map.empty,
    limit: Int = 10
  ) {
    def matches(info: AgentInfo): Boolean = {
      val hasCapabilities = capabilities.isEmpty || 
        capabilities.subsetOf(info.metadata.capabilities)
      
      val hasProperties = properties.isEmpty || 
        properties.forall { case (k, v) => info.metadata.properties.get(k).contains(v) }
      
      hasCapabilities && hasProperties
    }
  }
  
  /**
   * Simple in-memory agent directory.
   */
  class AgentDirectory {
    private val agents = TrieMap.empty[UUID, AgentInfo]
    
    def registerAgent[I, O](
      agent: Agent[I, O],
      metadata: AgentMetadata
    ): Task[UUID] = {
      val agentId = UUID.randomUUID()
      val info = AgentInfo(
        agentId = agentId,
        agent = agent,
        metadata = metadata,
        status = AgentStatus.Active,
        registeredAt = Instant.now()
      )
      
      ZIO.succeed {
        agents.put(agentId, info)
        agentId
      }
    }
    
    def discoverAgents(query: AgentQuery): Task[List[AgentInfo]] = {
      ZIO.succeed {
        agents.values
          .filter(query.matches)
          .toList
          .sortBy(-_.registeredAt.toEpochMilli())
          .take(query.limit)
      }
    }
    
    def findAgentsByCapabilities(
      capabilities: Set[String],
      limit: Int = 10
    ): Task[List[AgentInfo]] = {
      discoverAgents(AgentQuery(capabilities = capabilities, limit = limit))
    }
    
    def getAgentById(agentId: UUID): Task[Option[AgentInfo]] = {
      ZIO.succeed(agents.get(agentId))
    }
  }

  // ===== Example agent implementations =====
  
  /**
   * Text translation agent.
   */
  class TranslationAgent extends Agent[TranslationRequest, String] {
    def process(input: TranslationRequest): Task[String] = ZIO.succeed {
      val text = input.text
      val sourceLanguage = input.sourceLanguage
      val targetLanguage = input.targetLanguage
      
      s"[Translated from $sourceLanguage to $targetLanguage] $text"
    }
  }
  
  case class TranslationRequest(
    text: String,
    sourceLanguage: String,
    targetLanguage: String
  )
  
  /**
   * Sentiment analysis agent.
   */
  class SentimentAnalysisAgent extends Agent[String, SentimentResult] {
    private val random = new Random()
    
    def process(input: String): Task[SentimentResult] = ZIO.succeed {
      // Simulated sentiment analysis
      val score = (random.nextDouble() * 2.0) - 1.0 // -1.0 to 1.0
      val sentiment = if (score < -0.3) "Negative"
                      else if (score > 0.3) "Positive"
                      else "Neutral"
                      
      SentimentResult(input, sentiment, score)
    }
  }
  
  case class SentimentResult(
    text: String,
    sentiment: String,
    score: Double
  )
  
  /**
   * Text summarization agent.
   */
  class SummarizationAgent extends Agent[SummarizationRequest, String] {
    def process(input: SummarizationRequest): Task[String] = ZIO.succeed {
      val text = input.text
      val length = input.maxLength
      
      // Simulated summarization
      val words = text.split("\\s+")
      val summary = if (words.length > length) {
        words.take(length).mkString(" ") + "..."
      } else {
        text
      }
      
      summary
    }
  }
  
  case class SummarizationRequest(
    text: String,
    maxLength: Int
  )
  
  /**
   * Information extraction agent.
   */
  class ExtractionAgent extends Agent[String, Map[String, String]] {
    def process(input: String): Task[Map[String, String]] = ZIO.succeed {
      // Simulated information extraction
      val entities = Map(
        "person" -> "John Doe",
        "organization" -> "Agentic AI",
        "location" -> "San Francisco",
        "date" -> Instant.now().toString
      )
      
      entities
    }
  }
  
  /**
   * Query answering agent that combines other agents.
   */
  class QueryAnsweringAgent(directory: AgentDirectory) extends Agent[String, String] {
    def process(input: String): Task[String] = {
      if (input.startsWith("translate:")) {
        // Find translation agent and use it
        for {
          agents <- directory.findAgentsByCapabilities(Set("translation"))
          translationAgent <- ZIO.fromOption(agents.headOption)
            .orElseFail(new Exception("No translation agent found"))
          request = TranslationRequest(
            input.substring("translate:".length), 
            "English", 
            "Spanish"
          )
          result <- translationAgent.agent.asInstanceOf[Agent[TranslationRequest, String]].process(request)
        } yield result
        
      } else if (input.startsWith("sentiment:")) {
        // Find sentiment analysis agent and use it
        for {
          agents <- directory.findAgentsByCapabilities(Set("sentiment-analysis"))
          sentimentAgent <- ZIO.fromOption(agents.headOption)
            .orElseFail(new Exception("No sentiment analysis agent found"))
          text = input.substring("sentiment:".length)
          result <- sentimentAgent.agent.asInstanceOf[Agent[String, SentimentResult]].process(text)
        } yield s"Sentiment: ${result.sentiment} (score: ${result.score})"
        
      } else if (input.startsWith("summarize:")) {
        // Find summarization agent and use it
        for {
          agents <- directory.findAgentsByCapabilities(Set("summarization"))
          summaryAgent <- ZIO.fromOption(agents.headOption)
            .orElseFail(new Exception("No summarization agent found"))
          text = input.substring("summarize:".length)
          request = SummarizationRequest(text, 10)
          result <- summaryAgent.agent.asInstanceOf[Agent[SummarizationRequest, String]].process(request)
        } yield s"Summary: $result"
        
      } else if (input.startsWith("extract:")) {
        // Find extraction agent and use it
        for {
          agents <- directory.findAgentsByCapabilities(Set("extraction"))
          extractionAgent <- ZIO.fromOption(agents.headOption)
            .orElseFail(new Exception("No extraction agent found"))
          text = input.substring("extract:".length)
          result <- extractionAgent.agent.asInstanceOf[Agent[String, Map[String, String]]].process(text)
          formatted = result.map { case (k, v) => s"$k: $v" }.mkString("\n")
        } yield s"Extracted entities:\n$formatted"
        
      } else {
        ZIO.succeed("Unknown command. Try starting with 'translate:', 'sentiment:', 'summarize:', or 'extract:'.")
      }
    }
  }
  
  /**
   * Main program demonstrating agent discovery.
   */
  def run: Task[Unit] = {
    for {
      // Create the agent directory
      directory <- ZIO.succeed(new AgentDirectory())
      
      // Create our specialized agents
      translationAgent <- ZIO.succeed(new TranslationAgent())
      sentimentAgent <- ZIO.succeed(new SentimentAnalysisAgent())
      summarizationAgent <- ZIO.succeed(new SummarizationAgent())
      extractionAgent <- ZIO.succeed(new ExtractionAgent())
      
      // Register the agents with their capabilities
      _ <- Console.printLine("Registering specialized agents with their capabilities...")
      
      _ <- directory.registerAgent(
        translationAgent,
        AgentMetadata(
          capabilities = Set("translation", "natural-language-processing"),
          inputType = "TranslationRequest",
          outputType = "String",
          properties = Map("supported-languages" -> "English,Spanish,French,German")
        )
      )
      
      _ <- directory.registerAgent(
        sentimentAgent,
        AgentMetadata(
          capabilities = Set("sentiment-analysis", "natural-language-processing"),
          inputType = "String",
          outputType = "SentimentResult",
          properties = Map("model" -> "sentiment-classifier-v1")
        )
      )
      
      _ <- directory.registerAgent(
        summarizationAgent,
        AgentMetadata(
          capabilities = Set("summarization", "natural-language-processing"),
          inputType = "SummarizationRequest",
          outputType = "String",
          properties = Map("max-length" -> "100")
        )
      )
      
      _ <- directory.registerAgent(
        extractionAgent,
        AgentMetadata(
          capabilities = Set("extraction", "natural-language-processing", "entity-recognition"),
          inputType = "String",
          outputType = "Map[String, String]",
          properties = Map("entities" -> "person,organization,location,date")
        )
      )
      
      // Create our query answering agent that will use discovery
      queryAgent <- ZIO.succeed(new QueryAnsweringAgent(directory))
      
      // Display available agents and their capabilities
      _ <- Console.printLine("\nDiscovering all agents with natural-language-processing capability:")
      nlpAgents <- directory.findAgentsByCapabilities(Set("natural-language-processing"))
      _ <- ZIO.foreach(nlpAgents) { info =>
        Console.printLine(s"  - Agent: ${info.agentId}, Capabilities: ${info.metadata.capabilities.mkString(", ")}")
      }
      
      // Test our query answering agent with different requests
      _ <- Console.printLine("\nTesting the query answering agent:")
      
      queries = List(
        "translate:Hello, how are you today?",
        "sentiment:I'm really enjoying this distributed agent framework!",
        "summarize:The distributed agent discovery enables seamless collaboration between AI agents based on their capabilities. This system allows agents to register their capabilities and be discovered by other agents that need specific functionalities.",
        "extract:John Doe works at Agentic AI in San Francisco."
      )
      
      _ <- ZIO.foreach(queries) { query =>
        for {
          _ <- Console.printLine(s"\nQuery: $query")
          result <- queryAgent.process(query)
          _ <- Console.printLine(s"Result: $result")
        } yield ()
      }
      
      // Final message
      _ <- Console.printLine("""
        |
        |Agent discovery example completed.
        |
        |This example demonstrated the key concepts of agent discovery:
        |1. Agents register their capabilities in a directory
        |2. Other agents can discover them based on those capabilities
        |3. Dynamic composition of agent capabilities to solve complex tasks
        |
        |In a full mesh implementation, these principles would work across machines
        |and networks, enabling distributed agent discovery and collaboration.
        """.stripMargin)
    } yield ()
  }
}