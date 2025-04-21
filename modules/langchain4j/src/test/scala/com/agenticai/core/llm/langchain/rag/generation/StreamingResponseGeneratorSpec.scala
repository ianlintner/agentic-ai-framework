package com.agenticai.core.llm.langchain.rag.generation

import com.agenticai.core.llm.langchain.{LangchainError, ModelError}
import com.agenticai.core.llm.langchain.rag.RAGError
import com.agenticai.core.llm.langchain.test.MockChatLanguageModel
import dev.langchain4j.data.message.{ChatMessage, UserMessage}
import zio.*
import zio.stream.*
import zio.test.*
import zio.test.Assertion.*

object StreamingResponseGeneratorSpec extends ZIOSpecDefault {
  def spec = suite("StreamingResponseGeneratorSpec")(
    test("generateStreamingResponse should stream content chunks") {
      // Given
      val expectedChunks = List("This", " is", " a", " test", " response")
      val mockModel = new MockChatLanguageModel(
        generateStreamFn = _ => ZStream.fromIterable(expectedChunks)
      )
      val generator = DefaultStreamingResponseGenerator(mockModel)
      
      // When/Then
      for {
        chunks <- generator.generateStreamingResponse(
          context = "Some context", 
          query = "Test query"
        ).runCollect
      } yield assertTrue(chunks == expectedChunks) &&
        assertTrue(chunks.size == 5)
    }
  )
}