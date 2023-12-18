package errata.laws

import cats.syntax.all._
import cats.{Applicative, Monad}
import errata._

trait RaiseLaws[F[_], E] {
  implicit def F: Raise[F, E]

  def raiseFlatmap[A](e: E, f: A => F[A])(implicit M: Monad[F]): IsEq[F[A]] =
    F.raise(e).flatMap(f) <-> F.raise(e)

  def leftFromEither[A](e: E)(implicit A: Applicative[F]): IsEq[F[A]] =
    F.fromEither(Left[E, A](e)) <-> F.raise(e)

  def rightFromEither[A](a: A)(implicit A: Applicative[F]): IsEq[F[A]] =
    F.fromEither(Right[E, A](a)) <-> A.pure(a)

  def noneFromOption[A](e: E)(implicit A: Applicative[F]): IsEq[F[A]] =
    F.fromOption(e)(None: Option[A]) <-> F.raise(e)

  def someFromOption[A](a: A, e: E)(implicit A: Applicative[F]): IsEq[F[A]] =
    F.fromOption(e)(Some(a)) <-> A.pure(a)
}

object RaiseLaws {
  def apply[F[_], E](implicit ev: Raise[F, E]): RaiseLaws[F, E] =
    new RaiseLaws[F, E] { def F: Raise[F, E] = ev }
}