package errata.laws

import cats.Applicative
import errata._

trait HandleToLaws[F[_], G[_], E] {
  implicit def F: HandleTo[F, G, E]

  def pureHandleWith[A](a: A, f: E => G[A])(implicit AF: Applicative[F], AG: Applicative[G]): IsEq[G[A]] =
    F.handleWith(AF.pure(a))(f) <-> AG.pure(a)

  def pureHandle[A](a: A, f: E => A)(implicit AF: Applicative[F], AG: Applicative[G]): IsEq[G[A]] =
    F.handle(AF.pure(a))(f) <-> AG.pure(a)

  def pureRestore[A](a: A)(implicit AF: Applicative[F], AG: Applicative[G]): IsEq[G[Option[A]]] =
    F.restore(AF.pure(a)) <-> AG.pure(Some[A](a))

  def pureAttempt[A](a: A)(implicit AF: Applicative[F], AG: Applicative[G]): IsEq[G[Either[E, A]]] =
    F.attempt(AF.pure(a)) <-> AG.pure(Right[E, A](a))
}

object HandleToLaws {
  def apply[F[_], G[_], E](implicit ev: HandleTo[F, G, E]): HandleToLaws[F, G, E] =
    new HandleToLaws[F, G, E] { def F: HandleTo[F, G, E] = ev }
}
