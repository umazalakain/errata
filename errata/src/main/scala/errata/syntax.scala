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

import cats.data.Validated
import cats.{Applicative, Functor}

object syntax {
  implicit class RaiseSyntax[F[_], E2, E1 <: E2](err: E1)(implicit F: Raise[F, E2]) {
    def raise[A]: F[A] = F.raise(err)
  }

  implicit class EitherLiftSyntax[F[_], E2, E1 <: E2, A](either: Either[E1, A])(implicit F: Raise[F, E2]) {
    def liftTo(implicit A: Applicative[F]): F[A] = F.fromEither(either)
  }

  implicit class ValidatedLiftSyntax[F[_], E2, E1 <: E2, A](validated: Validated[E1, A])(implicit F: Raise[F, E2]) {
    def liftTo(implicit A: Applicative[F]): F[A] = F.fromValidated(validated)
  }

  implicit class OptionLiftSyntax[F[_], E, A](option: Option[A])(implicit F: Raise[F, E]) {
    def liftTo(err: E)(implicit A: Applicative[F]): F[A] = F.fromOption(err)(option)
  }

  implicit class OptionRaiseSyntax[F[_], E2, E1 <: E2](option: Option[E1])(implicit F: Raise[F, E2]) {
    def raiseTo(implicit A: Applicative[F]): F[Unit] = option.fold(A.unit)(F.raise)
  }

  implicit class HandleToSyntax[F[_], G[_], E, A](fa: F[A])(implicit F: HandleTo[F, G, E]) {
    def handleWith(f: E => G[A]): G[A] = F.handleWith(fa)(f)
    def handle(f: E => A)(implicit AG: Applicative[G]): G[A] = F.handle(fa)(f)
    def restore(implicit FF: Functor[F], AG: Applicative[G]): G[Option[A]] =
      F.restore(fa)
    def attempt(implicit FF: Functor[F], AG: Applicative[G]): G[Either[E, A]] =
      F.attempt(fa)
  }

  implicit class HandleSyntax[F[_], E, A](fa: F[A])(implicit F: Handle[F, E]) {
    def tryHandleWith(f: E => Option[F[A]]): F[A] = F.tryHandleWith(fa)(f)
    def tryHandle(f: E => Option[A])(implicit FF: Applicative[F]): F[A] =
      F.tryHandle(fa)(f)
    def recoverWith(pf: PartialFunction[E, F[A]]): F[A] = F.recoverWith(fa)(pf)
    def recover(pf: PartialFunction[E, A])(implicit AF: Applicative[F]): F[A] =
      F.recover(fa)(pf)
    def restoreWith(ra: => F[A]): F[A] = F.restoreWith(fa)(ra)
  }

  implicit class HandleByRecoverSyntax[F[_], E, A](fa: F[A])(implicit F: Handle.ByRecover[F, E]) {
    def recWith(pf: PartialFunction[E, F[A]]): F[A] = F.recWith(fa)(pf)
  }

  implicit class TransformToSyntax[F[_], G[_], E1, E2, A](fa: F[A])(implicit F: TransformTo[F, G, E1, E2]) {
    def transform(f: E1 => E2): G[A] = F.transform(fa)(f)
  }

  implicit class ErrorsSyntax[F[_], E, A](fa: F[A])(implicit F: Errors[F, E]) {
    def transform(f: E => E): F[A] = F.transform(fa)(f)
  }
}
