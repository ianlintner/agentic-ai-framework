package com.agenticai.examples.capability

import zio._
import com.agenticai.core.capability.{ComposableAgent, ComposableAgentDirectory}
import com.agenticai.core.capability.CapabilityTaxonomy
import com.agenticai.core.capability.CapabilityTaxonomy.{Capability, CapabilityRegistry}

/**
 * Example demonstrating the capability-based agent system.
 *
 * This example shows how specialized agents with specific capabilities can be 
 * organized, discovered, and composed to create complex workflows.
 */
object ComposableAgentExample extends ZIOAppDefault {
  
  // Data types for document processing
  case class Document(title: String, content: String)
  case class Category(name: String, confidence: Double)
  case class Sentiment(positive: Double, negative: Double, neutral: Double)
  
  // Specialized document processing agents
  
  // Extract text from a document (Document -> String)
  val documentToTextAgent = ComposableAgent[Document, String](
    processImpl = document => ZIO.succeed(document.content),
    agentCapabilities = Set("text-extraction", "document-processing"),
    inType = "Document",
    outType = "String",
    agentProperties = Map("description" -> "Extracts text from documents")
  )
  
  // Convert text to lowercase (String -> String)
  val lowercaseAgent = ComposableAgent[String, String](
    processImpl = text => ZIO.succeed(text.toLowerCase),
    agentCapabilities = Set("text-processing", "normalization"),
    inType = "String",
    outType = "String",
    agentProperties = Map("description" -> "Converts text to lowercase")
  )
  
  // Classify text into categories (String -> Category)
  val classificationAgent = ComposableAgent[String, Category](
    processImpl = text => {
      // Simple keyword-based classification
      val categories = Map(
        "tech" -> Set("computer", "software", "hardware", "algorithm", "code"),
        "finance" -> Set("money", "banking", "investment", "financial", "economy"),
        "health" -> Set("doctor", "medical", "health", "treatment", "symptom")
      )
      
      val words = text.toLowerCase.split("\\W+").toSet
      val matches = categories.map { case (category, keywords) =>
        val matchCount = keywords.count(words.contains)
        (category, matchCount.toDouble / keywords.size)
      }
      
      val (bestCategory, confidence) = matches.maxBy(_._2)
      ZIO.succeed(Category(bestCategory, confidence))
    },
    agentCapabilities = Set("text-classification", "categorization"),
    inType = "String",
    outType = "Category",
    agentProperties = Map("description" -> "Classifies text into categories")
  )
  
  // Analyze sentiment in text (String -> Sentiment)
  val sentimentAnalysisAgent = ComposableAgent[String, Sentiment](
    processImpl = text => {
      // Simple keyword-based sentiment analysis
      val positiveWords = Set("good", "great", "excellent", "happy", "positive")
      val negativeWords = Set("bad", "terrible", "awful", "sad", "negative")
      
      val words = text.toLowerCase.split("\\W+")
      val totalWords = words.length.toDouble
      
      val positiveCount = words.count(positiveWords.contains) / totalWords
      val negativeCount = words.count(negativeWords.contains) / totalWords
      val neutralCount = 1.0 - (positiveCount + negativeCount)
      
      ZIO.succeed(Sentiment(positiveCount, negativeCount, neutralCount))
    },
    agentCapabilities = Set("sentiment-analysis", "emotion-detection"),
    inType = "String",
    outType = "Sentiment",
    agentProperties = Map("description" -> "Analyzes sentiment in text")
  )
  
  // Summarize text (String -> String)
  val summarizationAgent = ComposableAgent[String, String](
    processImpl = text => {
      // Simple extractive summarization - first sentence
      val sentences = text.split("[.!?]\\s+")
      val summary = if (sentences.nonEmpty) sentences(0) + "." else text
      ZIO.succeed(summary)
    },
    agentCapabilities = Set("summarization", "text-processing"),
    inType = "String",
    outType = "String",
    agentProperties = Map("description" -> "Creates summaries of text")
  )
  
  // Create a capability registry with document processing capabilities
  private def createCapabilityRegistry: UIO[CapabilityRegistry] = {
    // Create an empty registry
    val registry = CapabilityTaxonomy.createRegistry()
    
    // Define capabilities directly and catch any errors
    ZIO.attempt {
      // Register root capabilities
      registry.registerCapability(Capability(
        id = "document-processing",
        name = "Document Processing",
        description = "Process document content"
      ))
      
      registry.registerCapability(Capability(
        id = "text-processing",
        name = "Text Processing",
        description = "Process text content"
      ))
      
      // Register text-processing capabilities
      registry.registerCapability(Capability(
        id = "normalization",
        name = "Text Normalization",
        parentId = Some("text-processing"),
        description = "Normalize text (lowercase, stemming, etc.)"
      ))
      
      registry.registerCapability(Capability(
        id = "summarization",
        name = "Text Summarization",
        parentId = Some("text-processing"),
        description = "Generate summaries of text"
      ))
      
      registry.registerCapability(Capability(
        id = "text-classification",
        name = "Text Classification",
        parentId = Some("text-processing"),
        description = "Classify text into categories"
      ))
      
      registry.registerCapability(Capability(
        id = "sentiment-analysis",
        name = "Sentiment Analysis",
        parentId = Some("text-processing"),
        description = "Analyze sentiment in text"
      ))
      
      // Register document-processing capabilities
      registry.registerCapability(Capability(
        id = "text-extraction",
        name = "Text Extraction",
        parentId = Some("document-processing"),
        description = "Extract text from documents"
      ))
      
      // Return the registry with capabilities registered
      registry
    }.orDie  // Convert any errors to defects for this example
  }
  
  // Main app logic
  def run = {
    for {
      // Create capability registry and agent directory
      registry <- createCapabilityRegistry
      agentDirectory = ComposableAgentDirectory(registry)
      
      // Register all agents
      docToTextAgentId <- agentDirectory.registerAgent(documentToTextAgent)
      _ <- agentDirectory.registerAgent(lowercaseAgent)
      _ <- agentDirectory.registerAgent(classificationAgent)
      _ <- agentDirectory.registerAgent(sentimentAnalysisAgent)
      _ <- agentDirectory.registerAgent(summarizationAgent)
      
      // Print registered agents
      _ <- Console.printLine("=== Registered Agents ===")
      allAgents <- agentDirectory.findAgentsByCapabilities(Set.empty)
      _ <- ZIO.foreach(allAgents) { agent =>
        Console.printLine(s"Agent with capabilities: ${agent.capabilities.mkString(", ")}")
      }
      
      // Find agents by capability
      _ <- Console.printLine("\n=== Capability-Based Discovery ===")
      textProcessingAgents <- agentDirectory.findAgentsByCapabilities(Set("text-processing"))
      _ <- Console.printLine(s"Found ${textProcessingAgents.length} text processing agents")
      
      sentimentAgents <- agentDirectory.findAgentsByCapabilities(Set("sentiment-analysis"))
      _ <- Console.printLine(s"Found ${sentimentAgents.length} sentiment analysis agents")
      
      // Sample documents
      documents = List(
        Document("Positive Review", "The product is excellent and I'm very happy with it."),
        Document("Tech Article", "New computer algorithms are revolutionizing software development."),
        Document("Financial News", "Banking sector shows strong growth despite economic challenges.")
      )
      
      // Manual workflow composition
      _ <- Console.printLine("\n=== Manual Workflow Composition ===")
      
      // Create a document classification workflow (Document -> Category)
      documentClassificationWorkflow = documentToTextAgent.andThen(classificationAgent)
      
      // Process documents through the workflow
      _ <- ZIO.foreach(documents) { document =>
        for {
          category <- documentClassificationWorkflow.process(document)
          _ <- Console.printLine(s"Document '${document.title}' classified as: ${category.name} (confidence: ${category.confidence})")
        } yield ()
      }
      
      // Create more complex workflows
      _ <- Console.printLine("\n=== Complex Workflow Creation ===")
      
      // Document -> Text -> Sentiment workflow
      documentSentimentWorkflow = documentToTextAgent.andThen(sentimentAnalysisAgent)
      
      // Process documents through sentiment workflow
      _ <- ZIO.foreach(documents) { document =>
        for {
          sentiment <- documentSentimentWorkflow.process(document)
          dominant = if (sentiment.positive > sentiment.negative) "positive" else "negative"
          _ <- Console.printLine(s"Document '${document.title}' sentiment: ${dominant} (pos: ${sentiment.positive}, neg: ${sentiment.negative})")
        } yield ()
      }
      
      // Document -> Text -> Lowercase -> Summary workflow
      documentSummaryWorkflow = documentToTextAgent
        .andThen(lowercaseAgent)
        .andThen(summarizationAgent)
      
      // Process documents through summary workflow
      _ <- ZIO.foreach(documents) { document =>
        for {
          summary <- documentSummaryWorkflow.process(document)
          _ <- Console.printLine(s"Document '${document.title}' summary: ${summary}")
        } yield ()
      }
      
      // Automatic workflow discovery
      _ <- Console.printLine("\n=== Automatic Workflow Discovery ===")
      
      // Get the document-to-text agent
      docToTextWorkflow <- agentDirectory.getAgent(docToTextAgentId)
      
      // Then create a workflow for text summarization (String -> String)
      textToSummary <- agentDirectory.createWorkflow[String, String](
        "String", 
        "String",
        Set("summarization")
      )
      
      // Combine workflows to create a Document -> String (summary) workflow
      documentSummarizationWorkflow = docToTextWorkflow.get.asInstanceOf[ComposableAgent[Document, String]]
        .andThen(textToSummary.get.asInstanceOf[ComposableAgent[String, String]])
      
      // Process documents through automatically created workflow
      _ <- ZIO.foreach(documents) { document =>
        for {
          result <- documentSummarizationWorkflow.process(document)
          _ <- Console.printLine(s"Auto-discovered workflow result for '${document.title}': ${result}")
        } yield ()
      }
      
      // Parallel agent composition
      _ <- Console.printLine("\n=== Parallel Agent Composition ===")
      
      // Create a parallel agent that applies both classification and sentiment analysis
      parallelAnalysisAgent = ComposableAgent.parallel[String, Any, String](
        agents = List(
          classificationAgent.asInstanceOf[ComposableAgent[String, Any]], 
          sentimentAnalysisAgent.asInstanceOf[ComposableAgent[String, Any]]
        ),
        combiner = results => {
          val category = results(0).asInstanceOf[Category]
          val sentiment = results(1).asInstanceOf[Sentiment]
          s"Category: ${category.name} (${category.confidence}), " +
            s"Sentiment: ${if (sentiment.positive > sentiment.negative) "Positive" else "Negative"}"
        }
      )
      
      // Create a complete workflow: Document -> Text -> Parallel(Classification, Sentiment)
      completeWorkflow = documentToTextAgent.andThen(parallelAnalysisAgent)
      
      // Process documents through the parallel workflow
      _ <- ZIO.foreach(documents) { document =>
        for {
          result <- completeWorkflow.process(document)
          _ <- Console.printLine(s"Document '${document.title}' parallel analysis: ${result}")
        } yield ()
      }
      
    } yield ()
  }
}