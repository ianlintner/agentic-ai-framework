package com.agenticai.core.capability

import zio._
import zio.test._
import zio.test.Assertion._
import com.agenticai.core.capability.CapabilityTaxonomy._

/**
 * Tests for the CapabilityTaxonomy implementation.
 * 
 * These tests verify that the capability taxonomy correctly handles:
 * - Registration of capabilities
 * - Parent-child relationships
 * - Capability lookups
 * - Capability matching
 */
object CapabilityTaxonomySpec extends ZIOSpecDefault {

  // Sample test capabilities
  private val nlpCapability = Capability(
    id = "nlp",
    name = "Natural Language Processing",
    description = "Process and understand human language"
  )
  
  private val translationCapability = Capability(
    id = "translation",
    name = "Translation",
    parentId = Some("nlp"),
    description = "Translate text between languages",
    tags = Set("language", "text")
  )
  
  private val sentimentCapability = Capability(
    id = "sentiment",
    name = "Sentiment Analysis",
    parentId = Some("nlp"),
    description = "Analyze sentiment in text",
    tags = Set("text", "emotion")
  )
  
  private val machineTranslationCapability = Capability(
    id = "machine-translation",
    name = "Machine Translation",
    parentId = Some("translation"),
    description = "Automatic translation using machine learning",
    tags = Set("ml", "language")
  )

  // Test suite
  def spec = suite("CapabilityTaxonomySpec")(
    
    // Test capability registration
    test("registerCapability should add a capability to the registry") {
      for {
        registry <- ZIO.succeed(new InMemoryCapabilityRegistry)
        _ <- registry.registerCapability(nlpCapability)
        result <- registry.getCapability("nlp")
      } yield assert(result)(isSome(equalTo(nlpCapability)))
    },
    
    // Test parent-child relationships
    test("parent-child relationship should be established correctly") {
      for {
        registry <- ZIO.succeed(new InMemoryCapabilityRegistry)
        _ <- registry.registerCapability(nlpCapability)
        _ <- registry.registerCapability(translationCapability)
        childCapabilities <- registry.getChildCapabilities("nlp")
      } yield assert(childCapabilities.map(_.id))(contains("translation"))
    },
    
    // Test fetching parent capabilities
    test("getParentCapabilities should return all ancestors") {
      for {
        registry <- ZIO.succeed(new InMemoryCapabilityRegistry)
        _ <- registry.registerCapability(nlpCapability)
        _ <- registry.registerCapability(translationCapability)
        _ <- registry.registerCapability(machineTranslationCapability)
        parentCapabilities <- registry.getParentCapabilities("machine-translation")
      } yield assert(parentCapabilities.map(_.id))(hasSameElements(List("translation", "nlp")))
    },
    
    // Test finding capabilities by tag
    test("findCapabilities should find capabilities by tags") {
      for {
        registry <- ZIO.succeed(new InMemoryCapabilityRegistry)
        _ <- registry.registerCapability(nlpCapability)
        _ <- registry.registerCapability(translationCapability)
        _ <- registry.registerCapability(sentimentCapability)
        emotionTaggedCapabilities <- registry.findCapabilities(tags = Set("emotion"))
      } yield assert(emotionTaggedCapabilities.map(_.id))(contains("sentiment"))
    },
    
    // Test finding capabilities by parent
    test("findCapabilities should find capabilities by parent") {
      for {
        registry <- ZIO.succeed(new InMemoryCapabilityRegistry)
        _ <- registry.registerCapability(nlpCapability)
        _ <- registry.registerCapability(translationCapability)
        _ <- registry.registerCapability(sentimentCapability)
        nlpChildCapabilities <- registry.findCapabilities(parentId = Some("nlp"))
      } yield assert(nlpChildCapabilities.map(_.id))(
        hasSameElements(List("translation", "sentiment"))
      )
    },
    
    // Test capability fulfillment
    test("canFulfill should check if an agent can fulfill capability requirements (direct match)") {
      for {
        registry <- ZIO.succeed(new InMemoryCapabilityRegistry)
        _ <- registry.registerCapability(nlpCapability)
        _ <- registry.registerCapability(translationCapability)
        result <- registry.canFulfill(
          agentCapabilities = Set("translation"),
          requiredCapabilities = Set("translation")
        )
      } yield assert(result)(isTrue)
    },
    
    // Test capability fulfillment with inheritance
    test("canFulfill should check if an agent can fulfill capability requirements (inheritance)") {
      for {
        registry <- ZIO.succeed(new InMemoryCapabilityRegistry)
        _ <- registry.registerCapability(nlpCapability)
        _ <- registry.registerCapability(translationCapability)
        _ <- registry.registerCapability(machineTranslationCapability)
        result <- registry.canFulfill(
          agentCapabilities = Set("machine-translation"),
          requiredCapabilities = Set("nlp")
        )
      } yield assert(result)(isTrue)
    },
    
    // Test capability fulfillment with multiple requirements
    test("canFulfill should handle multiple required capabilities") {
      for {
        registry <- ZIO.succeed(new InMemoryCapabilityRegistry)
        _ <- registry.registerCapability(nlpCapability)
        _ <- registry.registerCapability(translationCapability)
        _ <- registry.registerCapability(sentimentCapability)
        result <- registry.canFulfill(
          agentCapabilities = Set("translation", "sentiment"),
          requiredCapabilities = Set("nlp")
        )
      } yield assert(result)(isTrue)
    },
    
    // Test capability fulfillment failure
    test("canFulfill should return false when requirements can't be met") {
      for {
        registry <- ZIO.succeed(new InMemoryCapabilityRegistry)
        _ <- registry.registerCapability(nlpCapability)
        _ <- registry.registerCapability(translationCapability)
        result <- registry.canFulfill(
          agentCapabilities = Set("sentiment"),
          requiredCapabilities = Set("translation")
        )
      } yield assert(result)(isFalse)
    },
    
    // Test default registry creation
    test("createDefaultRegistry should create a registry with predefined capabilities") {
      for {
        registry <- CapabilityTaxonomy.createDefaultRegistry()
        nlpCap <- registry.getCapability("nlp")
        translationCap <- registry.getCapability("translation")
        summarizationCap <- registry.getCapability("summarization")
      } yield assert(nlpCap.isDefined)(isTrue) && 
             assert(translationCap.isDefined)(isTrue) &&
             assert(summarizationCap.isDefined)(isTrue)
    }
  )
}