package com.agenticai.core.llm.langchain.rag.context

import dev.langchain4j.data.segment.TextSegment
import zio.*
import zio.test.*
import zio.test.Assertion.*

object ContextBuilderSpec extends ZIOSpecDefault:

  def spec = suite("ContextBuilder")(
    test("should build context from query and segments") {
      val query = "What is the capital of France?"
      val segments = List(
        TextSegment.from("Paris is the capital of France."),
        TextSegment.from("France is a country in Western Europe.")
      )
      
      val contextBuilder = DefaultContextBuilder()
      
      for {
        context <- contextBuilder.buildContext(segments, query)
      } yield
        assertTrue(
          context.contains("Answer the question based on the following context:"),
          context.contains("Context:"),
          context.contains("Paris is the capital of France."),
          context.contains("France is a country in Western Europe."),
          context.contains("Question: What is the capital of France?")
        )
    },
    
    test("should handle empty segments list") {
      val query = "What is the capital of France?"
      val segments = List.empty[TextSegment]
      
      val contextBuilder = DefaultContextBuilder()
      
      for {
        context <- contextBuilder.buildContext(segments, query)
      } yield
        assertTrue(
          context.contains("Answer the question based on the following context:"),
          context.contains("Context:"),
          context.contains("Question: What is the capital of France?")
        )
    },
    
    test("should handle special characters in query and segments") {
      val query = "What about \"quoted text\" and line\nbreaks?"
      val segments = List(
        TextSegment.from("Text with \"quotes\" and\nmultiple\nline breaks."),
        TextSegment.from("Text with special chars: @#$%^&*()")
      )
      
      val contextBuilder = DefaultContextBuilder()
      
      for {
        context <- contextBuilder.buildContext(segments, query)
      } yield
        assertTrue(
          context.contains("\"quotes\""),
          context.contains("line\nbreaks"),
          context.contains("\"quoted text\""),
          context.contains("special chars: @#$%^&*()")
        )
    }
  )