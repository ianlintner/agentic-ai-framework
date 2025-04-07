package com.agenticai.core.category

/**
 * Natural Transformation represents a conversion from one functor to another.
 * It is a mapping between functors that preserves the structure, meaning it
 * commutes with the functors' map operations.
 *
 * @tparam F Source functor
 * @tparam G Target functor
 */
trait ~>[F[_], G[_]] {
  /**
   * Apply the natural transformation to a value in functor F.
   *
   * @param fa Value in context F
   * @return Value in context G
   */
  def apply[A](fa: F[A]): G[A]
  
  /**
   * Compose this natural transformation with another.
   *
   * @param g Natural transformation from G to H
   * @return Natural transformation from F to H
   */
  def andThen[H[_]](g: G ~> H): F ~> H =
    new (F ~> H) {
      def apply[A](fa: F[A]): H[A] = g(~>.this.apply(fa))
    }
    
  /**
   * Compose this natural transformation with another in reverse order.
   *
   * @param g Natural transformation from E to F
   * @return Natural transformation from E to G
   */
  def compose[E[_]](g: E ~> F): E ~> G =
    g andThen this
}

object ~> {
  /**
   * Identity natural transformation.
   *
   * @return Natural transformation that doesn't change the functor
   */
  def identity[F[_]]: F ~> F =
    new (F ~> F) {
      def apply[A](fa: F[A]): F[A] = fa
    }
    
  /**
   * Create a natural transformation from a function.
   *
   * @param f Function that preserves the structure
   * @return Natural transformation
   */
  def fromFunction[F[_], G[_]](f: [A] => F[A] => G[A]): F ~> G =
    new (F ~> G) {
      def apply[A](fa: F[A]): G[A] = f[A](fa)
    }
    
  /**
   * Laws that natural transformations should satisfy.
   */
  trait Laws[F[_], G[_]] {
    def FG: F ~> G
    
    /**
     * Naturality: The natural transformation commutes with the functors' map operations.
     * FG(F.map(fa)(f)) == G.map(FG(fa))(f)
     */
    def naturalityLaw[A, B](fa: F[A], f: A => B)(implicit F: Functor[F], G: Functor[G]): Boolean
  }
}