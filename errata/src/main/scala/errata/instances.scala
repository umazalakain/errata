/*
 * Copyright 2023 Uma Zalakain
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package errata

import cats.{Applicative, ApplicativeError, Id, Monad}
import cats.data.{EitherT, Kleisli, Nested, OptionT, Validated, WriterT}

import scala.reflect.ClassTag

/* Note on the resolution of implicit instances:
 * The Scala compiler prioritises implicits in subtypes, e.g. if A <: B then implicits in A are given a higher
 * priority than those in B. We use this feature to direct the compiler to the most concrete instances first.
 */

final case class WrappedError[E](tag: ClassTag[E], value: E) extends Throwable
final case class UnexpectedClassTag[E1, E2](expected: ClassTag[E1], actual: ClassTag[E2]) extends Error

object Bases {
  trait Constituents {
    final implicit def raiseHandleToErrorsTo[F[_], G[_], E](implicit
        R: Raise[F, E],
        H: HandleTo[F, G, E]
    ): ErrorsTo[F, G, E] =
      new ErrorsTo[F, G, E] {
        override def raise[A](err: E): F[A] = R.raise(err)
        override def handleWith[A](fa: F[A])(f: E => G[A]): G[A] = H.handleWith(fa)(f)
      }

    final implicit def raiseHandleErrors[F[_], E](implicit
        R: Raise[F, E],
        H: Handle[F, E]
    ): Errors[F, E] =
      new Errors[F, E] {
        override def raise[A](err: E): F[A] = R.raise(err)
        override def tryHandleWith[A](fa: F[A])(f: E => Option[F[A]]): F[A] = H.tryHandleWith(fa)(f)
      }

    final implicit def raiseHandleToTransformTo[F[_], G[_], E1, E2](implicit
        H: HandleTo[F, G, E1],
        R: Raise[G, E2]
    ): TransformTo[F, G, E1, E2] =
      new TransformTo[F, G, E1, E2] {
        override def raise[A](err: E2): G[A] = R.raise(err)
        override def handleWith[A](fa: F[A])(f: E1 => G[A]): G[A] = H.handleWith(fa)(f)
      }
  }

  trait ThrowableInstances extends Constituents {
    final def handleThrowable[F[_], E](etag: ClassTag[E])(implicit F: Handle[F, Throwable]): Handle[F, E] =
      new Handle[F, E] {
        override def tryHandleWith[A](fa: F[A])(f: E => Option[F[A]]): F[A] =
          F.tryHandleWith(fa) {
            case WrappedError(tag, value) =>
              if (tag == etag) f(value.asInstanceOf[E])
              else throw UnexpectedClassTag(etag, tag)
            case _ => None
          }
      }

    final def raiseThrowable[F[_], E](etag: ClassTag[E])(implicit F: Raise[F, Throwable]): Raise[F, E] =
      new Raise[F, E] {
        override def raise[A](err: E): F[A] = F.raise(WrappedError(etag, err))
      }

    final def errorsThrowable[F[_], E](
        etag: ClassTag[E]
    )(implicit F: Raise[F, Throwable], G: Handle[F, Throwable]): Errors[F, E] =
      raiseHandleErrors(raiseThrowable(etag), handleThrowable(etag))
  }

  trait ApplicativeErrorInstances extends ThrowableInstances {
    // If using cats.ApplicativeError on the outer layer but want to use errors
    final implicit def errorByCatsError[F[_], E](implicit
        F: ApplicativeError[F, E]
    ): Errors[F, E] =
      new Errors[F, E] {
        override def tryHandleWith[A](fa: F[A])(f: E => Option[F[A]]): F[A] =
          F.recoverWith(fa)(f.unlift)
        override def raise[A](err: E): F[A] =
          F.raiseError(err)
      }

    object inverse {
      // If using errors on the outer layer but want to use cats.ApplicativeError
      final implicit def catsErrorByError[F[_], E](implicit
          FE: Errors[F, E],
          A: Applicative[F]
      ): ApplicativeError[F, E] =
        new ApplicativeError[F, E] {
          override def raiseError[A](e: E): F[A] =
            FE.raise(e)
          override def handleErrorWith[A](fa: F[A])(f: E => F[A]): F[A] =
            FE.handleWith(fa)(f)
          override def ap[A, B](ff: F[A => B])(fa: F[A]): F[B] =
            A.ap(ff)(fa)
          override def pure[A](x: A): F[A] =
            A.pure(x)
        }
    }
  }

  trait RecursiveRaiseInstances extends ApplicativeErrorInstances {
    final implicit def kleisliRaise[F[_], L, E](implicit
        R: Raise[F, E]
    ): Raise[Kleisli[F, L, _], E] =
      new Raise[Kleisli[F, L, _], E] {
        override def raise[A](err: E): Kleisli[F, L, A] =
          Kleisli(_ => R.raise[A](err))
      }

    final implicit def writerTRaise[F[_], L, E](implicit
        R: Raise[F, E]
    ): Raise[WriterT[F, L, _], E] =
      new Raise[WriterT[F, L, _], E] {
        override def raise[A](err: E): WriterT[F, L, A] =
          WriterT(R.raise[(L, A)](err))
      }

    final implicit def nestedRaise[F[_], H[_], E](implicit
        R: Raise[F, E],
        AF: Applicative[F],
        AH: Applicative[H]
    ): Raise[Nested[F, H, _], E] =
      new Raise[Nested[F, H, _], E] {
        override def raise[A](err: E): Nested[F, H, A] =
          Nested(AF.map(R.raise[A](err))(AH.pure))
      }
  }

  trait RecursiveHandleToInstances extends RecursiveRaiseInstances {
    final implicit def kleisliHandleTo[F[_], G[_], L, E](implicit
        H: HandleTo[F, G, E]
    ): HandleTo[Kleisli[F, L, _], Kleisli[G, L, _], E] =
      new HandleTo[Kleisli[F, L, _], Kleisli[G, L, _], E] {
        override def handleWith[A](fa: Kleisli[F, L, A])(f: E => Kleisli[G, L, A]): Kleisli[G, L, A] =
          Kleisli(k => H.handleWith(fa.run(k))(f `andThen` (_.run(k))))
      }

    final implicit def nestedHandleTo[F[_], G[_], H[_], E](implicit
        H: HandleTo[F, G, E]
    ): HandleTo[Nested[F, H, _], Nested[G, H, _], E] =
      new HandleTo[Nested[F, H, _], Nested[G, H, _], E] {
        override def handleWith[A](fa: Nested[F, H, A])(f: E => Nested[G, H, A]): Nested[G, H, A] =
          Nested(H.handleWith(fa.value)(f `andThen` (_.value)))
      }

    final implicit def writerTHandleTo[F[_], G[_], L, E](implicit
        H: HandleTo[F, G, E]
    ): HandleTo[WriterT[F, L, _], WriterT[G, L, _], E] =
      new HandleTo[WriterT[F, L, _], WriterT[G, L, _], E] {
        override def handleWith[A](fa: WriterT[F, L, A])(f: E => WriterT[G, L, A]): WriterT[G, L, A] =
          WriterT(H.handleWith(fa.run)(f `andThen` (_.run)))
      }
  }

  trait ErrorsInstances extends RecursiveHandleToInstances {
    final implicit def eitherInstance1[E]: Errors[Either[E, _], E] =
      new Errors[Either[E, _], E] {
        override def raise[A](err: E): Either[E, A] =
          Left(err)
        override def tryHandleWith[A](fa: Either[E, A])(
            f: E => Option[Either[E, A]]
        ): Either[E, A] =
          fa.fold(e => f(e).getOrElse(fa), Right.apply)
      }

    final implicit val optionInstance1: Errors[Option, Unit] =
      new Errors[Option, Unit] {
        override def raise[A](err: Unit): Option[A] =
          None
        override def tryHandleWith[A](fa: Option[A])(
            f: Unit => Option[Option[A]]
        ): Option[A] =
          fa.orElse(f(()).flatten)
      }
  }

  trait ErrorsToInstances extends ErrorsInstances {
    final implicit def eitherTInstance[F[_], E](implicit
        F: Monad[F]
    ): ErrorsTo[EitherT[F, E, _], F, E] =
      new ErrorsTo[EitherT[F, E, _], F, E] {
        override def raise[A](err: E): EitherT[F, E, A] =
          EitherT.leftT(err)
        override def handleWith[A](fa: EitherT[F, E, A])(f: E => F[A]): F[A] =
          fa.foldF[A](f, F.pure)
      }

    final implicit def optionTInstance[F[_]](implicit
        F: Monad[F]
    ): ErrorsTo[OptionT[F, _], F, Unit] =
      new ErrorsTo[OptionT[F, _], F, Unit] {
        override def raise[A](err: Unit): OptionT[F, A] =
          OptionT.none
        override def handleWith[A](fa: OptionT[F, A])(f: Unit => F[A]): F[A] =
          fa.getOrElseF(f(()))
      }

    final implicit def eitherInstance[E]: ErrorsTo[Either[E, _], Id, E] =
      new ErrorsTo[Either[E, _], Id, E] {
        override def raise[A](err: E): Either[E, A] =
          Left(err)
        override def handleWith[A](fa: Either[E, A])(f: E => Id[A]): Id[A] =
          fa.fold(f, identity)
      }

    final implicit val optionInstance: ErrorsTo[Option, Id, Unit] =
      new ErrorsTo[Option, Id, Unit] {
        override def raise[A](err: Unit): Option[A] =
          None
        override def handleWith[A](fa: Option[A])(f: Unit => Id[A]): Id[A] =
          fa.getOrElse(f(()))
      }

    final implicit def validatedInstance[E]: ErrorsTo[Validated[E, _], Id, E] =
      new ErrorsTo[Validated[E, _], Id, E] {
        override def raise[A](err: E): Validated[E, A] =
          Validated.Invalid(err)
        override def handleWith[A](fa: Validated[E, A])(f: E => Id[A]): Id[A] =
          fa.fold(f, identity)
      }

  }
}

object instances extends Bases.ErrorsToInstances {
  def classTag[A](implicit ct: ClassTag[A]): ClassTag[A] = ct
}
