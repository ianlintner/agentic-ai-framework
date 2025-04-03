package com.agenticai.core.agent

import com.agenticai.core.memory._
import zio._
import zio.stream._

trait Agent {
  def name: String
  def process(input: String): ZStream[MemorySystem, Throwable, String]
  def processStream(input: String): ZStream[MemorySystem, Throwable, String]
} 