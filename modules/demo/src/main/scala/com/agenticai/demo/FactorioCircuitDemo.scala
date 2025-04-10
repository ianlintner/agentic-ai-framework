package com.agenticai.demo

import com.agenticai.core.memory.circuits.BitPacking
import com.agenticai.core.memory.circuits.CircuitMemory
import com.agenticai.core.memory.circuits.AgentCombinators.{pipeline, Agent}
import com.agenticai.core.memory.circuits.examples.TextProcessingDemo

/** Demo showcasing Factorio-inspired circuit patterns in the Agentic AI framework.
  *
  * This demo provides a user-friendly interface to explore different circuit patterns:
  *   - Memory cells for storing state
  *   - Bit packing for efficient data transfer
  *   - Pipeline processing using agent combinators
  *   - Clock-based timing systems
  */
object FactorioCircuitDemo:

  def main(args: Array[String]): Unit =
    printHeader()

    args.headOption match
      case Some("text")        => runTextProcessingDemo()
      case Some("bit-packing") => runBitPackingDemo()
      case Some("clock")       => runClockDemo()
      case Some("memory")      => runMemoryDemo()
      case Some("help")        => printHelp()
      case _                   => runAllDemos()

  def printHeader(): Unit =
    println("""
      |╔═══════════════════════════════════════════════════╗
      |║  Factorio Circuit Patterns in Agentic AI          ║
      |╚═══════════════════════════════════════════════════╝
      |
      |Demonstrating how patterns from Factorio's circuit networks
      |can be applied to AI agent composition and data processing.
      |""".stripMargin)

  def printHelp(): Unit =
    println("""
      |Usage: FactorioCircuitDemo [demo-name]
      |
      |Available demos:
      |  text       - Text processing pipeline demo
      |  bit-packing - Bit packing for efficient data transfer
      |  clock      - Clock-based timing demo
      |  memory     - Memory cell usage demo
      |  help       - Show this help message
      |
      |Run without arguments to execute all demos in sequence.
      |""".stripMargin)

  def runAllDemos(): Unit =
    println("Running all demos in sequence...\n")

    println("=== Memory Cell Demo ===")
    runMemoryDemo()
    println("\nPress Enter to continue to the next demo...")
    scala.io.StdIn.readLine()

    println("\n=== Clock Demo ===")
    runClockDemo()
    println("\nPress Enter to continue to the next demo...")
    scala.io.StdIn.readLine()

    println("\n=== Bit Packing Demo ===")
    runBitPackingDemo()
    println("\nPress Enter to continue to the next demo...")
    scala.io.StdIn.readLine()

    println("\n=== Text Processing Demo ===")
    runTextProcessingDemo()

  def runTextProcessingDemo(): Unit =
    println("Running text processing demo using circuit patterns...\n")
    TextProcessingDemo.main(Array.empty)

  def runBitPackingDemo(): Unit =
    println("Running bit packing demo...\n")

    // Basic bit packing example
    val values    = List(3, 7, 12, 5)
    val bitWidths = List(4, 4, 4, 4)

    println(s"Input values: ${values.mkString(", ")}")
    println(s"Bit widths: ${bitWidths.mkString(", ")}")

    BitPacking.packInts(values, bitWidths) match
      case Right(packed) =>
        println(s"Packed value: $packed (${packed.toBinaryString} in binary)")

        BitPacking.unpackInts(packed, bitWidths) match
          case Right(unpacked) =>
            println(s"Unpacked values: ${unpacked.mkString(", ")}")
            println(s"Original values preserved: ${unpacked == values}")
          case Left(error) =>
            println(s"Unpack error: $error")

      case Left(error) =>
        println(s"Pack error: $error")

    // Advanced example - variable width fields
    println("\nAdvanced example - variable width fields:")
    val advancedValues    = List(3, 5, 9, 15)
    val advancedBitWidths = List(2, 3, 4, 5)

    println(s"Input values: ${advancedValues.mkString(", ")}")
    println(
      s"Bit widths: ${advancedBitWidths.mkString(", ")} (using minimum bits needed for each value)"
    )

    BitPacking.packInts(advancedValues, advancedBitWidths) match
      case Right(packed) =>
        println(s"Packed value: $packed (${packed.toBinaryString} in binary)")
        println(
          s"Total bits used: ${advancedBitWidths.sum} (compared to ${advancedValues.size * 32} bits for separate integers)"
        )

        BitPacking.unpackInts(packed, advancedBitWidths) match
          case Right(unpacked) =>
            println(s"Unpacked values: ${unpacked.mkString(", ")}")
            println(s"Original values preserved: ${unpacked == advancedValues}")
          case Left(error) =>
            println(s"Unpack error: $error")

      case Left(error) =>
        println(s"Pack error: $error")

  def runClockDemo(): Unit =
    println("Running clock demo...\n")

    val clock      = new CircuitMemory.Clock(5) // Tick every 5 cycles
    val textMemory = new CircuitMemory.MemoryCell[String]("")

    // Agent that processes whatever text is in memory on clock pulse
    val processor = Agent[Unit, Option[String]] { _ =>
      if clock.tick() then
        val text = textMemory.get
        if text.nonEmpty then
          println(s"Clock triggered! Processing: $text")
          textMemory.set("") // Clear after processing
          Some(text)
        else {
          println("Clock triggered! No text to process.")
          None
        }
      else {
        println("Clock tick (no action)")
        None
      }
    }

    // Simulate adding text to memory and clock ticks
    println("Starting clock cycle simulation...")
    println("Initial state: empty memory cell")

    for i <- 1 to 3 do
      println(s"\nTick $i:")
      processor.process(())

    println("\nAdding text to memory: 'Hello, circuits!'")
    textMemory.set("Hello, circuits!")

    for i <- 4 to 8 do
      println(s"\nTick $i:")
      processor.process(())

    println("\nAdding text to memory: 'Factorio patterns are powerful!'")
    textMemory.set("Factorio patterns are powerful!")

    for i <- 9 to 13 do
      println(s"\nTick $i:")
      processor.process(())

  def runMemoryDemo(): Unit =
    println("Running memory cell demo...\n")

    val counterMemory = new CircuitMemory.MemoryCell[Int](0)
    val labelsMemory  = new CircuitMemory.MemoryCell[List[String]](List.empty)

    println("Created two memory cells:")
    println("  - counterMemory: stores an integer counter")
    println("  - labelsMemory: stores a list of string labels")

    // Define some agents that work with the memory cells
    val incrementCounter = Agent[Int, Int] { increment =>
      val current  = counterMemory.get
      val newValue = current + increment
      counterMemory.set(newValue)
      println(s"Counter incremented by $increment: $current -> $newValue")
      newValue
    }

    val addLabel = Agent[String, List[String]] { label =>
      val current   = labelsMemory.get
      val newLabels = current :+ label
      labelsMemory.set(newLabels)
      println(s"Label added: '$label'")
      println(s"Current labels: ${newLabels.mkString(", ")}")
      newLabels
    }

    // Demonstrate memory persistence
    println("\nDemonstrating memory cell persistence:")

    println("\n1. Incrementing counter by 5")
    incrementCounter.process(5)

    println("\n2. Adding label 'first'")
    addLabel.process("first")

    println("\n3. Incrementing counter by 10")
    incrementCounter.process(10)

    println("\n4. Adding label 'second'")
    addLabel.process("second")

    println("\n5. Reading memory cell states:")
    println(s"   Counter value: ${counterMemory.get}")
    println(s"   Labels: ${labelsMemory.get.mkString(", ")}")

    // Reset and show the effects
    println("\n6. Resetting counter to 0")
    counterMemory.set(0)
    println(s"   New counter value: ${counterMemory.get}")

    println("\n7. Clearing all labels")
    labelsMemory.set(List.empty)
    println(s"   Labels after clearing: ${labelsMemory.get.mkString(", ")}")
