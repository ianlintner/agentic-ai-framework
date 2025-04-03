package com.agenticai.core.memory.circuits

/**
 * Utility for bit packing operations, inspired by Factorio's circuit networks.
 * 
 * This allows multiple values to be packed into a single integer,
 * similar to how Factorio's combinators can encode multiple signals.
 */
object BitPacking {
  /**
   * Pack multiple integers into a single 64-bit value
   * 
   * @param values The values to pack
   * @param bitWidths The bit width for each value
   * @return Either an error or the packed value
   */
  def packInts(values: List[Int], bitWidths: List[Int]): Either[String, Long] = {
    if (values.length != bitWidths.length) {
      return Left(s"Values list length (${values.length}) must match bitWidths list length (${bitWidths.length})")
    }
    
    if (bitWidths.sum > 64) {
      return Left(s"Total bit width (${bitWidths.sum}) exceeds 64 bits")
    }
    
    // Check if any value exceeds its bit width
    val valueExceedsBitWidth = values.zip(bitWidths).exists { case (value, width) =>
      value >= (1 << width) || value < 0
    }
    
    if (valueExceedsBitWidth) {
      return Left("One or more values exceed their allocated bit width")
    }
    
    // Pack the values
    var result: Long = 0
    var bitPosition = 0
    
    for ((value, width) <- values.zip(bitWidths)) {
      result |= (value.toLong & ((1L << width) - 1)) << bitPosition
      bitPosition += width
    }
    
    Right(result)
  }
  
  /**
   * Unpack a single 64-bit value into multiple integers
   * 
   * @param packed The packed value
   * @param bitWidths The bit width for each value
   * @return Either an error or the unpacked values
   */
  def unpackInts(packed: Long, bitWidths: List[Int]): Either[String, List[Int]] = {
    if (bitWidths.sum > 64) {
      return Left(s"Total bit width (${bitWidths.sum}) exceeds 64 bits")
    }
    
    var bitPosition = 0
    val values = bitWidths.map { width =>
      val mask = (1L << width) - 1
      val value = (packed >> bitPosition) & mask
      bitPosition += width
      value.toInt
    }
    
    Right(values)
  }
  
  /**
   * Pack multiple boolean values into a single integer
   * 
   * @param flags The boolean values to pack
   * @return The packed value
   */
  def packBooleans(flags: List[Boolean]): Int = {
    var result = 0
    for ((flag, index) <- flags.zipWithIndex) {
      if (flag) {
        result |= 1 << index
      }
    }
    result
  }
  
  /**
   * Unpack multiple boolean values from a single integer
   * 
   * @param packed The packed value
   * @param count The number of boolean values to unpack
   * @return The unpacked boolean values
   */
  def unpackBooleans(packed: Int, count: Int): List[Boolean] = {
    (0 until count).map(i => ((packed >> i) & 1) == 1).toList
  }
}