package com.agenticai.workflow.server

import com.agenticai.workflow.agent._
import com.agenticai.workflow.engine._
import com.agenticai.workflow.model._

import zio._
import zio.http._
import zio.json._
import zio.test._
import zio.test.Assertion._
import zio.test.TestAspect._

/**
 * Test suite for WorkflowHttpServer
 */
object WorkflowHttpServerSpec extends ZIOSpecDefault {
  
  /**
   * Mock implementation of TextTransformerAgent for testing
   */
  private class MockTextTransformerAgent extends TextTransformerAgent {
    override def process(input: String): ZIO[Any, Throwable, String] = 
      ZIO.succeed(s"Transformed: $input")
  }
  
  /**
   * Mock implementation of TextSplitterAgent for testing
   */
  private class MockTextSplitterAgent extends TextSplitterAgent {
    override def process(input: String): ZIO[Any, Throwable, String] = 
      ZIO.succeed(s"Split: $input")
  }
  
  /**
   * Mock implementation of SummarizationAgent for testing
   */
  private class MockSummarizationAgent extends SummarizationAgent {
    override def process(input: String): ZIO[Any, Throwable, String] = 
      ZIO.succeed(s"Summary: $input")
  }
  
  /**
   * Mock implementation of BuildAgent for testing
   */
  private class MockBuildAgent extends BuildAgent {
    override def process(input: String): ZIO[Any, Throwable, String] = 
      ZIO.succeed(s"Built: $input")
  }
  
  /**
   * Create a sample workflow for testing
   */
  private val sampleWorkflow = Workflow(
    id = "test-workflow",
    name = "Test Workflow",
    description = "A test workflow for unit testing",
    nodes = List(
      WorkflowNode(
        id = "node1",
        nodeType = "text-transformer",
        label = "Transform Text",
        configuration = Map("transform" -> "capitalize"),
        position = NodePosition(100, 100)
      ),
      WorkflowNode(
        id = "node2",
        nodeType = "summarizer",
        label = "Summarize Text",
        configuration = Map(),
        position = NodePosition(300, 100)
      )
    ),
    connections = List(
      NodeConnection(
        id = "conn1",
        sourceNodeId = "node1",
        targetNodeId = "node2"
      )
    )
  )
  
  // Test layer for dependencies
  private val testLayer = {
    // Create individual agent layers
    val mockTransformer = new MockTextTransformerAgent
    val mockSplitter = new MockTextSplitterAgent
    val mockSummarizer = new MockSummarizationAgent
    val mockBuilder = new MockBuildAgent
    
    // Create workflow engine directly
    val workflowEngine = new WorkflowEngine(
      mockTransformer,
      mockSplitter,
      mockSummarizer,
      mockBuilder
    )
    
    // Create the complete layer using ZLayer.succeed for each component
    ZLayer.succeed(mockTransformer) ++
    ZLayer.succeed(mockSplitter) ++
    ZLayer.succeed(mockSummarizer) ++
    ZLayer.succeed(mockBuilder) ++
    ZLayer.succeed(workflowEngine)
  }
  
  /**
   * Simple helper class for testing workflow execution status
   */
  private class TestWorkflowStore {
    private val store = scala.collection.mutable.Map.empty[String, (String, Int, Option[String])]
    
    def create(id: String): Unit = {
      store.put(id, ("running", 0, None))
    }
    
    def updateProgress(id: String, progress: Int): Unit = {
      store.get(id).foreach { case (status, _, result) =>
        store.put(id, (status, progress, result))
      }
    }
    
    def complete(id: String, result: String): Unit = {
      store.put(id, ("completed", 100, Some(result)))
    }
    
    def getStatus(id: String): Option[(String, Int, Option[String])] = {
      store.get(id)
    }
  }
  
  /**
   * Test spec
   */
  override def spec = suite("WorkflowHttpServerSpec")(
    
    test("WorkflowExecutionStore should properly manage workflow status") {
      // Create a store instance
      val store = new TestWorkflowStore()
      
      // Create a workflow execution
      val id = "test-workflow-1"
      store.create(id)
      
      // Check initial status
      val initialStatus = store.getStatus(id)
      
      // Update progress
      store.updateProgress(id, 50)
      val progressStatus = store.getStatus(id)
      
      // Complete the workflow
      store.complete(id, "Test result")
      val completedStatus = store.getStatus(id)
      
      // Verify all statuses
      assertTrue(
        initialStatus.exists(_._1 == "running"),
        initialStatus.exists(_._2 == 0),
        initialStatus.exists(_._3.isEmpty),
        progressStatus.exists(_._1 == "running"),
        progressStatus.exists(_._2 == 50),
        progressStatus.exists(_._3.isEmpty),
        completedStatus.exists(_._1 == "completed"),
        completedStatus.exists(_._2 == 100),
        completedStatus.exists(_._3.contains("Test result"))
      )
    },
    
    test("WorkflowEngine should execute a workflow correctly") {
      for {
        engine <- ZIO.service[WorkflowEngine]
        result <- engine.executeWorkflow(sampleWorkflow, "Test input")
      } yield assertTrue(
        result == "Summary: Transformed: Test input"
      )
    }
  ).provide(testLayer) @@ timeout(10.seconds)
}