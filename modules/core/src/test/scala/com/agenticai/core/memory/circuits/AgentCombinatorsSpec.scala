package com.agenticai.core.memory.circuits

import zio.*
import zio.test.*
import zio.test.Assertion.*

object AgentCombinatorsSpec extends ZIOSpecDefault:
  import AgentCombinators.*

  override def spec: Spec[Any, Any] = suite("AgentCombinators")(
    test("transform should apply a function to an agent's output") {
      val agent       = Agent[Int, Int](_ * 2)
      val transformed = transform(agent)(x => x + 1)

      assertTrue(transformed.process(5) == 11) // (5 * 2) + 1 = 11
    },
    test("filter should conditionally pass output based on a predicate") {
      val agent    = Agent[Int, Int](x => x * x)
      val filtered = filter(agent)(_ > 10)

      assertTrue(filtered.process(4) == Some(16)) && // 16 > 10, so Some(16)
      assertTrue(filtered.process(3) == None)        // 9 < 10, so None
    },
    test("pipeline should connect agents in sequence") {
      val double    = Agent[Int, Int](_ * 2)
      val addOne    = Agent[Int, Int](_ + 1)
      val pipeline1 = pipeline(double, addOne)

      assertTrue(pipeline1.process(5) == 11) // (5 * 2) + 1 = 11
    },
    test("parallel should process through multiple agents and combine results") {
      val square   = Agent[Int, Int](x => x * x)
      val double   = Agent[Int, Int](_ * 2)
      val combiner = parallel(square, double)((a, b) => a + b)

      assertTrue(combiner.process(5) == 35) // (5 * 5) + (5 * 2) = 25 + 10 = 35
    },
    test("shiftRegister should apply multiple transformations in sequence") {
      val stages = List(
        Agent[Int, Int](_ + 1),
        Agent[Int, Int](_ * 2),
        Agent[Int, Int](_ - 3)
      )
      val initial  = Agent[String, Int](_.toInt)
      val register = shiftRegister(stages)(initial)

      assertTrue(register.process("5") == 9) // ((5 + 1) * 2) - 3 = 12 - 3 = 9
    },
    test("feedback should apply an agent repeatedly") {
      val doubler  = Agent[Int, Int](_ * 2)
      val repeated = feedback(doubler)(3)

      assertTrue(repeated.process(2) == 16) // 2 * 2 * 2 * 2 = 16
    },
    test("MemoryCell should maintain state between operations") {
      // Each test case uses a fresh memory cell
      val initialCell  = new MemoryCell[Int](0)
      val initialValue = initialCell.get

      val setCell = new MemoryCell[Int](0)
      setCell.set(5)
      val setValue = setCell.get

      val updateCell = new MemoryCell[Int](5)
      updateCell.update(_ + 10)
      val updateValue = updateCell.get

      val agentCell  = new MemoryCell[Int](15)
      val agent      = agentCell.asAgent
      val result     = agent.process(_ + 7)
      val finalValue = agentCell.get

      assertTrue(initialValue == 0) &&
      assertTrue(setValue == 5) &&
      assertTrue(updateValue == 15) &&
      assertTrue(result == 22) &&
      assertTrue(finalValue == 22)
    }
  )
