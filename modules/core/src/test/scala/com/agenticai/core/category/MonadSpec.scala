package com.agenticai.core.category

import zio.*
import zio.test.*
import zio.test.Assertion.*

/** Test suite for the Monad typeclass.
  */
object MonadSpec extends ZIOSpecDefault:

  /** Test implementation of Monad for Option.
    */
  val optionMonad: Monad[Option] = new Monad[Option]:
    def pure[A](a: A): Option[A] = Some(a)

    def flatMap[A, B](fa: Option[A])(f: A => Option[B]): Option[B] =
      fa.flatMap(f)

    override def map[A, B](fa: Option[A])(f: A => B): Option[B] =
      fa.map(f)

  /** Test implementation of Monad for List.
    */
  val listMonad: Monad[List] = new Monad[List]:
    def pure[A](a: A): List[A] = List(a)

    def flatMap[A, B](fa: List[A])(f: A => List[B]): List[B] =
      fa.flatMap(f)

    override def map[A, B](fa: List[A])(f: A => B): List[B] =
      fa.map(f)

  /** Test implementation of Monad for ZIO.
    */
  def zioMonad[R]: Monad[({ type F[A] = ZIO[R, Throwable, A] })#F] =
    new Monad[({ type F[A] = ZIO[R, Throwable, A] })#F]:
      def pure[A](a: A): ZIO[R, Throwable, A] = ZIO.succeed(a)

      def flatMap[A, B](fa: ZIO[R, Throwable, A])(
          f: A => ZIO[R, Throwable, B]
      ): ZIO[R, Throwable, B] =
        fa.flatMap(f)

      override def map[A, B](fa: ZIO[R, Throwable, A])(f: A => B): ZIO[R, Throwable, B] =
        fa.map(f)

  /** Implementation of Monad laws for testing.
    */
  // Changed the parameter name to avoid conflicts
  def testMonadLaws[F[_]](
      monad: Monad[F],
      eqFn: [A] => (F[A], F[A]) => Boolean
  ): Spec[Any, Nothing] =
    val laws = new Monad.Laws[F]:
      def F: Monad[F] = monad

      override def leftIdentityLaw[A, B](a: A, f: A => F[B]): Boolean =
        val fa      = F.pure(a)
        val result1 = F.flatMap(fa)(f)
        val result2 = f(a)
        // Use the function directly from the outer scope
        eqFn[B](result1, result2)

      override def rightIdentityLaw[A](fa: F[A]): Boolean =
        val result1 = F.flatMap(fa)(a => F.pure(a))
        val result2 = fa
        // Use the function directly from the outer scope
        eqFn[A](result1, result2)

      override def associativityLaw[A, B, C](
          fa: F[A],
          f: A => F[B],
          g: B => F[C]
      ): Boolean =
        val result1 = F.flatMap(F.flatMap(fa)(f))(g)
        val result2 = F.flatMap(fa)(a => F.flatMap(f(a))(g))
        // Use the function directly from the outer scope
        eqFn[C](result1, result2)

    suite("Monad Laws")(
      test("left identity") {
        assertTrue(laws.leftIdentityLaw(42, (n: Int) => monad.pure(n * 2)))
      },
      test("right identity") {
        assertTrue(laws.rightIdentityLaw(monad.pure(42)))
      },
      test("associativity") {
        val fa = monad.pure(5)
        val f  = (n: Int) => monad.pure(n * 2)
        val g  = (n: Int) => monad.pure(n + 1)

        assertTrue(laws.associativityLaw(fa, f, g))
      }
    )

  /** Test spec for Monad implementations.
    */
  def spec = suite("MonadSpec")(
    suite("Option Monad")(
      testMonadLaws(optionMonad, [A] => (a: Option[A], b: Option[A]) => a == b),
      test("flatMap behavior") {
        val opt    = Some(5)
        val result = optionMonad.flatMap(opt)(x => Some(x * 2))
        assertTrue(result == Some(10))
      },
      test("flatten behavior") {
        val nested = Some(Some(42))
        val result = optionMonad.flatten(nested)
        assertTrue(result == Some(42))
      }
    ),
    suite("List Monad")(
      testMonadLaws(listMonad, [A] => (a: List[A], b: List[A]) => a == b),
      test("flatMap behavior") {
        val list   = List(1, 2, 3)
        val result = listMonad.flatMap(list)(x => List(x, x * 2))
        assertTrue(result == List(1, 2, 2, 4, 3, 6))
      },
      test("flatten behavior") {
        val nested = List(List(1, 2), List(3, 4))
        val result = listMonad.flatten(nested)
        assertTrue(result == List(1, 2, 3, 4))
      }
    ),
    suite("ZIO Monad")(
      test("flatMap behavior") {
        for
          monad <- ZIO.succeed(zioMonad[Any])
          computation = monad.pure(5)
          result <- monad.flatMap(computation)(x => monad.pure(x * 2))
        yield assertTrue(result == 10)
      },
      test("flatten behavior") {
        for
          monad <- ZIO.succeed(zioMonad[Any])
          nested = monad.pure(monad.pure(42))
          result <- monad.flatten(nested)
        yield assertTrue(result == 42)
      }
    )
  )
