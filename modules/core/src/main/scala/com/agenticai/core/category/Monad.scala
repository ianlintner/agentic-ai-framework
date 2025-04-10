package com.agenticai.core.category

/** Monad typeclass, extending Applicative with the ability to flatten nested contexts via flatMap.
  */
trait Monad[F[_]] extends Applicative[F]:
  /** Monadic bind operation (flatMap).
    *
    * @param fa
    *   Value in context F
    * @param f
    *   Function that returns a value in context F
    * @return
    *   Result of binding fa to f
    */
  def flatMap[A, B](fa: F[A])(f: A => F[B]): F[B]

  /** Flatten a nested structure.
    *
    * @param ffa
    *   Nested structure
    * @return
    *   Flattened structure
    */
  def flatten[A](ffa: F[F[A]]): F[A] = flatMap(ffa)(a => a) // Use explicit identity lambda

  /** Kleisli composition of monadic functions.
    *
    * @param f
    *   First function
    * @param g
    *   Second function
    * @return
    *   Composed function
    */
  def compose[A, B, C](f: A => F[B], g: B => F[C]): A => F[C] =
    a => flatMap(f(a))(g)

  /** Implementation of Applicative's ap in terms of flatMap.
    */
  override def ap[A, B](ff: F[A => B])(fa: F[A]): F[B] =
    flatMap(ff)(f => map(fa)(f))

  /** Monadic version of product.
    */
  def flatProduct[A, B](fa: F[A])(f: A => F[B]): F[(A, B)] =
    flatMap(fa)(a => map(f(a))(b => (a, b)))

  /** Sequence actions, discarding the value of the first action.
    */
  def flatTap[A, B](fa: F[A])(f: A => F[B]): F[A] =
    flatMap(fa)(a => map(f(a))(_ => a))

object Monad:
  /** Accessor for the Monad instance of type F.
    *
    * @return
    *   The Monad instance
    */
  def apply[F[_]](implicit F: Monad[F]): Monad[F] = F

  /** Create a Monad instance from pure and flatMap functions.
    *
    * @param pureF
    *   Implementation of pure
    * @param flatMapF
    *   Implementation of flatMap
    * @return
    *   A Monad instance
    */
  def make[F[_]](
      pureF: [A] => A => F[A],
      flatMapF: [A, B] => (F[A], A => F[B]) => F[B]
  ): Monad[F] =
    new Monad[F]:
      def pure[A](a: A): F[A]                         = pureF(a)
      def flatMap[A, B](fa: F[A])(f: A => F[B]): F[B] = flatMapF(fa, f)

      override def map[A, B](fa: F[A])(f: A => B): F[B] =
        flatMap(fa)(a => pure(f(a)))

  /** Laws that all Monad instances should satisfy.
    */
  trait Laws[F[_]]:
    def F: Monad[F]

    /** Left identity: pure(a) flatMap f == f(a)
      */
    def leftIdentityLaw[A, B](a: A, f: A => F[B]): Boolean =
      val fa      = F.pure(a)
      val result1 = F.flatMap(fa)(f)
      val result2 = f(a)
      result1 == result2

    /** Right identity: fa flatMap pure == fa
      */
    def rightIdentityLaw[A](fa: F[A]): Boolean =
      val result1 = F.flatMap(fa)(a => F.pure(a))
      val result2 = fa
      result1 == result2

    /** Associativity: (fa flatMap f) flatMap g == fa flatMap (a => f(a) flatMap g)
      */
    def associativityLaw[A, B, C](
        fa: F[A],
        f: A => F[B],
        g: B => F[C]
    ): Boolean =
      val result1 = F.flatMap(F.flatMap(fa)(f))(g)
      val result2 = F.flatMap(fa)(a => F.flatMap(f(a))(g))
      result1 == result2
