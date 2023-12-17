package errors.laws

import cats.Applicative
import errors._

trait ErrorsLaws[F[_], E] extends RaiseLaws[F, E] with HandleLaws[F, E] with ErrorsToLaws[F, F, E] with TransformToLaws[F, F, E, E] {
  implicit def F: Errors[F, E]

  def pureAdaptError[A](a: A, f: PartialFunction[E, E])(implicit A: Applicative[F]): IsEq[F[A]] =
    F.adaptError(A.pure(a))(f) <-> A.pure(a)

  def raiseAdaptError[A](e: E, f: PartialFunction[E, E])(implicit A: Applicative[F]): IsEq[F[A]] =
    F.adaptError(F.raise[A](e))(f) <-> f.lift(e).fold(F.raise[A](e))(F.raise[A])
}

object ErrorsLaws {
  def apply[F[_], E](implicit ev: Errors[F, E]): ErrorsLaws[F, E] =
    new ErrorsLaws[F, E] { def F: Errors[F, E] = ev }
}
