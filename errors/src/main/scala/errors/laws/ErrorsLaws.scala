package errors.laws

import cats.syntax.all._
import cats.Monad
import errors._

trait ErrorsLaws[F[_], E] extends ErrorsToLaws[F, F, E] {
  implicit override def F: Errors[F, E]

  def handleWithRaise[A](fa: F[A]): IsEq[F[A]] =
    F.handleWith(fa)(F.raise) <-> fa

  def attemptFromEither[A](fa: F[A])(implicit M: Monad[F]): IsEq[F[A]] =
    F.attempt(fa).flatMap(F.fromEither) <-> fa

  def restoreFromOption[A](fa: F[A], e: E)(implicit M: Monad[F]): IsEq[F[A]] =
    F.restore(fa).flatMap(F.fromOption(e)) <-> fa
}

object ErrorsLaws {
  def apply[F[_], E](implicit ev: Errors[F, E]): ErrorsLaws[F, E] =
    new ErrorsLaws[F, E] { def F: Errors[F, E] = ev }
}