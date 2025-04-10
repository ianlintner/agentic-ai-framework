package com.agenticai.core.memory.circuits

import zio.*
import zio.test.*
import zio.test.Assertion.*

object CircuitMemorySpec extends ZIOSpecDefault:

  def spec = suite("BitPacking")(
    test("should pack and unpack integers") {
      val values    = List(3, 5, 7, 9)
      val bitWidths = List(4, 4, 4, 4)

      for
        packed   <- ZIO.fromEither(BitPacking.packInts(values, bitWidths))
        unpacked <- ZIO.fromEither(BitPacking.unpackInts(packed, bitWidths))
      yield assertTrue(unpacked == values)
    },
    test("should handle different bit widths") {
      val values    = List(1, 100, 3)
      val bitWidths = List(2, 8, 4) // 2 bits, 8 bits, 4 bits

      for
        packed   <- ZIO.fromEither(BitPacking.packInts(values, bitWidths))
        unpacked <- ZIO.fromEither(BitPacking.unpackInts(packed, bitWidths))
      yield assertTrue(unpacked == values)
    },
    test("should pack and unpack booleans") {
      val flags    = List(true, false, true, false, true)
      val packed   = BitPacking.packBooleans(flags)
      val unpacked = BitPacking.unpackBooleans(packed, flags.length)

      assertTrue(unpacked == flags)
    },
    test("should fail when value doesn't fit in bit width") {
      val values    = List(5, 20, 3) // 20 won't fit in 4 bits
      val bitWidths = List(4, 4, 4)

      val result = BitPacking.packInts(values, bitWidths)
      assertTrue(result.isLeft)
    },
    test("should fail when bit widths exceed 64") {
      val values    = List(1, 2, 3, 4, 5, 6, 7, 8, 9)
      val bitWidths = List.fill(9)(8) // 9*8 = 72 bits, which exceeds 64

      val result = BitPacking.packInts(values, bitWidths)
      assertTrue(result.isLeft)
    }
  )
