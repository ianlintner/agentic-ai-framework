package com.agenticai.core.llm.langchain.rag.generation

import com.agenticai.core.llm.langchain.{LangchainError, ModelError}
import com.agenticai.core.llm.langchain.test.MockChatLanguageModel
import com.agenticai.core.llm.langchain.rag.RAGError
import dev.langchain4j.data.message.{AiMessage, ChatMessage, UserMessage}
import zio.*
import zio.test.*
import zio.test.Assertion.*

object ResponseGeneratorSpec extends ZIOSpecDefault:

  def spec = suite("ResponseGenerator")(
    test("should generate response from context") {
      val expectedResponse = "This is a generated response"
      val mockModel = new MockChatLanguageModel(Map.empty, expectedResponse)
      val responseGenerator = DefaultResponseGenerator(mockModel)
      
      val context = "Answer this question based on the context: What is the capital of France?"
      val query = "What is the capital of France?"
      
      responseGenerator.generateResponse(context, query).map { response =>
        assertTrue(response == expectedResponse)
      }
    },
    
    test("should handle errors from the chat model") {
      val errorMessage = "Model API error"
      val errorThrowingModel = new MockChatLanguageModel(Map.empty) {
        override def generate(messages: List[ChatMessage]): ZIO[Any, LangchainError, AiMessage] = {
          val exception = new RuntimeException(errorMessage)
          ZIO.fail(ModelError(exception, errorMessage))
        }
      }
      
      val responseGenerator = DefaultResponseGenerator(errorThrowingModel)
      
      val context = "Some context"
      val query = "Test query"
      
      responseGenerator.generateResponse(context, query).exit.map { result =>
        assert(result)(
          fails(isSubtype[RAGError.GenerationError](
            hasField("message", _.message, containsString(errorMessage))
          ))
        )
      }
    }
  )