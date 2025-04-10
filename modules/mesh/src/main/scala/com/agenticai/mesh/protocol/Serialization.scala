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
  def deserialize[A](bytes: Array[Byte])(implicit tag: ClassTag[A]): Task[A]
  
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
  def deserializeAgent[I, O](bytes: Array[Byte])(implicit tagI: ClassTag[I], tagO: ClassTag[O]): Task[Agent[I, O]]
  
  /**
   * Get the type name for a class.
   *
   * @param clazz Class to get name for
   * @return Type name
   */
  def getTypeName(clazz: Class[_]): String = {
    clazz.getName
  }
}

object Serialization {
  /**
   * Create a test serialization that doesn't actually serialize anything.
   * 
   * @return A no-op serialization for testing
   */
  def test: Serialization = new Serialization {
    import java.util.UUID
    
    // Simple implementation that just returns placeholders
    def serialize[A](value: A): Task[Array[Byte]] = 
      ZIO.succeed(UUID.randomUUID().toString.getBytes)
    
    def deserialize[A](bytes: Array[Byte])(implicit tag: ClassTag[A]): Task[A] = 
      ZIO.attempt(null.asInstanceOf[A])
    
    def serializeAgent[I, O](agent: Agent[I, O]): Task[Array[Byte]] = 
      ZIO.succeed(UUID.randomUUID().toString.getBytes)
    
    def deserializeAgent[I, O](bytes: Array[Byte])(implicit tagI: ClassTag[I], tagO: ClassTag[O]): Task[Agent[I, O]] = 
      ZIO.attempt(null.asInstanceOf[Agent[I, O]])
  }
}