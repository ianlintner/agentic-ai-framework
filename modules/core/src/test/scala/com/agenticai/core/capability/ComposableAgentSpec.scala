package com.agenticai.core.capability

import zio.*
import zio.test.*
import zio.test.Assertion.*
import com.agenticai.core.capability.CapabilityTaxonomy.*

/** Tests for the ComposableAgent implementation.
  *
  * These tests verify that composable agents correctly:
  *   - Process inputs
  *   - Chain with other agents
  *   - Combine in parallel
  *   - Preserve capabilities and properties during composition
  */
object ComposableAgentSpec extends ZIOSpecDefault:

  // Sample test agents
  private val upperCaseAgent = ComposableAgent[String, String](
    processImpl = text => ZIO.succeed(text.toUpperCase),
    agentCapabilities = Set("text-transformation", "uppercase"),
    inType = "String",
    outType = "String",
    agentProperties = Map("description" -> "Converts text to uppercase")
  )

  private val appendExclamationAgent = ComposableAgent[String, String](
    processImpl = text => ZIO.succeed(text + "!"),
    agentCapabilities = Set("text-transformation", "punctuation"),
    inType = "String",
    outType = "String",
    agentProperties = Map("description" -> "Adds exclamation marks")
  )

  private val countWordsAgent = ComposableAgent[String, Int](
    processImpl = text => ZIO.succeed(text.split("\\s+").length),
    agentCapabilities = Set("text-analysis", "word-count"),
    inType = "String",
    outType = "Int",
    agentProperties = Map("description" -> "Counts words in text")
  )

  private val intToStringAgent = ComposableAgent[Int, String](
    processImpl = num => ZIO.succeed(num.toString),
    agentCapabilities = Set("conversion", "int-to-string"),
    inType = "Int",
    outType = "String",
    agentProperties = Map("description" -> "Converts integers to strings")
  )

  // Test suite
  def spec = suite("ComposableAgentSpec")(
    // Test basic agent processing
    test("ComposableAgent should process inputs correctly") {
      for result <- upperCaseAgent.process("hello world")
      yield assert(result)(equalTo("HELLO WORLD"))
    },

    // Test sequential composition
    test("andThen should chain agents sequentially") {
      val chainedAgent = upperCaseAgent.andThen(appendExclamationAgent)

      for result <- chainedAgent.process("hello world")
      yield assert(result)(equalTo("HELLO WORLD!"))
    },

    // Test capability aggregation in sequential composition
    test("andThen should combine capabilities of both agents") {
      val chainedAgent = upperCaseAgent.andThen(appendExclamationAgent)

      val expectedCapabilities = Set(
        "text-transformation",
        "uppercase",
        "punctuation"
      )

      assert(chainedAgent.capabilities)(hasSameElements(expectedCapabilities))
    },

    // Test property preservation in sequential composition
    test("andThen should preserve properties from both agents") {
      val chainedAgent = upperCaseAgent.andThen(appendExclamationAgent)

      val upperCaseDescription         = "Converts text to uppercase"
      val appendExclamationDescription = "Adds exclamation marks"

      assert(chainedAgent.properties.get("description").isDefined)(isTrue) &&
      (
        assert(chainedAgent.properties.get("description").get)(equalTo(upperCaseDescription)) ||
          assert(chainedAgent.properties.get("description").get)(
            equalTo(appendExclamationDescription)
          )
      )
    },

    // Test multi-step chaining
    test("andThen should support multi-step chaining with type safety") {
      val threeStepAgent = upperCaseAgent
        .andThen(appendExclamationAgent)
        .andThen(countWordsAgent)
        .andThen(intToStringAgent)

      for result <- threeStepAgent.process("hello world")
      yield assert(result)(equalTo("2"))
    },

    // Test parallel composition
    test("parallel should apply multiple agents to the same input") {
      val parallelAgent = ComposableAgent.parallel[String, String, String](
        agents = List(upperCaseAgent, appendExclamationAgent),
        combiner = results => results.mkString(" + ")
      )

      for result <- parallelAgent.process("hello")
      yield assert(result)(equalTo("HELLO + hello!"))
    },

    // Test capability aggregation in parallel composition
    test("parallel should combine capabilities from all agents") {
      val parallelAgent = ComposableAgent.parallel[String, String, String](
        agents = List(upperCaseAgent, appendExclamationAgent),
        combiner = results => results.mkString(" + ")
      )

      val expectedCapabilities = Set(
        "text-transformation",
        "uppercase",
        "punctuation"
      )

      assert(parallelAgent.capabilities)(hasSameElements(expectedCapabilities))
    },

    // Test error handling in agent processing
    test("agents should propagate errors") {
      val failingAgent = ComposableAgent[String, String](
        processImpl = _ => ZIO.fail(new RuntimeException("Deliberate test failure")),
        agentCapabilities = Set("failure"),
        inType = "String",
        outType = "String"
      )

      val chainedWithFailure = upperCaseAgent.andThen(failingAgent)

      for result <- chainedWithFailure.process("hello").exit
      yield assert(result)(fails(hasMessage(equalTo("Deliberate test failure"))))
    },

    // Test for proper type handling
    test("agents should handle type conversions correctly") {
      // Create a chain that involves type conversion
      val stringToIntToStringChain = countWordsAgent.andThen(intToStringAgent)

      for result <- stringToIntToStringChain.process("one two three four")
      yield assert(result)(equalTo("4"))
    }
  )
