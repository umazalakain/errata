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
