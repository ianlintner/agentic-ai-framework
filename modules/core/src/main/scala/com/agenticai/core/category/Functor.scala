package com.agenticai.core.category

/** Functor typeclass representing a type that can be mapped over.
  *
  * @tparam F
  *   The higher-kinded type
  */
trait Functor[F[_]]:
  /** Map a function over a value in the context.
    *
    * @param fa
    *   Value in context F
    * @param f
    *   Function to apply
    * @return
    *   Mapped value in context F
    */
  def map[A, B](fa: F[A])(f: A => B): F[B]

  /** Replace all values with a constant value.
    *
    * @param fa
    *   Value in context F
    * @param b
    *   Constant value
    * @return
    *   Value in context F with all positions replaced by b
    */
  def as[A, B](fa: F[A], b: B): F[B] = map(fa)(_ => b)

  /** Lift a function to operate on functors.
    *
    * @param f
    *   Function to lift
    * @return
    *   Function that operates on F[A]
    */
  def lift[A, B](f: A => B): F[A] => F[B] = fa => map(fa)(f)

object Functor:
  /** Accessor for the Functor instance of type F.
    *
    * @return
    *   The Functor instance
    */
  def apply[F[_]](implicit F: Functor[F]): Functor[F] = F

  /** Create a Functor instance from a map function.
    *
    * @param mapF
    *   Implementation of map
    * @return
    *   A Functor instance
    */
  def make[F[_]](mapF: [A, B] => (F[A], A => B) => F[B]): Functor[F] =
    new Functor[F]:
      def map[A, B](fa: F[A])(f: A => B): F[B] = mapF(fa, f)

  /** Laws that all Functor instances should satisfy.
    */
  trait Laws[F[_]]:
    def F: Functor[F]

    /** Identity: map with the identity function is a no-op map(fa)(identity) == fa
      */
    def identityLaw[A](fa: F[A]): Boolean

    /** Composition: mapping with f and then g is the same as mapping with f andThen g
      * map(map(fa)(f))(g) == map(fa)(f andThen g)
      */
    def compositionLaw[A, B, C](fa: F[A], f: A => B, g: B => C): Boolean
