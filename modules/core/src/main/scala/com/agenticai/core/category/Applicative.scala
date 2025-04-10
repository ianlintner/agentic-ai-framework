package com.agenticai.core.category

/** Applicative typeclass, extending Functor with the ability to wrap values and apply functions in
  * the context.
  */
trait Applicative[F[_]] extends Functor[F]:
  /** Wrap a value in the context.
    *
    * @param a
    *   Value to wrap
    * @return
    *   Value wrapped in the context F
    */
  def pure[A](a: A): F[A]

  /** Apply a function in a context to a value in a context.
    *
    * @param ff
    *   Function in context F
    * @param fa
    *   Value in context F
    * @return
    *   Result of applying function to value, in context F
    */
  def ap[A, B](ff: F[A => B])(fa: F[A]): F[B]

  /** Implementation of map in terms of pure and ap. Default implementation is provided but can be
    * overridden for efficiency.
    */
  override def map[A, B](fa: F[A])(f: A => B): F[B] =
    ap(pure(f))(fa)

  /** Combine two values in a context.
    *
    * @param fa
    *   First value in context F
    * @param fb
    *   Second value in context F
    * @return
    *   Combined values in context F
    */
  def product[A, B](fa: F[A], fb: F[B]): F[(A, B)] =
    ap(map(fa)(a => (b: B) => (a, b)))(fb)

  /** Apply a binary function to two values in a context.
    *
    * @param fa
    *   First value in context F
    * @param fb
    *   Second value in context F
    * @param f
    *   Binary function to apply
    * @return
    *   Result of applying function to values, in context F
    */
  def map2[A, B, C](fa: F[A], fb: F[B])(f: (A, B) => C): F[C] =
    map(product(fa, fb)) { case (a, b) => f(a, b) }

  /** Apply a ternary function to three values in a context.
    *
    * @param fa
    *   First value in context F
    * @param fb
    *   Second value in context F
    * @param fc
    *   Third value in context F
    * @param f
    *   Ternary function to apply
    * @return
    *   Result of applying function to values, in context F
    */
  def map3[A, B, C, D](fa: F[A], fb: F[B], fc: F[C])(f: (A, B, C) => D): F[D] =
    map(product(product(fa, fb), fc)) { case ((a, b), c) => f(a, b, c) }

object Applicative:
  /** Accessor for the Applicative instance of type F.
    *
    * @return
    *   The Applicative instance
    */
  def apply[F[_]](implicit F: Applicative[F]): Applicative[F] = F

  /** Create an Applicative instance from pure and ap functions.
    *
    * @param pureF
    *   Implementation of pure
    * @param apF
    *   Implementation of ap
    * @return
    *   An Applicative instance
    */
  def make[F[_]](
      pureF: [A] => A => F[A],
      apF: [A, B] => (F[A => B], F[A]) => F[B]
  ): Applicative[F] = new Applicative[F]:
    def pure[A](a: A): F[A]                     = pureF(a)
    def ap[A, B](ff: F[A => B])(fa: F[A]): F[B] = apF(ff, fa)

  /** Laws that all Applicative instances should satisfy.
    */
  trait Laws[F[_]]:
    def F: Applicative[F]

    /** Identity: ap with a pure identity function is a no-op ap(pure(identity))(fa) == fa
      */
    def identityLaw[A](fa: F[A]): Boolean =
      val identityF = F.pure[A => A](a => a) // Use explicit identity lambda instead of identity
      val result    = F.ap(identityF)(fa)
      result == fa

    /** Homomorphism: applying a pure function to a pure value is the same as applying the function
      * to the value and then using pure ap(pure(f))(pure(a)) == pure(f(a))
      */
    def homomorphismLaw[A, B](a: A, f: A => B): Boolean =
      val pureF   = F.pure(f)
      val pureA   = F.pure(a)
      val result1 = F.ap(pureF)(pureA)
      val result2 = F.pure(f(a))
      result1 == result2

    /** Interchange: applying a function in a context to a pure value is the same as applying pure
      * to that value and then applying it to the function ap(ff)(pure(a)) == ap(pure((f: A => B) =>
      * f(a)))(ff)
      */
    def interchangeLaw[A, B](a: A, ff: F[A => B]): Boolean =
      val pureA   = F.pure(a)
      val result1 = F.ap(ff)(pureA)

      val applyA     = (f: A => B) => f(a)
      val pureApplyA = F.pure(applyA)
      val result2    = F.ap(pureApplyA)(ff)

      result1 == result2

    /** Composition: composing functions in a context is the same as composing the functions and
      * then using pure ap(ap(ap(pure(compose))(u))(v))(w) == ap(u)(ap(v)(w)) where compose = (f: B
      * \=> C) => (g: A => B) => (a: A) => f(g(a))
      */
    def compositionLaw[A, B, C](
        u: F[B => C],
        v: F[A => B],
        w: F[A]
    ): Boolean =
      val compose     = (f: B => C) => (g: A => B) => (a: A) => f(g(a))
      val pureCompose = F.pure(compose)

      val step1   = F.ap(pureCompose)(u)
      val step2   = F.ap(step1)(v)
      val result1 = F.ap(step2)(w)

      val step3   = F.ap(v)(w)
      val result2 = F.ap(u)(step3)

      result1 == result2
