package com.agenticai.core.memory.circuits.examples

import com.agenticai.core.memory.circuits.AgentCombinators.{Agent, pipeline}
import com.agenticai.core.memory.circuits.CircuitMemory

/**
 * Demonstrates the use of circuit patterns for text processing.
 * 
 * This example shows how to compose agents using various combinators
 * to build a text processing pipeline inspired by Factorio's circuit networks.
 */
object TextProcessingDemo {
  
  // Set of common stop words to filter out
  val stopWords = Set("the", "a", "an", "and", "or", "but", "in", "on", "at", "to", "for", "with", "by")
  
  /**
   * Main demo showing how to use the circuit patterns
   */
  def main(args: Array[String]): Unit = {
    // Example sentences to process
    val sentences = List(
      "The quick brown fox jumps over the lazy dog",
      "A watched pot never boils",
      "Actions speak louder than words",
      "The early bird catches the worm"
    )
    
    // Create a memory cell to track word frequency
    val wordCountMemory = new CircuitMemory.MemoryCell[Map[String, Int]](Map.empty)
    
    // Create a tracking memory cell for the top words
    val topWordsMemory = new CircuitMemory.MemoryCell[List[(String, Int)]](List.empty)
    
    // Basic text processing agents
    val tokenizer = Agent[String, List[String]] { text =>
      text.toLowerCase.replaceAll("[^a-z\\s]", "").split("\\s+").toList
    }
    
    val stopWordsFilter = Agent[List[String], List[String]] { words =>
      words.filterNot(stopWords.contains)
    }
    
    val counter = Agent[List[String], Map[String, Int]] { words =>
      words.groupBy(identity).view.mapValues(_.size).toMap
    }
    
    val wordFrequencyUpdater = Agent[Map[String, Int], Map[String, Int]] { counts =>
      // Merge new counts with existing counts in memory
      val currentCounts = wordCountMemory.get
      val updatedCounts = currentCounts ++ counts.map { case (word, count) => 
        word -> (currentCounts.getOrElse(word, 0) + count)
      }
      
      // Update memory
      wordCountMemory.set(updatedCounts)
      updatedCounts
    }
    
    val topWordsFinder = Agent[Map[String, Int], List[(String, Int)]] { counts =>
      val topWords = counts.toList.sortBy(-_._2).take(5)
      topWordsMemory.set(topWords)
      topWords
    }
    
    val formatter = Agent[List[(String, Int)], String] { topWords =>
      "Top words:\n" + topWords.map { case (word, count) => 
        s"  $word: $count"
      }.mkString("\n")
    }
    
    // Compose the pipeline using combinators
    val processWords = pipeline(tokenizer, stopWordsFilter)
    val updateFrequency = pipeline(counter, wordFrequencyUpdater)
    val findTopWords = pipeline(topWordsFinder, formatter)
    
    // Connect the entire pipeline
    val pipeline1 = pipeline(processWords, updateFrequency)
    val completePipeline = pipeline(pipeline1, findTopWords)
    
    // Process each sentence and view the evolving results
    println("Starting text processing demo using circuit patterns...\n")
    sentences.foreach { sentence =>
      println(s"Processing: '$sentence'")
      val result = completePipeline.process(sentence)
      println(result)
      println()
    }
    
    // Show the final word frequency counts
    println("Final word frequency counts:")
    wordCountMemory.get.toList.sortBy(-_._2).foreach { case (word, count) =>
      println(s"  $word: $count")
    }
  }
  
  /**
   * Alternative demo showing how to use a memory cell with a clock
   * to process text at regular intervals
   */
  def clockDemo(): Unit = {
    val textMemory = new CircuitMemory.MemoryCell[String]("")
    val clock = new CircuitMemory.Clock(5) // Tick every 5 cycles
    
    // Agent that processes whatever text is in memory on clock pulse
    val processor = Agent[Unit, Option[String]] { _ =>
      if (clock.tick()) {
        val text = textMemory.get
        if (text.nonEmpty) {
          println(s"Processing: $text")
          textMemory.set("") // Clear after processing
          Some(text)
        } else None
      } else None
    }
    
    // Simulate adding text to memory and clock ticks
    textMemory.set("Hello world")
    (1 to 10).foreach { i =>
      println(s"Tick $i")
      processor.process(())
    }
  }
  
  /**
   * Demo showing how to use bit packing for efficient signal transmission
   */
  def bitPackingDemo(): Unit = {
    import com.agenticai.core.memory.circuits.BitPacking
    
    // Pack multiple values into a single signal
    val values = List(3, 7, 12, 5)
    val bitWidths = List(4, 4, 4, 4)
    
    BitPacking.packInts(values, bitWidths) match {
      case Right(packed) =>
        println(s"Packed value: $packed")
        
        // Unpack the values
        BitPacking.unpackInts(packed, bitWidths) match {
          case Right(unpacked) =>
            println(s"Unpacked values: ${unpacked.mkString(", ")}")
          case Left(error) =>
            println(s"Unpack error: $error")
        }
        
      case Left(error) =>
        println(s"Pack error: $error")
    }
  }
}