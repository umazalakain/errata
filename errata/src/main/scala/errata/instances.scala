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
import cats.data.{EitherT, Kleisli, Nested, OptionT, ReaderT, Validated, WriterT}

import scala.reflect.ClassTag

/* Note on the resolution of implicit instances:
 * The Scala compiler prioritises implicits in subtypes, e.g. if A <: B then implicits in A are given a higher
 * priority than those in B. We use this feature to direct the compiler to the most concrete instances first.
 */

final case class WrappedError[E](tag: ClassTag[E], value: E) extends Throwable

object Bases {
  trait ThrowableInstances {
    final implicit def handleThrowable[F[_], E](implicit
        F: Handle[F, Throwable],
        etag: ClassTag[E]
    ): Handle[F, E] =
      new Handle[F, E] {
        override def tryHandleWith[A](fa: F[A])(f: E => Option[F[A]]): F[A] =
          F.tryHandleWith(fa) {
            case WrappedError(tag, value) if tag == etag =>
              f(value.asInstanceOf[E])
            case _ => None
          }
      }

    final implicit def raiseThrowable[F[_], E](implicit
        F: Raise[F, Throwable],
        etag: ClassTag[E]
    ): Raise[F, E] =
      new Raise[F, E] {
        override def raise[A](err: E): F[A] = F.raise(WrappedError(etag, err))
      }

    final implicit def errorsThrowable[F[_], E](implicit
        F: Errors[F, Throwable],
        etag: ClassTag[E]
    ): Errors[F, E] =
      new Errors[F, E] {
        override def raise[A](err: E): F[A] = F.raise(WrappedError(etag, err))
        override def tryHandleWith[A](fa: F[A])(f: E => Option[F[A]]): F[A] =
          F.tryHandleWith(fa) {
            case WrappedError(tag, value) if tag == etag =>
              f(value.asInstanceOf[E])
            case _ => None
          }
      }

    final implicit def transformToByCatsError[F[_], E1, E2](implicit
        H: Handle[F, E1],
        R: Raise[F, E2]
    ): TransformTo[F, F, E1, E2] =
      new TransformTo[F, F, E1, E2] {
        override def raise[A](err: E2): F[A] = R.raise(err)
        override def handleWith[A](fa: F[A])(f: E1 => F[A]): F[A] =
          H.handleWith(fa)(f)
      }
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

  trait ErrorsInstances extends ApplicativeErrorInstances {
    final implicit def readerTErrors[F[_], R, E](implicit
        F: Errors[F, E]
    ): Errors[ReaderT[F, R, _], E] =
      new Errors[ReaderT[F, R, _], E] {
        def raise[A](err: E): ReaderT[F, R, A] =
          ReaderT.liftF(F.raise(err))

        def tryHandleWith[A](fa: ReaderT[F, R, A])(
            f: E => Option[ReaderT[F, R, A]]
        ): ReaderT[F, R, A] =
          ReaderT(r => F.tryHandleWith(fa.run(r))(e => f(e).map(_.run(r))))

        def restore[A](fa: ReaderT[F, R, A])(implicit
            AF: Applicative[F]
        ): ReaderT[F, R, Option[A]] =
          ReaderT(r => F.restore(fa.run(r)))

        def lift[A](fa: ReaderT[F, R, A]): ReaderT[F, R, A] = fa
      }

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

    final implicit def kleisliInstance[F[_], G[_], L, E](implicit
        R: Raise[F, E],
        H: HandleTo[F, G, E]
    ): ErrorsTo[Kleisli[F, L, _], Kleisli[G, L, _], E] =
      new ErrorsTo[Kleisli[F, L, _], Kleisli[G, L, _], E] {
        override def raise[A](err: E): Kleisli[F, L, A] =
          Kleisli(_ => R.raise[A](err))
        override def handleWith[A](fa: Kleisli[F, L, A])(f: E => Kleisli[G, L, A]): Kleisli[G, L, A] =
          Kleisli(k => H.handleWith(fa.run(k))(f `andThen` (_.run(k))))
      }

    final implicit def nestedInstance[F[_], G[_], H[_], E](implicit
        R: Raise[F, E],
        H: HandleTo[F, G, E],
        AF: Applicative[F],
        AH: Applicative[H]
    ): ErrorsTo[Nested[F, H, _], Nested[G, H, _], E] =
      new ErrorsTo[Nested[F, H, _], Nested[G, H, _], E] {
        override def raise[A](err: E): Nested[F, H, A] =
          Nested(AF.map(R.raise[A](err))(AH.pure))

        override def handleWith[A](fa: Nested[F, H, A])(f: E => Nested[G, H, A]): Nested[G, H, A] =
          Nested(H.handleWith(fa.value)(f `andThen` (_.value)))
      }

    final implicit def writerTInstance[F[_], G[_], L, E](implicit
        R: Raise[F, E],
        H: HandleTo[F, G, E]
    ): ErrorsTo[WriterT[F, L, _], WriterT[G, L, _], E] =
      new ErrorsTo[WriterT[F, L, _], WriterT[G, L, _], E] {
        override def raise[A](err: E): WriterT[F, L, A] =
          WriterT(R.raise[(L, A)](err))

        override def handleWith[A](fa: WriterT[F, L, A])(f: E => WriterT[G, L, A]): WriterT[G, L, A] =
          WriterT(H.handleWith(fa.run)(f `andThen` (_.run)))
      }
  }
}

object instances extends Bases.ErrorsToInstances
