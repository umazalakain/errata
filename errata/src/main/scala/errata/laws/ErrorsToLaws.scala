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

package errata.laws

import cats.{Applicative, Functor}
import errata._

trait ErrorsToLaws[F[_], G[_], E] extends RaiseLaws[F, E] with HandleToLaws[F, G, E] {
  implicit override def F: ErrorsTo[F, G, E]

  def raiseHandleWith[A](e: E, f: E => G[A]): IsEq[G[A]] =
    F.handleWith(F.raise(e))(f) <-> f(e)

  def raiseHandle[A](e: E, f: E => A)(implicit A: Applicative[G]): IsEq[G[A]] =
    F.handle(F.raise(e))(f) <-> A.pure(f(e))

  def raiseRestore[A](
      e: E
  )(implicit FF: Functor[F], AG: Applicative[G]): IsEq[G[Option[A]]] =
    F.restore[A](F.raise(e)) <-> AG.pure(None: Option[A])

  def raiseAttempt[A](
      e: E
  )(implicit FF: Functor[F], AG: Applicative[G]): IsEq[G[Either[E, A]]] =
    F.attempt[A](F.raise(e)) <-> AG.pure(Left[E, A](e))

  def fromEitherAttempt[A](
      either: Either[E, A]
  )(implicit AF: Applicative[F], AG: Applicative[G]): IsEq[G[Either[E, A]]] =
    F.attempt[A](F.fromEither(either)) <-> AG.pure(either)

  def fromOptionRestore[A](option: Option[A], e: E)(implicit
      AF: Applicative[F],
      AG: Applicative[G]
  ): IsEq[G[Option[A]]] =
    F.restore[A](F.fromOption(e)(option)) <-> AG.pure(option)
}

object ErrorsToLaws {
  def apply[F[_], G[_], E](implicit
      ev: ErrorsTo[F, G, E]
  ): ErrorsToLaws[F, G, E] =
    new ErrorsToLaws[F, G, E] { def F: ErrorsTo[F, G, E] = ev }
}
