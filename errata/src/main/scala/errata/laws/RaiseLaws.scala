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

import cats.syntax.all._
import cats.{Applicative, Monad}
import errata.*
import IsEq.IsEqSyntax

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
