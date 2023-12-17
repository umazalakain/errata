package errors.laws

import cats.Applicative
import errors._

trait HandleLaws[F[_], E] extends HandleToLaws[F, F, E] {
  implicit def F: Handle[F, E]

  def pureTryHandleWith[A](a: A, f: E => Option[F[A]])(implicit A: Applicative[F]): IsEq[F[A]] =
    F.tryHandleWith(A.pure(a))(f) <-> A.pure(a)

  def raiseTryHandleWith[A](e: E, f: E => Option[F[A]])(implicit A: Applicative[F], R: Raise[F, E]): IsEq[F[A]] =
    F.tryHandleWith(R.raise[A](e))(f) <-> f(e).getOrElse(R.raise[A](e))

  def pureTryHandle[A](a: A, f: E => Option[A])(implicit A: Applicative[F]): IsEq[F[A]] =
    F.tryHandle(A.pure(a))(f) <-> A.pure(a)

  def raiseTryHandle[A](e: E, f: E => Option[A])(implicit A: Applicative[F], R: Raise[F, E]): IsEq[F[A]] =
    F.tryHandle(R.raise[A](e))(f) <-> f(e).fold(R.raise[A](e))(A.pure)

  def pureRecoverWith[A](a: A, f: PartialFunction[E, F[A]])(implicit A: Applicative[F]): IsEq[F[A]] =
    F.recoverWith(A.pure(a))(f) <-> A.pure(a)

  def raiseRecoverWith[A](e: E, f: PartialFunction[E, F[A]])(implicit A: Applicative[F], R: Raise[F, E]): IsEq[F[A]] =
    F.recoverWith(R.raise[A](e))(f) <-> f.lift(e).getOrElse(R.raise[A](e))

  def pureRecover[A](a: A, f: PartialFunction[E, A])(implicit A: Applicative[F]): IsEq[F[A]] =
    F.recover(A.pure(a))(f) <-> A.pure(a)

  def raiseRecover[A](e: E, f: PartialFunction[E, A])(implicit A: Applicative[F], R: Raise[F, E]): IsEq[F[A]] =
    F.recover(R.raise[A](e))(f) <-> f.lift(e).fold(R.raise[A](e))(A.pure)

  def pureRestoreWith[A](a: A, fa: F[A])(implicit A: Applicative[F]): IsEq[F[A]] =
    F.restoreWith(A.pure(a))(fa) <-> A.pure(a)

  def raiseRestoreWith[A](e: E, fa: F[A])(implicit A: Applicative[F], R: Raise[F, E]): IsEq[F[A]] =
    F.restoreWith(R.raise[A](e))(fa) <-> fa
}

object HandleLaws {
  def apply[F[_], E](implicit ev: Handle[F, E]): HandleLaws[F, E] =
    new HandleLaws[F, E] { def F: Handle[F, E] = ev }
}
