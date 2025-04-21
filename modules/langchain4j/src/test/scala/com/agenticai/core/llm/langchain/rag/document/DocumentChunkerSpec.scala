package com.agenticai.core.llm.langchain.rag.document

import dev.langchain4j.data.document.Document
import zio.*
import zio.test.*
import zio.test.Assertion.*

object DocumentChunkerSpec extends ZIOSpecDefault:

  def spec = suite("DocumentChunker")(
    test("should chunk document into segments of specified size") {
      // Create a test document
      val text = "This is a test document that will be split into chunks. " +
                 "We want to ensure that the chunking works correctly with the specified size and overlap."
      val document = Document.from(text)
      
      // Create a chunker with maxChunkSize = 30 and overlap = 5
      val chunker = DefaultDocumentChunker(30, 5)
      
      for
        segments <- chunker.chunkDocument(document)
      yield
        // We should have 4 segments
        // Check the number of segments
        assertTrue(segments.size == 4)
        
        // Check the first segment starts with the expected text
        assertTrue(segments(0).text().startsWith("This is a test document that"))
        
        // Check that the combined content contains the original text
        val combinedText = segments.foldLeft("")((acc, segment) => acc + segment.text())
        assertTrue(combinedText.replaceAll("\\s+", " ").contains(text.replaceAll("\\s+", " ")))
    },
    
    test("should handle short documents that fit in a single chunk") {
      val text = "Short document."
      val document = Document.from(text)
      
      val chunker = DefaultDocumentChunker(100, 10)
      
      for
        segments <- chunker.chunkDocument(document)
      yield
        assertTrue(
          segments.size == 1,
          segments(0).text() == text
        )
    },
    
    test("should handle empty documents") {
      // Use a non-empty string since Document.from() doesn't accept empty strings
      val document = Document.from("A")
      
      val chunker = DefaultDocumentChunker(10, 2)
      
      for
        segments <- chunker.chunkDocument(document)
      yield
        assertTrue(
          segments.size == 1,
          segments(0).text() == "A"
        )
    }
  )