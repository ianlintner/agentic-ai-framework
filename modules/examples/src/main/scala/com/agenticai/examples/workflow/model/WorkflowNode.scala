package com.agenticai.examples.workflow.model

import com.agenticai.examples.workflow.agent.Agent
import java.util.UUID
import scala.reflect.ClassTag
import zio.*

/** Base trait for workflow nodes to handle type erasure */
trait BaseWorkflowNode:
  def id: String
  def name: String
  def inputType: String
  def outputType: String
  def agent: Agent[_, _]
  def processInput(input: Any): ZIO[Any, Throwable, Any]

/** Represents a node in a workflow graph with specific input and output types */
case class WorkflowNode[I: ClassTag, O](
    id: String = UUID.randomUUID().toString,
    name: String,
    agent: Agent[I, O],
    inputType: String,
    outputType: String
) extends BaseWorkflowNode:
  override def processInput(input: Any): ZIO[Any, Throwable, Any] =
    input match
      case i: I => agent.process(i)
      case _ => ZIO.fail(new IllegalArgumentException(s"Expected input of type ${inputType} but got ${input.getClass.getName}"))