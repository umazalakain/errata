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
import cats.{Applicative, Functor, Id}

object syntax {
  object all extends syntax[Id] {
    override implicit def untag[A](fa: Id[A]): A = fa
  }

  object namespaced extends syntax[Wrapper] {
    override implicit def untag[A](fa: Wrapper[A]): A = fa.value

    implicit class Wrapped[A](value: A) {
      def withErrata: Wrapper[A] = Wrapper(value)
    }
  }

  final case class Wrapper[A](value: A)

  trait syntax[Tag[_]] {
    implicit def untag[A](fa: Tag[A]): A

    implicit class RaiseSyntax[E, A](err: Tag[E]) {
      def raise[F[_]](implicit F: Raise[F, E]): F[A] = F.raise(err)
    }

    implicit class EitherLiftSyntax[E, A](either: Tag[Either[E, A]]) {
      def liftTo[F[_]](implicit F: Raise[F, E], A: Applicative[F]): F[A] = F.fromEither(either)
    }

    implicit class ValidatedLiftSyntax[E, A](validated: Tag[Validated[E, A]]) {
      def liftTo[F[_]](implicit F: Raise[F, E], A: Applicative[F]): F[A] = F.fromValidated(validated)
    }

    implicit class OptionLiftSyntax[E, A](option: Tag[Option[A]]) {
      def liftTo[F[_]](err: E)(implicit F: Raise[F, E], A: Applicative[F]): F[A] = F.fromOption(err)(option)
    }

    implicit class OptionRaiseSyntax[E](option: Tag[Option[E]]) {
      def raiseTo[F[_]](implicit F: Raise[F, E], A: Applicative[F]): F[Unit] = option.fold(A.unit)(F.raise)
    }

    implicit class HandleToSyntax[F[_], A](fa: Tag[F[A]]) {
      def handleWith[G[_], E](f: E => G[A])(implicit F: HandleTo[F, G, E]): G[A] = F.handleWith(fa)(f)
      def handle[G[_], E](f: E => A)(implicit AG: Applicative[G], F: HandleTo[F, G, E]): G[A] = F.handle(fa)(f)
      def restore[G[_], E](implicit FF: Functor[F], AG: Applicative[G], F: HandleTo[F, G, E]): G[Option[A]] =
        F.restore(fa)
      def attempt[G[_], E](implicit FF: Functor[F], AG: Applicative[G], F: HandleTo[F, G, E]): G[Either[E, A]] =
        F.attempt(fa)
    }

    implicit class HandleSyntax[F[_], A](fa: Tag[F[A]]) {
      def tryHandleWith[E](f: E => Option[F[A]])(implicit F: Handle[F, E]): F[A] = F.tryHandleWith(fa)(f)
      def tryHandle[E](f: E => Option[A])(implicit FF: Applicative[F], F: Handle[F, E]): F[A] =
        F.tryHandle(fa)(f)
      def recoverWith[E](pf: PartialFunction[E, F[A]])(implicit F: Handle[F, E]): F[A] = F.recoverWith(fa)(pf)
      def recover[E](pf: PartialFunction[E, A])(implicit AF: Applicative[F], F: Handle[F, E]): F[A] =
        F.recover(fa)(pf)
      def restoreWith[E](ra: => F[A])(implicit F: Handle[F, E]): F[A] = F.restoreWith(fa)(ra)
    }

    implicit class HandleByRecoverSyntax[F[_], A](fa: Tag[F[A]]) {
      def recWith[E](pf: PartialFunction[E, F[A]])(implicit F: Handle.ByRecover[F, E]): F[A] = F.recWith(fa)(pf)
    }

    implicit class TransformToSyntax[F[_], A](fa: Tag[F[A]]) {
      def transform[G[_], E1, E2](f: E1 => E2)(implicit F: TransformTo[F, G, E1, E2]): G[A] = F.transform(fa)(f)
    }
  }
}
