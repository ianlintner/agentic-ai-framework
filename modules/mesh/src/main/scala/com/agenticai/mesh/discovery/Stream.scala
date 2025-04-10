package com.agenticai.mesh.discovery

/** A simplified Stream trait for handling a stream of events. This is intended to be a lightweight
  * interface for event subscriptions.
  *
  * @tparam A
  *   The type of events in the stream
  */
trait Stream[A]:
  /** Process each event in the stream using the provided function.
    *
    * @param f
    *   The function to apply to each event
    */
  def forEach(f: A => Unit): Unit

object Stream:

  /** Create a simple stream implementation
    */
  def apply[A](consumer: (A => Unit) => Unit): Stream[A] = new Stream[A]:
    override def forEach(f: A => Unit): Unit = consumer(f)

  /** Backward compatibility for code that expects a two-parameter stream
    */
  type WithError[E, A] = Stream[A]

  /** Helper to create a stream with an error type (compatibility)
    */
  def withError[E, A](stream: Stream[A]): Stream[A] = stream
