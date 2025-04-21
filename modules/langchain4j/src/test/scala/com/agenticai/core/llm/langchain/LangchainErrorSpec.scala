package com.agenticai.core.llm.langchain

import zio.*
import zio.test.*
import zio.test.Assertion.*
import zio.test.TestAspect.*

import java.io.IOException
import java.util.concurrent.TimeoutException

object LangchainErrorSpec extends ZIOSpecDefault {

  def spec = suite("LangchainError")(
    test("fromThrowable should map LangchainError to itself") {
      // Given a LangchainError
      val error = ModelError(new RuntimeException("Test error"), "Test model error")
      
      // When mapping it with fromThrowable
      val result = LangchainError.fromThrowable(error)
      
      // Then it should return the same error
      assertTrue(result == error)
    },
    
    test("fromThrowable should wrap non-LangchainError in ModelError") {
      // Given a non-LangchainError
      val exception = new RuntimeException("Test exception")
      
      // When mapping it with fromThrowable
      val result = LangchainError.fromThrowable(exception)
      
      // Then it should be wrapped in a ModelError
      assertTrue(
        result.isInstanceOf[ModelError] &&
        result.asInstanceOf[ModelError].underlyingCause == exception
      )
    },
    
    test("fromLangchain4jException should map exceptions based on class name") {
      // Create test exceptions with class names that match the patterns
      val rateLimitException = new RuntimeException("Rate limit exceeded, retry after 30 seconds") 
      val authException = new RuntimeException("Invalid API key")
      val contextLengthException = new RuntimeException("Context length exceeded")
      val serviceException = new RuntimeException("Service unavailable")
      val invalidRequestException = new RuntimeException("Invalid request")
      
      // Map each exception and check the result type
      val rateLimitResult = LangchainError.fromLangchain4jException(rateLimitException)
      val authResult = LangchainError.fromLangchain4jException(authException)
      val contextLengthResult = LangchainError.fromLangchain4jException(contextLengthException)
      val serviceResult = LangchainError.fromLangchain4jException(serviceException)
      val invalidRequestResult = LangchainError.fromLangchain4jException(invalidRequestException)
      
      assertTrue(
        rateLimitResult.isInstanceOf[RateLimitError] &&
        authResult.isInstanceOf[AuthenticationError] &&
        contextLengthResult.isInstanceOf[ContextLengthError] &&
        serviceResult.isInstanceOf[ServiceUnavailableError] &&
        invalidRequestResult.isInstanceOf[InvalidRequestError]
      )
    },
    
    test("RateLimitError should extract retry-after information") {
      // Given an exception with retry-after information
      val exception = new RuntimeException("Rate limit exceeded, retry after 30 seconds")

      // When mapping it with fromLangchain4jException
      val result = LangchainError.fromLangchain4jException(exception)
      
      // Then it should extract the retry-after duration
      assertTrue(
        result.isInstanceOf[RateLimitError] &&
        result.asInstanceOf[RateLimitError].retryAfter.isDefined &&
        result.asInstanceOf[RateLimitError].retryAfter.get == Duration.fromSeconds(30)
      )
    },
    
    test("Error messages should be properly formatted") {
      // Create various error types
      val modelError = ModelError(new RuntimeException("Test exception"), "Model error")
      val rateLimitError = RateLimitError(Some(Duration.fromSeconds(30)), "Rate limit")
      val authError = AuthenticationError("Invalid API key")
      val contextLengthError = ContextLengthError("Context too long", Some(2000), Some(1000))
      val serviceError = ServiceUnavailableError("Service down", Some(5.minutes))
      val invalidRequestError = InvalidRequestError("Bad request")
      
      // Check the formatted messages
      assertTrue(
        modelError.getMessage.contains("Model error") &&
        modelError.getMessage.contains("Test exception") &&
        rateLimitError.getMessage.contains("Rate limit") &&
        rateLimitError.getMessage.contains("30 seconds") &&
        authError.getMessage == "Invalid API key" &&
        contextLengthError.getMessage.contains("Context too long") &&
        contextLengthError.getMessage.contains("2000 tokens") &&
        contextLengthError.getMessage.contains("max: 1000") &&
        serviceError.getMessage.contains("Service down") &&
        serviceError.getMessage.contains("5 minutes") &&
        invalidRequestError.getMessage == "Bad request"
      )
    }
  ) @@ timeout(10.seconds)
}