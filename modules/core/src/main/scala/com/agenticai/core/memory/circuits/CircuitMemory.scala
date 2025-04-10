package com.agenticai.core.memory.circuits

/** Implementation of circuit-based memory systems inspired by Factorio's combinators.
  *
  * This provides a foundation for implementing the various circuit patterns from the video, like
  * memory cells, clocks, shift registers, etc.
  */
object CircuitMemory:

  /** A Memory Cell that maintains state between operations
    *
    * @param initialValue
    *   The initial value stored in the cell
    * @tparam T
    *   The type of value stored in the cell
    */
  class MemoryCell[T](initialValue: T):
    private var value: T = initialValue

    /** Read the current value in the cell
      */
    def get: T = value

    /** Set a new value in the cell
      */
    def set(newValue: T): Unit = value = newValue

    /** Update the cell's value using a transformation function
      */
    def update(f: T => T): Unit = value = f(value)

    /** Create an agent interface to the memory cell
      */
    def asAgent: AgentCombinators.Agent[T => T, T] = AgentCombinators.Agent { f =>
      update(f)
      get
    }

  /** A Clock implementation that generates regular pulses
    *
    * @param interval
    *   The number of ticks between pulses
    */
  class Clock(interval: Int):
    private var currentTick: Int = 0

    /** Advances the clock by one tick and returns true if the clock has completed a cycle
      */
    def tick(): Boolean =
      currentTick = (currentTick + 1) % interval
      currentTick == 0

    /** Get the current tick count
      */
    def current: Int = currentTick

    /** Reset the clock to zero
      */
    def reset(): Unit =
      currentTick = 0

  /** A Shift Register that processes inputs sequentially through multiple stages
    *
    * @param stages
    *   The number of stages in the register
    * @tparam T
    *   The type of value processed by the register
    */
  class ShiftRegister[T](stages: Int):
    private val values: Array[Option[T]] = Array.fill(stages)(None)

    /** Push a new value into the register, shifting all other values down
      */
    def push(value: T): Option[T] =
      val result = values(stages - 1)
      (stages - 1 to 1 by -1).foreach(i => values(i) = values(i - 1))
      values(0) = Some(value)
      result

    /** Get the value at a specific stage
      */
    def get(stage: Int): Option[T] =
      if stage < 0 || stage >= stages then None else values(stage)

    /** Get all values in the register
      */
    def getAll: List[Option[T]] = values.toList

    /** Clear all values in the register
      */
    def clear(): Unit =
      (0 until stages).foreach(i => values(i) = None)

  /** A Ring Buffer that creates a continuous loop with a shift register
    *
    * @param capacity
    *   The capacity of the buffer
    * @tparam T
    *   The type of value stored in the buffer
    */
  class RingBuffer[T](capacity: Int):
    private val buffer: Array[Option[T]] = Array.fill(capacity)(None)
    private var readIndex                = 0
    private var writeIndex               = 0
    private var size                     = 0

    /** Add a value to the buffer
      */
    def write(value: T): Unit =
      buffer(writeIndex) = Some(value)
      writeIndex = (writeIndex + 1) % capacity
      if size < capacity then size += 1
      else readIndex = (readIndex + 1) % capacity

    /** Read the next value from the buffer
      */
    def read(): Option[T] =
      if size == 0 then return None

      val result = buffer(readIndex)
      buffer(readIndex) = None
      readIndex = (readIndex + 1) % capacity
      size -= 1
      result

    /** Check if the buffer is full
      */
    def isFull: Boolean = size == capacity

    /** Check if the buffer is empty
      */
    def isEmpty: Boolean = size == 0

    /** Get the current size of the buffer
      */
    def currentSize: Int = size
