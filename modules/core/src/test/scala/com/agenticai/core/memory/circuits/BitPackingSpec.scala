package com.agenticai.core.memory.circuits

import zio.*
import zio.test.*
import zio.test.Assertion.*

object BitPackingSpec extends ZIOSpecDefault:

  def spec = suite("BitPacking")(
    test("pack byte values into an integer") {
      val values = Seq[Byte](1, 2, 3, 4)
      val packed = BitPacking.pack(values, 8)

      // Expected: 0x04030201 = 67305985
      assertTrue(packed == 67305985)
    },
    test("unpack an integer into byte values") {
      val packed = 67305985 // 0x04030201
      val values = BitPacking.unpack(packed, 8, 4)

      assertTrue(values == Seq[Byte](1, 2, 3, 4))
    },
    test("handle bit widths smaller than 8 for bytes") {
      val values = Seq[Byte](3, 2, 1, 7, 5, 4, 6)

      // Using 4 bits per value
      val packed   = BitPacking.pack(values, 4)
      val unpacked = BitPacking.unpack(packed, 4, 7)

      assertTrue(unpacked == values)
    },
    test("pack integer values into a long") {
      val values = Seq(10000, 20000, 30000)
      val packed = BitPacking.packToLong(values, 16)

      // Expected: 0x00007530 0000 4E20 0000 2710 (in reverse order)
      // = 30000 << 32 + 20000 << 16 + 10000
      val expected = 30000L << 32 | 20000L << 16 | 10000L
      assertTrue(packed == expected)
    },
    test("unpack a long into integer values") {
      // 30000 << 32 + 20000 << 16 + 10000
      val packed = 30000L << 32 | 20000L << 16 | 10000L
      val values = BitPacking.unpackFromLong(packed, 16, 3)

      assertTrue(values == Seq(10000, 20000, 30000))
    },
    test("pack fields with varying bit widths") {
      val fields = Seq(
        (3, 2), // 3 in 2 bits
        (5, 3), // 5 in 3 bits
        (9, 4), // 9 in 4 bits
        (15, 5) // 15 in 5 bits
      )

      val packed = BitPacking.packFields(fields)

      // Expected bits: 01111 1001 101 11
      // 3 = 11 (2 bits)
      // 5 = 101 (3 bits)
      // 9 = 1001 (4 bits)
      // 15 = 01111 (5 bits)
      // Total: 0001 1111 0011 0111 in binary = 8039 in decimal
      assertTrue(packed == 8039L)
    },
    test("unpack fields with varying bit widths") {
      val packed = 8039L // 0001 1111 0011 0111 in binary
      val widths = Seq(2, 3, 4, 5)

      val unpacked = BitPacking.unpackFields(packed, widths)

      assertTrue(unpacked == Seq(3, 5, 9, 15))
    },
    test("extract specific fields") {
      val packed = 8039L // 0001 1111 0011 0111 in binary

      assertTrue(
        BitPacking.extractField(packed, 0, 2) == 3 &&
          BitPacking.extractField(packed, 2, 3) == 5 &&
          BitPacking.extractField(packed, 5, 4) == 9 &&
          BitPacking.extractField(packed, 9, 5) == 15
      )
    },
    test("throw an exception when trying to pack too many values") {
      val values = Seq[Byte](1, 2, 3, 4, 5)

      // 5 values with 8 bits each = 40 bits, which exceeds 32-bit int
      for result <- ZIO.attempt(BitPacking.pack(values, 8)).exit
      yield assert(result)(fails(anything))
    },
    test("throw an exception when bit width is invalid") {
      val values = Seq[Byte](1, 2, 3)

      for
        result1 <- ZIO.attempt(BitPacking.pack(values, 0)).exit
        result2 <- ZIO.attempt(BitPacking.pack(values, 9)).exit
      yield assert(result1)(fails(anything)) && assert(result2)(fails(anything))
    },
    test("handle the maximum number of values that fit in an integer") {
      val values = (1 to 32).map(_.toByte)

      // 32 values with 1 bit each = 32 bits, which is the maximum for an int
      val packed   = BitPacking.pack(values.take(32), 1)
      val unpacked = BitPacking.unpack(packed, 1, 32)

      // Only the lowest bit of each value is preserved
      val expected = values.take(32).map(v => (v & 1).toByte)
      assertTrue(unpacked == expected)
    },
    test("handle the maximum number of values that fit in a long") {
      val values = (1 to 64).map(i => i)

      // 64 values with 1 bit each = 64 bits, which is the maximum for a long
      val packed   = BitPacking.packToLong(values.take(64), 1)
      val unpacked = BitPacking.unpackFromLong(packed, 1, 64)

      // Only the lowest bit of each value is preserved
      val expected = values.take(64).map(v => v & 1)
      assertTrue(unpacked == expected)
    },
    test("packInts packs values with variable bit widths") {
      val values    = List(3, 5, 9, 15)
      val bitWidths = List(2, 3, 4, 5)

      val result = BitPacking.packInts(values, bitWidths)
      assertTrue(
        result.isRight &&
          result.toOption.get == 8039L // Same as our packFields test
      )
    },
    test("packInts returns error when values and bit widths have different lengths") {
      val values    = List(1, 2, 3)
      val bitWidths = List(4, 4)

      val result = BitPacking.packInts(values, bitWidths)
      assertTrue(
        result.isLeft &&
          result.swap.toOption.get.contains("must match number of bit widths")
      )
    },
    test("packInts returns error when total bits exceed 64") {
      val values    = List(1, 2, 3, 4)
      val bitWidths = List(20, 20, 20, 20) // 80 bits total

      val result = BitPacking.packInts(values, bitWidths)
      assertTrue(
        result.isLeft &&
          result.swap.toOption.get.contains("exceeds 64-bit limit")
      )
    },
    test("unpackInts unpacks values with variable bit widths") {
      val packed    = 8039L // 0001 1111 0011 0111 in binary
      val bitWidths = List(2, 3, 4, 5)

      val result = BitPacking.unpackInts(packed, bitWidths)
      assertTrue(
        result.isRight &&
          result.toOption.get == List(3, 5, 9, 15)
      )
    },
    test("unpackInts returns error when total bits exceed 64") {
      val packed    = 123456789L
      val bitWidths = List(20, 20, 20, 20) // 80 bits total

      val result = BitPacking.unpackInts(packed, bitWidths)
      assertTrue(
        result.isLeft &&
          result.swap.toOption.get.contains("exceeds 64-bit limit")
      )
    }
  ) @@ TestAspect.sequential
