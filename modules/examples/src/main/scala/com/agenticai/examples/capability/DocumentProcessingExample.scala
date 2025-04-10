package com.agenticai.examples.capability

import zio.*
import com.agenticai.core.capability.{ComposableAgent, ComposableAgentDirectory}
import com.agenticai.core.capability.CapabilityTaxonomy
import com.agenticai.core.capability.CapabilityTaxonomy.{Capability, CapabilityRegistry}

/** Example demonstrating the capability-based agent composition for document processing.
  *
  * This example shows how to:
  *   1. Create specialized agents with specific capabilities 2. Register them in an agent directory
  *      3. Discover agents by capability 4. Compose agents into workflows 5. Process documents
  *      through the workflows
  */
object DocumentProcessingExample extends ZIOAppDefault:

  // Define document processing data types
  case class Document(title: String, content: String)

  case class SentimentScore(positive: Double, negative: Double, neutral: Double):

    def dominantSentiment: String =
      if positive > negative && positive > neutral then "positive"
      else if negative > positive && negative > neutral then "negative"
      else "neutral"

    override def toString: String =
      s"Sentiment(positive=$positive, negative=$negative, neutral=$neutral, dominant=$dominantSentiment)"

  // Create specialized document processing agents

  // Agent to extract text from a document
  val textExtractionAgent = ComposableAgent[Document, String](
    processImpl = doc => ZIO.succeed(doc.content),
    agentCapabilities = Set("text-extraction"),
    inType = "Document",
    outType = "String",
    agentProperties = Map("description" -> "Extracts text content from documents")
  )

  // Agent to perform sentiment analysis on text
  val sentimentAnalysisAgent = ComposableAgent[String, SentimentScore](
    processImpl = text =>
      // Simple keyword-based sentiment analysis
      val words = text.toLowerCase.split("""\W+""")
      val positiveWords =
        Set("good", "great", "excellent", "happy", "positive", "wonderful", "amazing")
      val negativeWords =
        Set("bad", "terrible", "awful", "sad", "negative", "horrible", "disappointing")

      val positive = words.count(positiveWords.contains) / words.length.toDouble
      val negative = words.count(negativeWords.contains) / words.length.toDouble
      val neutral  = 1.0 - (positive + negative)

      ZIO.succeed(SentimentScore(positive, negative, neutral))
    ,
    agentCapabilities = Set("sentiment-analysis"),
    inType = "String",
    outType = "SentimentScore",
    agentProperties = Map("description" -> "Analyzes sentiment in text")
  )

  // Agent to count words in text
  val wordCountAgent = ComposableAgent[String, Int](
    processImpl = text => ZIO.succeed(text.split("""\W+""").length),
    agentCapabilities = Set("word-count"),
    inType = "String",
    outType = "Int",
    agentProperties = Map("description" -> "Counts words in text")
  )

  // Agent to summarize text
  val summarizationAgent = ComposableAgent[String, String](
    processImpl = text =>
      // Simple summarization by taking the first sentence
      val firstSentence = text.split("[.!?]")(0).trim + "."
      ZIO.succeed(firstSentence)
    ,
    agentCapabilities = Set("summarization"),
    inType = "String",
    outType = "String",
    agentProperties = Map("description" -> "Summarizes text content")
  )

  // Create capability registry with document processing capabilities
  private def createCapabilityRegistry: UIO[CapabilityRegistry] = {
    ZIO.succeed {
      val registry = CapabilityTaxonomy.createRegistry()
      
      // We'll use traditional try-catch for error handling since we just want to ignore errors
      try {
        // Register all capabilities, catching any exceptions
        registry.registerCapability(
          Capability(
            id = "document-processing",
            name = "Document Processing",
            description = "Process document content"
          )
        )
        
        registry.registerCapability(
          Capability(
            id = "text-extraction",
            name = "Text Extraction",
            parentId = Some("document-processing"),
            description = "Extract text from documents"
          )
        )
        
        registry.registerCapability(
          Capability(
            id = "sentiment-analysis",
            name = "Sentiment Analysis",
            parentId = Some("document-processing"),
            description = "Analyze sentiment in text"
          )
        )
        
        registry.registerCapability(
          Capability(
            id = "word-count",
            name = "Word Count",
            parentId = Some("document-processing"),
            description = "Count words in text"
          )
        )
        
        registry.registerCapability(
          Capability(
            id = "summarization",
            name = "Summarization",
            parentId = Some("document-processing"),
            description = "Summarize text content"
          )
        )
      } catch {
        case _: Throwable => () // Just ignore errors for this example
      }
      
      registry
    }
    }

  // Sample documents to process
  val documents = List(
    Document(
      "Positive Review",
      "The product is excellent! I'm very happy with my purchase. The quality is amazing and it works wonderfully."
    ),
    Document(
      "Negative Review",
      "I'm disappointed with this purchase. The product is terrible and doesn't work as advertised. Horrible experience."
    ),
    Document(
      "Neutral Information",
      "The product comes in three colors: red, blue, and green. It weighs approximately 500 grams and is made of plastic."
    )
  )

  // Main application logic
  def run =
    for
      // Create registry and directory
      registry <- createCapabilityRegistry
      directory = ComposableAgentDirectory(registry)

      // Register agents
      _ <- directory.registerAgent(textExtractionAgent)
      _ <- directory.registerAgent(sentimentAnalysisAgent)
      _ <- directory.registerAgent(wordCountAgent)
      _ <- directory.registerAgent(summarizationAgent)

      // Display available agents
      allAgents <- directory.findAgentsByCapabilities(Set.empty)
      _         <- Console.printLine(s"Available agents: ${allAgents.length}")
      _ <- ZIO.foreach(allAgents) { agent =>
        Console.printLine(s"  - Agent with capabilities: ${agent.capabilities.mkString(", ")}")
      }

      // Find agents by capability
      sentimentAgents <- directory.findAgentsByCapabilities(Set("sentiment-analysis"))
      _ <- Console.printLine(s"\nFound ${sentimentAgents.length} sentiment analysis agents")

      // Demonstrate manual composition
      _ <- Console.printLine("\n=== Manual Workflow Composition ===")

      // Create document analysis workflow manually
      docToSentimentWorkflow = textExtractionAgent.andThen(sentimentAnalysisAgent)

      // Process documents through the workflow
      _ <- ZIO.foreach(documents) { doc =>
        for
          _         <- Console.printLine(s"\nAnalyzing document: ${doc.title}")
          sentiment <- docToSentimentWorkflow.process(doc)
          _         <- Console.printLine(s"Sentiment: $sentiment")
        yield ()
      }

      // Demonstrate automatic workflow creation
      _ <- Console.printLine("\n=== Automatic Workflow Discovery ===")

      // Automatically create a document summarization workflow
      docToSummaryWorkflow <- directory.createWorkflow[Document, String](
        "Document",
        "String",
        Set("text-extraction", "summarization")
      )

      // Process documents through the discovered workflow
      _ <- ZIO.foreach(documents) { doc =>
        for
          _       <- Console.printLine(s"\nSummarizing document: ${doc.title}")
          summary <- docToSummaryWorkflow.get.process(doc)
          _       <- Console.printLine(s"Summary: $summary")
        yield ()
      }

      // Demonstrate parallel agent composition
      _ <- Console.printLine("\n=== Parallel Agent Composition ===")

      // Create multi-analysis workflow that runs sentiment and word count in parallel
      wordCountAndSentimentWorkflow = ComposableAgent.parallel[String, Any, String](
        agents = List(
          wordCountAgent.asInstanceOf[ComposableAgent[String, Any]],
          sentimentAnalysisAgent.asInstanceOf[ComposableAgent[String, Any]]
        ),
        combiner = results =>
          s"Word count: ${results(0)}, Sentiment: ${results(1).asInstanceOf[SentimentScore].dominantSentiment}"
      )

      // Complete workflow from document to combined analysis
      completeWorkflow = textExtractionAgent.andThen(wordCountAndSentimentWorkflow)

      // Process documents through the parallel workflow
      _ <- ZIO.foreach(documents) { doc =>
        for
          _      <- Console.printLine(s"\nPerforming multi-analysis on document: ${doc.title}")
          result <- completeWorkflow.process(doc)
          _      <- Console.printLine(result)
        yield ()
      }
    yield ()
