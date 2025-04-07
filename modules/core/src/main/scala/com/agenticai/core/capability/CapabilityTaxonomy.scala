package com.agenticai.core.capability

import zio._

/**
 * A structured taxonomy for agent capabilities.
 * 
 * Provides a hierarchical organization of capabilities that enables:
 * 1. More precise capability matching
 * 2. Capability inheritance (agents with a capability automatically have parent capabilities)
 * 3. Capability relationships and compatibility detection
 */
object CapabilityTaxonomy {

  /**
   * Represents a capability in the taxonomy.
   * 
   * @param id Unique identifier for the capability
   * @param name Human-readable name for the capability
   * @param parentId Optional parent capability ID (for hierarchy)
   * @param description Human-readable description of what the capability represents
   * @param tags Optional tags for additional categorization 
   */
  case class Capability(
    id: String,
    name: String,
    parentId: Option[String] = None,
    description: String = "",
    tags: Set[String] = Set.empty
  ) {
    def isChildOf(other: Capability): Boolean = parentId.contains(other.id)
    def hasTag(tag: String): Boolean = tags.contains(tag)
  }

  /**
   * A registry of capabilities with lookups and hierarchy traversal.
   */
  trait CapabilityRegistry {
    /**
     * Register a new capability in the taxonomy.
     */
    def registerCapability(capability: Capability): Task[Unit]
    
    /**
     * Get a capability by its ID.
     */
    def getCapability(id: String): Task[Option[Capability]]
    
    /**
     * Find all capabilities that match the given criteria.
     */
    def findCapabilities(
      tags: Set[String] = Set.empty,
      parentId: Option[String] = None
    ): Task[List[Capability]]
    
    /**
     * Get all parent capabilities of the given capability.
     */
    def getParentCapabilities(capabilityId: String): Task[List[Capability]]
    
    /**
     * Get all child capabilities of the given capability.
     */
    def getChildCapabilities(capabilityId: String): Task[List[Capability]]
    
    /**
     * Check if an agent with the given capabilities can fulfill a requirement.
     * This handles capability hierarchy, so an agent with a more specific capability
     * can fulfill a requirement for a more general capability.
     */
    def canFulfill(
      agentCapabilities: Set[String], 
      requiredCapabilities: Set[String]
    ): Task[Boolean]
  }
  
  /**
   * In-memory implementation of the capability registry.
   */
  class InMemoryCapabilityRegistry extends CapabilityRegistry {
    private val capabilities = new java.util.concurrent.ConcurrentHashMap[String, Capability]()
    
    override def registerCapability(capability: Capability): Task[Unit] = {
      ZIO.succeed(capabilities.put(capability.id, capability)).unit
    }
    
    override def getCapability(id: String): Task[Option[Capability]] = {
      ZIO.succeed(Option(capabilities.get(id)))
    }
    
    override def findCapabilities(
      tags: Set[String] = Set.empty,
      parentId: Option[String] = None
    ): Task[List[Capability]] = {
      ZIO.succeed {
        import scala.jdk.CollectionConverters._
        capabilities.values().asScala.filter { capability =>
          (tags.isEmpty || tags.exists(capability.hasTag)) &&
          (parentId.isEmpty || capability.parentId == parentId)
        }.toList
      }
    }
    
    override def getParentCapabilities(capabilityId: String): Task[List[Capability]] = {
      def collectParents(id: String, acc: List[Capability] = List.empty): Task[List[Capability]] = {
        getCapability(id).flatMap {
          case Some(capability) => 
            capability.parentId match {
              case Some(parentId) => 
                getCapability(parentId).flatMap {
                  case Some(parent) => collectParents(parentId, parent :: acc)
                  case None => ZIO.succeed(acc)
                }
              case None => ZIO.succeed(acc)
            }
          case None => ZIO.succeed(acc)
        }
      }
      
      collectParents(capabilityId)
    }
    
    override def getChildCapabilities(capabilityId: String): Task[List[Capability]] = {
      ZIO.succeed {
        import scala.jdk.CollectionConverters._
        capabilities.values().asScala.filter { capability =>
          capability.parentId.contains(capabilityId)
        }.toList
      }
    }
    
    override def canFulfill(
      agentCapabilities: Set[String], 
      requiredCapabilities: Set[String]
    ): Task[Boolean] = {
      if (requiredCapabilities.isEmpty) {
        ZIO.succeed(true)
      } else if (agentCapabilities.isEmpty) {
        ZIO.succeed(false)
      } else {
        // Direct capability check
        val directMatch = requiredCapabilities.subsetOf(agentCapabilities)
        if (directMatch) {
          ZIO.succeed(true)
        } else {
          // Check hierarchy (an agent with a more specific capability can fulfill
          // a requirement for a more general capability in its ancestry)
          ZIO.foreach(requiredCapabilities) { required =>
            ZIO.foreach(agentCapabilities) { agent =>
              getParentCapabilities(agent).map(parents => 
                parents.exists(_.id == required)
              )
            }.map(_.exists(identity))
          }.map(_.forall(identity))
        }
      }
    }
  }
  
  /**
   * Create a new in-memory capability registry.
   */
  def createRegistry(): CapabilityRegistry = new InMemoryCapabilityRegistry()
  
  /**
   * Create a registry with common predefined capabilities.
   */
  def createDefaultRegistry(): Task[CapabilityRegistry] = {
    val registry = createRegistry()
    
    for {
      // Define root capabilities
      _ <- registry.registerCapability(Capability(
        id = "nlp",
        name = "Natural Language Processing",
        description = "Process and understand human language"
      ))
      
      _ <- registry.registerCapability(Capability(
        id = "vision",
        name = "Computer Vision",
        description = "Process and understand visual information"
      ))
      
      _ <- registry.registerCapability(Capability(
        id = "reasoning",
        name = "Reasoning",
        description = "Apply logical reasoning to solve problems"
      ))
      
      _ <- registry.registerCapability(Capability(
        id = "knowledge",
        name = "Knowledge",
        description = "Access and utilize stored knowledge"
      ))
      
      // Define NLP subcapabilities
      _ <- registry.registerCapability(Capability(
        id = "translation",
        name = "Translation",
        parentId = Some("nlp"),
        description = "Translate text between languages",
        tags = Set("language", "text")
      ))
      
      _ <- registry.registerCapability(Capability(
        id = "sentiment-analysis",
        name = "Sentiment Analysis",
        parentId = Some("nlp"),
        description = "Analyze sentiment in text",
        tags = Set("text", "emotion")
      ))
      
      _ <- registry.registerCapability(Capability(
        id = "summarization",
        name = "Summarization",
        parentId = Some("nlp"),
        description = "Create concise summaries of text",
        tags = Set("text", "condensation")
      ))
      
      _ <- registry.registerCapability(Capability(
        id = "extraction",
        name = "Information Extraction",
        parentId = Some("nlp"),
        description = "Extract structured information from text",
        tags = Set("text", "structure")
      ))
      
      // Define vision subcapabilities
      _ <- registry.registerCapability(Capability(
        id = "object-detection",
        name = "Object Detection",
        parentId = Some("vision"),
        description = "Detect objects in images",
        tags = Set("detection", "image")
      ))
      
      _ <- registry.registerCapability(Capability(
        id = "image-classification",
        name = "Image Classification",
        parentId = Some("vision"),
        description = "Classify images into categories",
        tags = Set("classification", "image")
      ))
      
      // Define reasoning subcapabilities
      _ <- registry.registerCapability(Capability(
        id = "planning",
        name = "Planning",
        parentId = Some("reasoning"),
        description = "Create plans to achieve goals",
        tags = Set("goals", "strategy")
      ))
      
      _ <- registry.registerCapability(Capability(
        id = "decision-making",
        name = "Decision Making",
        parentId = Some("reasoning"),
        description = "Make decisions based on criteria",
        tags = Set("choice", "optimization")
      ))
    } yield registry
  }
}