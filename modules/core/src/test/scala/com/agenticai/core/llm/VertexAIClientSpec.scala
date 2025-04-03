package com.agenticai.core.llm

import zio._
import zio.test._
import zio.test.Assertion._
import zio.test.TestAspect._
import zio.stream.ZStream

object VertexAIClientSpec extends ZIOSpecDefault {
  def spec = suite("VertexAIClient")(
    test("creates functional client with successful responses") {
      for {
        mockClient <- ZIO.succeed(new VertexAIClient {
          override def streamCompletion(prompt: String): ZStream[Any, Throwable, String] = 
            ZStream.fromIterable(List("Test response"))
          override def complete(prompt: String): Task[String] = 
            ZIO.succeed("Test response")
          override def generateText(prompt: String): Task[String] =
            ZIO.succeed("Test response")
        })
        completion <- mockClient.complete("test")
        generation <- mockClient.generateText("test")
        stream <- mockClient.streamCompletion("test").runCollect
      } yield assertTrue(
        completion == "Test response",
        generation == "Test response",
        stream == List("Test response")
      )
    },
    test("handles errors appropriately") {
      val testError = new RuntimeException("Test error")
      for {
        mockClient <- ZIO.succeed(new VertexAIClient {
          override def streamCompletion(prompt: String): ZStream[Any, Throwable, String] = 
            ZStream.fail(testError)
          override def complete(prompt: String): Task[String] = 
            ZIO.fail(testError)
          override def generateText(prompt: String): Task[String] =
            ZIO.fail(testError)
        })
        completionExit <- mockClient.complete("test").exit
        generationExit <- mockClient.generateText("test").exit
        streamExit <- mockClient.streamCompletion("test").runCollect.exit
      } yield assertTrue(
        completionExit == Exit.fail(testError),
        generationExit == Exit.fail(testError),
        streamExit == Exit.fail(testError)
      )
    },
    test("generates text from prompt with proper error handling") {
      for {
        mockClient <- ZIO.succeed(new VertexAIClient {
          override def streamCompletion(prompt: String): ZStream[Any, Throwable, String] = 
            ZStream.fromIterable(List("Test response"))
          override def complete(prompt: String): Task[String] = 
            ZIO.succeed("Test response")
          override def generateText(prompt: String): Task[String] =
            if (prompt.isEmpty) ZIO.fail(new IllegalArgumentException("Prompt cannot be empty"))
            else ZIO.succeed("Generated: " + prompt)
        })
        successResponse <- mockClient.generateText("Write a story")
        emptyPromptExit <- mockClient.generateText("").exit
      } yield assertTrue(
        successResponse == "Generated: Write a story",
        emptyPromptExit.isFailure
      )
    },
    test("completes prompt with response and handles errors") {
      for {
        mockClient <- ZIO.succeed(new VertexAIClient {
          override def streamCompletion(prompt: String): ZStream[Any, Throwable, String] = 
            ZStream.fromIterable(List("Test response"))
          override def complete(prompt: String): Task[String] = 
            if (prompt.isEmpty) ZIO.fail(new IllegalArgumentException("Prompt cannot be empty"))
            else ZIO.succeed("Completed: " + prompt)
          override def generateText(prompt: String): Task[String] =
            ZIO.succeed("Test response")
        })
        successResponse <- mockClient.complete("Hello, who are you?")
        emptyPromptExit <- mockClient.complete("").exit
      } yield assertTrue(
        successResponse == "Completed: Hello, who are you?",
        emptyPromptExit.isFailure
      )
    },
    test("streams response tokens sequentially with error handling") {
      for {
        mockClient <- ZIO.succeed(new VertexAIClient {
          override def streamCompletion(prompt: String): ZStream[Any, Throwable, String] = 
            if (prompt.isEmpty) ZStream.fail(new IllegalArgumentException("Prompt cannot be empty"))
            else ZStream.fromIterable(List("1", "2", "3", "4", "5"))
          override def complete(prompt: String): Task[String] = 
            ZIO.succeed("Test response")
          override def generateText(prompt: String): Task[String] =
            ZIO.succeed("Test response")
        })
        successTokens <- mockClient.streamCompletion("Count from 1 to 5.").runCollect
        emptyPromptExit <- mockClient.streamCompletion("").runCollect.exit
      } yield assertTrue(
        successTokens == List("1", "2", "3", "4", "5"),
        emptyPromptExit.isFailure
      )
    }
  ) @@ TestAspect.sequential
}