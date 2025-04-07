package com.agenticai.core.capability

import zio._
import zio.test._
import zio.test.Assertion._
import com.agenticai.core.capability.CapabilityTaxonomy._

/**
 * Tests for the ComposableAgentDirectory implementation.
 */
object ComposableAgentDirectorySpec extends ZIOSpecDefault {

  // Sample test agents
  private val textToUpperAgent = ComposableAgent[String, String](
    processImpl = text => ZIO.succeed(text.toUpperCase),
    agentCapabilities = Set("text-processing", "uppercase"),
    inType = "String",
    outType = "String"
  )
  
  private val textToWordCountAgent = ComposableAgent[String, Int](
    processImpl = text => ZIO.succeed(text.split("\\s+").length),
    agentCapabilities = Set("text-processing", "word-count"),
    inType = "String",
    outType = "Int"
  )
  
  private val intToStringAgent = ComposableAgent[Int, String](
    processImpl = num => ZIO.succeed(num.toString),
    agentCapabilities = Set("conversion"),
    inType = "Int",
    outType = "String"
  )
  
  private val stringToHtmlAgent = ComposableAgent[String, String](
    processImpl = text => ZIO.succeed(s"<p>$text</p>"),
    agentCapabilities = Set("html-generation"),
    inType = "String",
    outType = "String"
  )

  // Test suite
  def spec = suite("ComposableAgentDirectorySpec")(
    
    // Test agent registration
    test("registerAgent should add an agent to the directory") {
      for {
        registry <- ZIO.succeed(CapabilityTaxonomy.createRegistry())
        _ <- registry.registerCapability(Capability(
          id = "text-processing", name = "Text Processing", description = "Process text"
        ))
        directory = ComposableAgentDirectory(registry)
        agentId <- directory.registerAgent(textToUpperAgent)
        agent <- directory.getAgent(agentId)
      } yield assert(agent.isDefined)(isTrue)
    },
    
    // Test finding agents by capabilities
    test("findAgentsByCapabilities should find agents with required capabilities") {
      for {
        registry <- ZIO.succeed(CapabilityTaxonomy.createRegistry())
        _ <- registry.registerCapability(Capability(
          id = "text-processing", name = "Text Processing", description = "Process text"
        ))
        _ <- registry.registerCapability(Capability(
          id = "uppercase", name = "Uppercase Conversion", 
          parentId = Some("text-processing"), description = "Convert text to uppercase"
        ))
        _ <- registry.registerCapability(Capability(
          id = "word-count", name = "Word Count", 
          parentId = Some("text-processing"), description = "Count words in text"
        ))
        _ <- registry.registerCapability(Capability(
          id = "conversion", name = "Type Conversion", description = "Convert between types"
        ))
        
        directory = ComposableAgentDirectory(registry)
        _ <- directory.registerAgent(textToUpperAgent)
        _ <- directory.registerAgent(textToWordCountAgent)
        _ <- directory.registerAgent(intToStringAgent)
        textProcessingAgents <- directory.findAgentsByCapabilities(Set("text-processing"))
      } yield assert(textProcessingAgents.size)(equalTo(2))
    },
    
    // Test finding agents by specific capability
    test("findAgentsByCapabilities should find agents with specific capabilities") {
      for {
        registry <- ZIO.succeed(CapabilityTaxonomy.createRegistry())
        _ <- registry.registerCapability(Capability(
          id = "text-processing", name = "Text Processing", description = "Process text"
        ))
        _ <- registry.registerCapability(Capability(
          id = "uppercase", name = "Uppercase Conversion", 
          parentId = Some("text-processing"), description = "Convert text to uppercase"
        ))
        _ <- registry.registerCapability(Capability(
          id = "conversion", name = "Type Conversion", description = "Convert between types"
        ))
        
        directory = ComposableAgentDirectory(registry)
        _ <- directory.registerAgent(textToUpperAgent)
        _ <- directory.registerAgent(intToStringAgent)
        uppercaseAgents <- directory.findAgentsByCapabilities(Set("uppercase"))
      } yield assert(uppercaseAgents.size)(equalTo(1))
    },
    
    // Test capability hierarchy matching
    test("findAgentsByCapabilities should respect capability hierarchy") {
      for {
        registry <- ZIO.succeed(CapabilityTaxonomy.createRegistry())
        _ <- registry.registerCapability(Capability(
          id = "text-processing", name = "Text Processing", description = "Process text"
        ))
        _ <- registry.registerCapability(Capability(
          id = "uppercase", name = "Uppercase Conversion", 
          parentId = Some("text-processing"), description = "Convert text to uppercase"
        ))
        _ <- registry.registerCapability(Capability(
          id = "conversion", name = "Type Conversion", description = "Convert between types"
        ))
        
        directory = ComposableAgentDirectory(registry)
        _ <- directory.registerAgent(textToUpperAgent) // has uppercase capability (child of text-processing)
        _ <- directory.registerAgent(intToStringAgent) // has conversion capability
        textProcessingAgents <- directory.findAgentsByCapabilities(Set("text-processing"))
      } yield assert(textProcessingAgents.size)(equalTo(1)) &&
             assert(textProcessingAgents.head.capabilities)(contains("uppercase"))
    },
    
    // Test finding agents by input/output types
    test("findAgentsByTypes should find agents with specified input and output types") {
      for {
        registry <- ZIO.succeed(CapabilityTaxonomy.createRegistry())
        directory = ComposableAgentDirectory(registry)
        _ <- directory.registerAgent(textToUpperAgent)
        _ <- directory.registerAgent(textToWordCountAgent)
        _ <- directory.registerAgent(intToStringAgent)
        _ <- directory.registerAgent(stringToHtmlAgent)
        stringToStringAgents <- directory.findAgentsByTypes("String", "String")
      } yield assert(stringToStringAgents.size)(equalTo(2)) // textToUpperAgent and stringToHtmlAgent
    },
    
    // Test creating workflows with direct matches
    test("createWorkflow should find direct matches when available") {
      for {
        registry <- ZIO.succeed(CapabilityTaxonomy.createRegistry())
        directory = ComposableAgentDirectory(registry)
        _ <- directory.registerAgent(textToUpperAgent)
        _ <- directory.registerAgent(textToWordCountAgent)
        _ <- directory.registerAgent(intToStringAgent)
        workflow <- directory.createWorkflow[String, String]("String", "String")
      } yield assert(workflow.isDefined)(isTrue) &&
             assert(workflow.get.inputType)(equalTo("String")) &&
             assert(workflow.get.outputType)(equalTo("String"))
    },
    
    // Test creating workflows with composition
    test("createWorkflow should compose agents when needed") {
      for {
        registry <- ZIO.succeed(CapabilityTaxonomy.createRegistry())
        _ <- registry.registerCapability(Capability(
          id = "word-count", name = "Word Count", description = "Count words in text"
        ))
        
        directory = ComposableAgentDirectory(registry)
        _ <- directory.registerAgent(textToWordCountAgent) // String -> Int
        _ <- directory.registerAgent(intToStringAgent)     // Int -> String
        
        // This should create a workflow: String -> Int -> String
        workflow <- directory.createWorkflow[String, String](
          "String", "String", Set("word-count")
        )
        result <- workflow.get.process("hello world")
      } yield assert(result)(equalTo("2")) // "hello world" has 2 words
    },
    
    // Test workflow creation failure when no path exists
    test("createWorkflow should return None when no valid path exists") {
      for {
        registry <- ZIO.succeed(CapabilityTaxonomy.createRegistry())
        directory = ComposableAgentDirectory(registry)
        _ <- directory.registerAgent(textToUpperAgent)
        _ <- directory.registerAgent(intToStringAgent)
        workflow <- directory.createWorkflow[Int, Int]("Int", "Int")
      } yield assert(workflow.isDefined)(isFalse)
    }
  )
}