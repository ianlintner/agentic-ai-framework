package com.agenticai.examples.webdashboard.agents

import com.agenticai.core.memory.MemorySystem
import com.agenticai.examples.webdashboard.models.TaskRequest
import zio._
import zio.test._
import zio.test.Assertion._
import zio.test.TestAspect._
import java.time.Instant
import java.util.UUID

object TaskProcessorAgentSpec extends ZIOSpecDefault {
  def spec = suite("TaskProcessorAgent")(
    test("should process a simple task request") {
      val taskRequest = TaskRequest(
        title = "Research quantum computing",
        description = "Investigate recent developments in quantum computing",
        priority = "Medium"
      )
      
      val agent = new TaskProcessorAgent()
      
      for {
        result <- agent.process(taskRequest)
          .runHead
          .someOrFail(new RuntimeException("No response generated"))
          .provideLayer(ZLayer.fromZIO(MemorySystem.make))
      } yield assertTrue(
        result.requestId == taskRequest.id &&
        result.status == "Completed" &&
        result.results.nonEmpty
      )
    } @@ TestAspect.withLiveClock @@ timeout(5.seconds),
    
    test("should handle complex task with multiple subtasks") {
      val taskRequest = TaskRequest(
        title = "Write technical documentation",
        description = """Create comprehensive documentation for our API including:
          |1. Installation guide
          |2. API reference
          |3. Code examples
          |4. Troubleshooting guide""".stripMargin,
        priority = "High"
      )
      
      val agent = new TaskProcessorAgent()
      
      for {
        result <- agent.process(taskRequest)
          .runHead
          .someOrFail(new RuntimeException("No response generated"))
          .provideLayer(ZLayer.fromZIO(MemorySystem.make))
      } yield assertTrue(
        result.requestId == taskRequest.id &&
        result.status == "Completed" &&
        result.results.size >= 4
      )
    } @@ TestAspect.withLiveClock @@ timeout(5.seconds),
    
    test("should prioritize high priority tasks") {
      val highPriorityTask = TaskRequest(
        title = "Critical bug fix",
        description = "Fix production server crash issue",
        priority = "Critical"
      )
      
      val agent = new TaskProcessorAgent()
      
      for {
        result <- agent.process(highPriorityTask)
          .runHead
          .someOrFail(new RuntimeException("No response generated"))
          .provideLayer(ZLayer.fromZIO(MemorySystem.make))
      } yield assertTrue(
        result.requestId == highPriorityTask.id &&
        result.status == "Completed" &&
        result.results.nonEmpty
      )
    } @@ TestAspect.withLiveClock @@ timeout(5.seconds)
  ) @@ sequential
}