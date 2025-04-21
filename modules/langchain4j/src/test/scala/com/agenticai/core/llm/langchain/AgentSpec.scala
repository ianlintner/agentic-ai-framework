package com.agenticai.core.llm.langchain

import com.agenticai.core.llm.langchain.test.MockChatLanguageModel
import dev.langchain4j.data.message.{AiMessage, ChatMessage, UserMessage}
import zio.*
import zio.stream.ZStream
import zio.test.*
import zio.test.Assertion.*

object AgentSpec extends ZIOSpecDefault:

  override def spec: Spec[TestEnvironment, Any] =
    suite("LangchainAgent")(
      test("should process user input and return a streaming response") {
        val testProgram = for {
          // Create a mock model with a predefined response
          model <- ZIO.succeed(new MockChatLanguageModel(
            defaultResponse = "Mock streaming response to: Hello"
          ))
          
          // Create memory and agent
          memory <- ZIOChatMemory.createInMemory(100)
          agent = LangchainAgent(model, memory, "test-agent")
          
          // Process a user input
          response <- agent.process("Hello").runCollect
          responseText = response.map(_.toString).mkString
          
          // Get the conversation history
          history <- agent.getHistory
          
          // Find the AI message in the history
          aiMessage = history.find(_.isInstanceOf[AiMessage])
        } yield {
          // Assertions
          assertTrue(
            responseText.contains("Mock streaming response to:"),
            history.size == 2,
            aiMessage.isDefined,
            aiMessage.exists(_.toString.contains("Mock streaming response to:"))
          )
        }
        
        testProgram
      },
      
      test("should handle errors gracefully") {
        val testProgram = for {
          // Create a model that will throw an error
          errorException <- ZIO.succeed(new RuntimeException("Test error"))
          model <- ZIO.succeed(new MockChatLanguageModel() {
            override def generateStream(messages: List[ChatMessage]): ZStream[Any, LangchainError, String] =
              ZStream.fail(ModelError(errorException))
          })
          
          // Create memory and agent
          memory <- ZIOChatMemory.createInMemory(100)
          agent = LangchainAgent(model, memory, "test-agent")
          
          // Process a user input and expect an error
          result <- agent.process("Hello").runCollect.either
        } yield {
          // Assertions
          assertTrue(
            result.isLeft,
            result.left.exists(_.getMessage.contains("Test error"))
          )
        }
        
        testProgram
      }
    )