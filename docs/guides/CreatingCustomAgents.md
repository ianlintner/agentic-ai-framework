# Creating Custom Agents: Developer's Guide

This guide provides a comprehensive overview of how to create custom agents using the Agentic AI Framework. It covers the architectural fundamentals, implementation patterns, and best practices for developing effective AI agents.

## Table of Contents

1. [Agent Architecture Fundamentals](#agent-architecture-fundamentals)
2. [Creating Basic Agents](#creating-basic-agents)
3. [Specialized Agent Types](#specialized-agent-types)
4. [Integration with Memory Systems](#integration-with-memory-systems)
5. [Composing Agents](#composing-agents)
6. [Advanced Agent Patterns](#advanced-agent-patterns)
7. [Testing and Debugging](#testing-and-debugging)
8. [Performance Optimization](#performance-optimization)
9. [Deployment Considerations](#deployment-considerations)

## Agent Architecture Fundamentals

### Core Agent Interface

At the heart of the framework is the `Agent` interface, which defines the fundamental contract for all agents:

```scala
trait Agent[I, O] {
  def process(input: I): Task[O]
}
```

This simple yet powerful interface enables:
- **Type Safety**: Input and output types are clearly defined
- **Effect Management**: Returns a ZIO `Task` for proper effect handling
- **Composability**: Allows agents to be combined into more complex structures

### Key Agent Principles

1. **Functional Purity**: Agents should be functionally pure, with behavior determined by their inputs
2. **Composability**: Designed to be composed with other agents
3. **Effect Encapsulation**: All side effects encapsulated in ZIO
4. **Statelessness**: Core agents should be stateless (state managed through memory systems)

## Creating Basic Agents

### Simple Functional Agent

The most straightforward way to create an agent is by implementing the `Agent` trait directly:

```scala
import zio._
import com.agenticai.core.agent.Agent

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
```

### Function-Based Agent

For simple cases, you can create an agent from a function:

```scala
import com.agenticai.core.agent.Agent

val reverseAgent: Agent[String, String] = Agent.fromFunction { input =>
  ZIO.succeed(input.reverse)
}
```

### Using Agent Combinators

The framework provides functional combinators to create and transform agents:

```scala
import com.agenticai.core.agent.Agent

// Create from pure function
val uppercaseAgent = Agent.pure[String, String](str => str.toUpperCase())

// Map the output
val countAgent = uppercaseAgent.map(str => str.length)

// Chain agents together
val processAgent = uppercaseAgent.flatMap(upper => 
  Agent.pure[String, Int](str => str.length).map(len => s"$upper has length $len")
)
```

## Specialized Agent Types

### LLM-Based Agent

Creating an agent that uses a language model:

```scala
import com.agenticai.core.agent.Agent
import com.agenticai.core.llm.ClaudeAgent

class TextSummarizer extends Agent[String, String] {
  private val llm = ClaudeAgent() // Use default configuration

  def process(input: String): Task[String] = {
    val prompt = s"""
      |Please summarize the following text in a concise manner:
      |
      |$input
      |
      |Summary:
      """.stripMargin
    
    llm.process(prompt).map(response => response.trim)
  }
}
```

### Multi-Stage Reasoning Agent

Implementing an agent with multi-stage reasoning:

```scala
class ReasoningAgent extends Agent[String, String] {
  private val llm = ClaudeAgent()
  
  def process(input: String): Task[String] = {
    for {
      // Stage 1: Analysis
      analysis <- analyzeQuestion(input)
      
      // Stage 2: Research
      research <- gatherInformation(input, analysis)
      
      // Stage 3: Reasoning
      reasoning <- developReasoning(input, analysis, research)
      
      // Stage 4: Final answer
      answer <- generateAnswer(input, reasoning)
    } yield answer
  }
  
  private def analyzeQuestion(question: String): Task[String] = {
    val prompt = s"Question: $question\n\nAnalyze what this question is asking for:"
    llm.process(prompt)
  }
  
  private def gatherInformation(question: String, analysis: String): Task[String] = {
    val prompt = s"Question: $question\n\nAnalysis: $analysis\n\nWhat information is needed to answer this question?"
    llm.process(prompt)
  }
  
  private def developReasoning(question: String, analysis: String, research: String): Task[String] = {
    val prompt = s"Question: $question\n\nAnalysis: $analysis\n\nInformation: $research\n\nDevelop a step-by-step reasoning process:"
    llm.process(prompt)
  }
  
  private def generateAnswer(question: String, reasoning: String): Task[String] = {
    val prompt = s"Question: $question\n\nReasoning: $reasoning\n\nBased on this reasoning, the answer is:"
    llm.process(prompt)
  }
}
```

### Agent with External Tools

Creating an agent that can use external tools:

```scala
import com.agenticai.core.capability.Capability

class ResearchAgent extends Agent[String, String] {
  private val llm = ClaudeAgent()
  
  // Define capabilities
  private val searchWeb = Capability[String, String] { query =>
    // Implementation to search the web
    ZIO.succeed(s"Results for: $query")
  }
  
  private val calculateExpression = Capability[String, Double] { expr =>
    // Implementation to evaluate mathematical expressions
    ZIO.attempt(new javax.script.ScriptEngineManager()
      .getEngineByName("JavaScript")
      .eval(expr).toString.toDouble)
  }
  
  def process(input: String): Task[String] = {
    for {
      // Determine if and which tool to use
      toolChoice <- determineToolUse(input)
      
      // Use the appropriate tool or just think
      result <- toolChoice match {
        case "search" => 
          searchWeb(input).flatMap(results => 
            answerWithContext(input, results))
            
        case "calculate" => 
          calculateExpression(input).flatMap(result => 
            ZIO.succeed(s"The result is $result"))
            
        case _ => 
          justThink(input)
      }
    } yield result
  }
  
  private def determineToolUse(input: String): Task[String] = {
    val prompt = s"""
      |Question: $input
      |
      |What tool should I use to answer this question?
      |Options:
      |- search: Use a web search tool
      |- calculate: Use a calculator tool
      |- none: No tool needed, just think
      |
      |Tool choice:
      """.stripMargin
    
    llm.process(prompt).map(_.trim.toLowerCase)
  }
  
  private def answerWithContext(question: String, context: String): Task[String] = {
    val prompt = s"""
      |Question: $question
      |
      |Context information:
      |$context
      |
      |Answer based on the context:
      """.stripMargin
    
    llm.process(prompt)
  }
  
  private def justThink(question: String): Task[String] = {
    val prompt = s"Question: $question\n\nThoughtful answer:"
    llm.process(prompt)
  }
}
```

## Integration with Memory Systems

### Agent with Short-Term Memory

Implementing an agent with conversation memory:

```scala
import com.agenticai.core.memory.MemorySystem

class ConversationalAgent extends Agent[String, String] {
  private val llm = ClaudeAgent()
  private val memory = MemorySystem.inMemory[String]
  
  def process(input: String): Task[String] = {
    for {
      // Retrieve conversation history
      history <- memory.getAll
      
      // Construct prompt with history
      prompt = constructPromptWithHistory(input, history)
      
      // Get response from LLM
      response <- llm.process(prompt)
      
      // Store the new interaction
      _ <- memory.store(s"User: $input\nAssistant: $response")
    } yield response
  }
  
  private def constructPromptWithHistory(input: String, history: List[String]): String = {
    val historyText = history.mkString("\n")
    s"""
      |Conversation history:
      |$historyText
      |
      |User: $input
      |Assistant:
      """.stripMargin
  }
}
```

### Agent with Compressed Memory

Using memory compression for efficiency:

```scala
import com.agenticai.core.memory.CompressedMemoryCell

class LongContextAgent extends Agent[String, String] {
  private val llm = ClaudeAgent()
  private val memory = CompressedMemoryCell[String](
    compressThreshold = 10,
    compressionRatio = 0.5
  )
  
  def process(input: String): Task[String] = {
    for {
      // Get compressed memory
      context <- memory.get
      
      // Process with context
      response <- processWithContext(input, context)
      
      // Update memory
      _ <- memory.update(current => current + s"\nUser: $input\nAssistant: $response")
    } yield response
  }
  
  private def processWithContext(input: String, context: String): Task[String] = {
    val prompt = s"""
      |Context:
      |$context
      |
      |User: $input
      |Assistant:
      """.stripMargin
    
    llm.process(prompt)
  }
}
```

## Composing Agents

### Sequential Composition

Chaining agents together in sequence:

```scala
// Base agents
val translationAgent: Agent[String, String] = new TranslationAgent()
val summaryAgent: Agent[String, String] = new SummaryAgent()

// Composed agent that translates then summarizes
val translateAndSummarize: Agent[String, String] = 
  translationAgent.flatMap(translated => summaryAgent.process(translated))
```

### Parallel Processing

Running agents in parallel and combining results:

```scala
import zio.ZIO

class MultifacetedAnalysisAgent extends Agent[String, String] {
  private val sentimentAgent = new SentimentAnalysisAgent()
  private val topicAgent = new TopicExtractionAgent()
  private val entityAgent = new EntityRecognitionAgent()
  
  def process(input: String): Task[String] = {
    for {
      // Run analyses in parallel
      results <- ZIO.collectAllPar(Seq(
        sentimentAgent.process(input),
        topicAgent.process(input),
        entityAgent.process(input)
      ))
      
      // Combine results
      combinedResult = s"""
        |Sentiment: ${results(0)}
        |Topics: ${results(1)}
        |Entities: ${results(2)}
        """.stripMargin
    } yield combinedResult
  }
}
```

### Agent Teams

Creating a team of agents that collaborate:

```scala
class TeamCoordinator extends Agent[String, String] {
  private val researchAgent = new ResearchAgent()
  private val analysisAgent = new AnalysisAgent()
  private val writingAgent = new WritingAgent()
  private val editingAgent = new EditingAgent()
  
  def process(input: String): Task[String] = {
    for {
      // Research phase
      researchResults <- researchAgent.process(input)
      
      // Analysis phase
      analysis <- analysisAgent.process(researchResults)
      
      // Writing phase
      draft <- writingAgent.process(analysis)
      
      // Editing phase
      finalDocument <- editingAgent.process(draft)
    } yield finalDocument
  }
}
```

## Advanced Agent Patterns

### Recursive Self-Improvement

Implementing an agent that can improve its own outputs:

```scala
class SelfImprovingAgent extends Agent[String, String] {
  private val llm = ClaudeAgent()
  
  def process(input: String): Task[String] = {
    for {
      // Initial response
      initialResponse <- generateResponse(input)
      
      // Self-critique
      critique <- critiqueSolution(input, initialResponse)
      
      // Improved response
      improvedResponse <- generateImprovedResponse(input, initialResponse, critique)
    } yield improvedResponse
  }
  
  private def generateResponse(input: String): Task[String] = {
    val prompt = s"Question: $input\n\nAnswer:"
    llm.process(prompt)
  }
  
  private def critiqueSolution(input: String, solution: String): Task[String] = {
    val prompt = s"""
      |Question: $input
      |Solution: $solution
      |
      |Please critique this solution. What are its strengths and weaknesses?
      |How could it be improved?
      """.stripMargin
    
    llm.process(prompt)
  }
  
  private def generateImprovedResponse(
    input: String, 
    initialResponse: String, 
    critique: String
  ): Task[String] = {
    val prompt = s"""
      |Original question: $input
      |Initial solution: $initialResponse
      |Critique: $critique
      |
      |Based on this critique, please provide an improved solution:
      """.stripMargin
    
    llm.process(prompt)
  }
}
```

### Meta-Cognitive Agent

An agent that reasons about its own reasoning process:

```scala
class MetaCognitiveAgent extends Agent[String, String] {
  private val llm = ClaudeAgent()
  
  def process(input: String): Task[String] = {
    for {
      // Generate a plan for how to approach the problem
      plan <- generatePlan(input)
      
      // Execute each step of the plan
      intermediateResults <- executeSteps(input, plan)
      
      // Reflect on the process and results
      reflection <- reflectOnProcess(input, plan, intermediateResults)
      
      // Generate final answer incorporating meta-cognitive insights
      finalAnswer <- generateFinalAnswer(input, intermediateResults, reflection)
    } yield finalAnswer
  }
  
  private def generatePlan(input: String): Task[String] = {
    val prompt = s"""
      |Question: $input
      |
      |Before answering, I should develop a step-by-step plan. What steps should I take to answer this question effectively?
      """.stripMargin
    
    llm.process(prompt)
  }
  
  private def executeSteps(input: String, plan: String): Task[String] = {
    val prompt = s"""
      |Question: $input
      |Plan:
      |$plan
      |
      |Now I'll execute this plan step by step:
      """.stripMargin
    
    llm.process(prompt)
  }
  
  private def reflectOnProcess(
    input: String, 
    plan: String, 
    intermediateResults: String
  ): Task[String] = {
    val prompt = s"""
      |Question: $input
      |Plan:
      |$plan
      |
      |Work:
      |$intermediateResults
      |
      |Let me reflect on my process:
      |1. Did I follow the plan effectively?
      |2. What worked well in my approach?
      |3. What could I have done differently?
      |4. Am I confident in my results?
      |5. What are the limitations of my answer?
      """.stripMargin
    
    llm.process(prompt)
  }
  
  private def generateFinalAnswer(
    input: String, 
    intermediateResults: String, 
    reflection: String
  ): Task[String] = {
    val prompt = s"""
      |Question: $input
      |
      |My work:
      |$intermediateResults
      |
      |My reflection:
      |$reflection
      |
      |Taking all this into account, my final answer is:
      """.stripMargin
    
    llm.process(prompt)
  }
}
```

### Agent with Circuit-Based Reasoning

Using the circuit pattern for complex reasoning flows:

```scala
import com.agenticai.core.memory.circuits._

class CircuitReasoningAgent extends Agent[String, String] {
  private val llm = ClaudeAgent()
  
  // Define circuit components
  private val questionAnalysis = Circuit.component[String, String]("analysis") { question =>
    val prompt = s"Question: $question\n\nAnalyze what's being asked:"
    llm.process(prompt)
  }
  
  private val relevantInformation = Circuit.component[String, String]("information") { state =>
    val prompt = s"Context: $state\n\nWhat information is relevant here?"
    llm.process(prompt)
  }
  
  private val reasoning = Circuit.component[String, String]("reasoning") { state =>
    val prompt = s"Context: $state\n\nReasoning process:"
    llm.process(prompt)
  }
  
  private val conclusion = Circuit.component[String, String]("conclusion") { state =>
    val prompt = s"Context: $state\n\nFinal conclusion:"
    llm.process(prompt)
  }
  
  // Connect components into a circuit
  private val reasoningCircuit = Circuit.sequence(
    questionAnalysis,
    relevantInformation,
    reasoning,
    conclusion
  )
  
  def process(input: String): Task[String] = {
    reasoningCircuit.process(input)
  }
}
```

## Testing and Debugging

### Unit Testing Agents

Basic strategy for testing agents:

```scala
import zio.test._
import zio.test.Assertion._

object CalculatorAgentSpec extends ZIOSpecDefault {
  def spec = suite("CalculatorAgent")(
    test("should correctly add numbers") {
      val agent = new CalculatorAgent()
      for {
        result <- agent.process("add 5 3")
      } yield assert(result)(equalTo(8.0))
    },
    
    test("should correctly subtract numbers") {
      val agent = new CalculatorAgent()
      for {
        result <- agent.process("subtract 10 4")
      } yield assert(result)(equalTo(6.0))
    },
    
    test("should handle invalid input") {
      val agent = new CalculatorAgent()
      assertZIO(agent.process("invalid input").exit)(fails(isSubtype[IllegalArgumentException](anything)))
    }
  )
}
```

### Mock LLM for Testing

Creating a mock LLM for deterministic tests:

```scala
import com.agenticai.core.llm.ClaudeAgent
import com.agenticai.core.agent.Agent

class MockLLM extends Agent[String, String] {
  private val responses = Map(
    "What is the capital of France?" -> "The capital of France is Paris.",
    "Summarize this text:" -> "This is a summary."
  )
  
  def process(input: String): Task[String] = {
    val matchingKey = responses.keys.find(input.contains)
    ZIO.fromOption(matchingKey.map(responses))
      .orElse(ZIO.succeed("I don't know the answer to that."))
  }
}

// Test an agent that uses the mock
class SummarizeAgent(llm: Agent[String, String]) extends Agent[String, String] {
  def process(input: String): Task[String] = {
    llm.process(s"Summarize this text: $input")
  }
}

object SummarizeAgentSpec extends ZIOSpecDefault {
  def spec = suite("SummarizeAgent")(
    test("should summarize text") {
      val mockLLM = new MockLLM()
      val agent = new SummarizeAgent(mockLLM)
      
      for {
        result <- agent.process("Some long text...")
      } yield assert(result)(equalTo("This is a summary."))
    }
  )
}
```

### Debugging with Logging

Adding logging to trace agent execution:

```scala
import zio.logging._

class LoggingAgent extends Agent[String, String] {
  private val llm = ClaudeAgent()
  
  def process(input: String): Task[String] = {
    for {
      _ <- ZIO.logInfo(s"Processing input: $input")
      
      // Generate plan
      _ <- ZIO.logDebug("Generating plan")
      plan <- generatePlan(input)
      _ <- ZIO.logDebug(s"Plan: $plan")
      
      // Execute plan
      _ <- ZIO.logDebug("Executing plan")
      result <- executePlan(input, plan)
      _ <- ZIO.logInfo(s"Completed processing with result length: ${result.length}")
    } yield result
  }
  
  private def generatePlan(input: String): Task[String] = {
    // Implementation...
    ZIO.succeed("Step 1: Analyze question\nStep 2: Generate answer")
  }
  
  private def executePlan(input: String, plan: String): Task[String] = {
    // Implementation...
    ZIO.succeed("This is the answer.")
  }
}
```

## Performance Optimization

### Caching Results

Implementing caching to improve performance:

```scala
import zio.cache.Cache

class CachingAgent extends Agent[String, String] {
  private val baseAgent: Agent[String, String] = new ExpensiveAgent()
  
  // Create a cache
  private val cache: UIO[Cache[String, Throwable, String]] = Cache.make(
    capacity = 100,
    timeToLive = Duration.Infinity,
    lookup = (key: String) => baseAgent.process(key)
  )
  
  def process(input: String): Task[String] = {
    for {
      c <- cache
      result <- c.get(input)
    } yield result
  }
}
```

### Streaming Responses

Creating an agent that streams its response:

```scala
import zio.stream._

trait StreamingAgent[I, O] {
  def processStream(input: I): ZStream[Any, Throwable, O]
}

class StreamingChatAgent extends StreamingAgent[String, String] {
  private val llm = ClaudeAgent()
  
  def processStream(input: String): ZStream[Any, Throwable, String] = {
    // Implement streaming behavior
    ZStream.fromEffect(llm.process(input)).flatMap { fullResponse =>
      ZStream.fromIterable(fullResponse.split(" "))
    }
  }
}
```

## Deployment Considerations

### Configurable Agents

Creating configurable agents:

```scala
case class TextGenerationConfig(
  temperature: Double,
  maxTokens: Int,
  systemPrompt: String
)

class ConfigurableTextGenerator(config: TextGenerationConfig) extends Agent[String, String] {
  private val llm = ClaudeAgent()
  
  def process(input: String): Task[String] = {
    val prompt = s"""
      |${config.systemPrompt}
      |
      |User input: $input
      |
      |Response:
      """.stripMargin
    
    llm.process(prompt)
  }
  
  def withTemperature(temperature: Double): ConfigurableTextGenerator =
    new ConfigurableTextGenerator(config.copy(temperature = temperature))
    
  def withMaxTokens(maxTokens: Int): ConfigurableTextGenerator =
    new ConfigurableTextGenerator(config.copy(maxTokens = maxTokens))
    
  def withSystemPrompt(systemPrompt: String): ConfigurableTextGenerator =
    new ConfigurableTextGenerator(config.copy(systemPrompt = systemPrompt))
}
```

### Resource Management

Proper resource management for agents:

```scala
import zio.Scope

class DatabaseAgent extends Agent[String, String] {
  def process(input: String): RIO[Scope, String] = {
    ZIO.acquireRelease(
      acquire = openDatabaseConnection()
    )(
      release = closeDatabaseConnection
    ).flatMap { connection =>
      // Use connection to process query
      queryDatabase(connection, input)
    }
  }
  
  private def openDatabaseConnection(): Task[Connection] = {
    // Implementation to open database connection
    ZIO.succeed(new MockConnection())
  }
  
  private def closeDatabaseConnection(connection: Connection): UIO[Unit] = {
    // Implementation to close database connection
    ZIO.succeed(())
  }
  
  private def queryDatabase(connection: Connection, query: String): Task[String] = {
    // Implementation to query database
    ZIO.succeed("Query results")
  }
}
```

### Distributed Agents

Making agents work in a distributed environment:

```scala
import com.agenticai.mesh._
import com.agenticai.mesh.protocol._

object DistributedAgentExample extends ZIOAppDefault {
  def run = {
    for {
      // Create mesh and register agents
      mesh <- ZIO.succeed(AgentMesh())
      
      calculatorAgent = new CalculatorAgent()
      textAgent = new TextProcessingAgent()
      
      // Register agents on different nodes
      calcRef <- mesh.deploy(calculatorAgent, AgentLocation("node1", 8080))
      textRef <- mesh.deploy(textAgent, AgentLocation("node2", 8080))
      
      // Get remote wrappers
      remoteCalc <- mesh.getRemoteAgent(calcRef)
      remoteText <- mesh.getRemoteAgent(textRef)
      
      // Create a composite agent using remote agents
      compositeAgent = new CompositeAgent(remoteCalc, remoteText)
      
      // Process inputs
      result1 <- compositeAgent.process("calculate multiply 5 10")
      _ <- Console.printLine(s"Result 1: $result1")
      
      result2 <- compositeAgent.process("format hello distributed agents")
      _ <- Console.printLine(s"Result 2: $result2")
    } yield ()
  }
}
```

## Conclusion

This guide has covered the fundamentals of creating custom agents with the Agentic AI Framework, from basic implementations to advanced patterns. By following these principles and techniques, you can build sophisticated AI agents that leverage the full power of the framework's functional, composable architecture.

Remember the key principles:

1. **Start Simple**: Begin with the basic Agent interface and add complexity incrementally
2. **Leverage Composition**: Use functional composition to build complex agents from simpler ones
3. **Maintain Purity**: Keep core logic pure and push effects to the edges
4. **Test Thoroughly**: Create comprehensive tests for predictable agent behavior
5. **Optimize Gradually**: Start with correct behavior, then optimize for performance

For more detailed information on specific components, refer to the API documentation and other guides in the framework documentation.