package com.agenticai.mesh.protocol

import zio._
import com.agenticai.core.agent.Agent
import scala.reflect.ClassTag

/**
 * Serialization interface for the protocol.
 */
trait Serialization {
  /**
   * Serialize an object to bytes.
   *
   * @param value Object to serialize
   * @return Serialized bytes
   */
  def serialize[A](value: A): Task[Array[Byte]]
  
  /**
   * Deserialize bytes to an object.
   *
   * @param bytes Serialized bytes
   * @return Deserialized object
   */
  def deserialize[A: ClassTag](bytes: Array[Byte]): Task[A]
  
  /**
   * Serialize an agent.
   *
   * @param agent Agent to serialize
   * @return Serialized bytes
   */
  def serializeAgent[I, O](agent: Agent[I, O]): Task[Array[Byte]]
  
  /**
   * Deserialize an agent.
   *
   * @param bytes Serialized bytes
   * @return Deserialized agent
   */
  def deserializeAgent[I: ClassTag, O: ClassTag](bytes: Array[Byte]): Task[Agent[I, O]]
  
  /**
   * Get the type name for a type.
   *
   * @return Type name
   */
  def getTypeName[A: ClassTag]: String = {
    val tag = implicitly[ClassTag[A]]
    tag.runtimeClass.getName
  }
}

object Serialization {
  /**
   * Create a test serialization that doesn't actually serialize anything.
   * 
   * @return A no-op serialization for testing
   */
  def test: Serialization = new NoOpSerialization

  /**
   * No-op implementation for testing.
   */
  private class NoOpSerialization extends Serialization {
    import java.util.concurrent.ConcurrentHashMap
    import scala.collection.concurrent
    
    // Maps for storing references instead of actually serializing
    private val agentMap = new ConcurrentHashMap[Array[Byte], Agent[_, _]]()
    private val valueMap = new ConcurrentHashMap[Array[Byte], Any]()
    
    def serialize[A](value: A): Task[Array[Byte]] = ZIO.succeed {
      val bytes = UUID.randomUUID().toString.getBytes
      valueMap.put(bytes, value)
      bytes
    }
    
    def deserialize[A: ClassTag](bytes: Array[Byte]): Task[A] = ZIO.succeed {
      valueMap.get(bytes).asInstanceOf[A]
    }
    
    def serializeAgent[I, O](agent: Agent[I, O]): Task[Array[Byte]] = ZIO.succeed {
      val bytes = UUID.randomUUID().toString.getBytes
      agentMap.put(bytes, agent)
      bytes
    }
    
    def deserializeAgent[I: ClassTag, O: ClassTag](bytes: Array[Byte]): Task[Agent[I, O]] = ZIO.succeed {
      agentMap.get(bytes).asInstanceOf[Agent[I, O]]
    }
  }
}