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

trait TransformToLaws[F[_], G[_], E1, E2] extends HandleToLaws[F, G, E1] with RaiseLaws[G, E2] {
  implicit override def F: TransformTo[F, G, E1, E2]

  def pureTransform[A](a: A, f: E1 => E2)(implicit AF: Applicative[F], AG: Applicative[G]): IsEq[G[A]] =
    F.transform(AF.pure(a))(f) <-> AG.pure(a)

  def raiseTransform[A](e1: E1, f: E1 => E2)(implicit R: Raise[F, E1]): IsEq[G[A]] =
    F.transform(R.raise[A](e1))(f) <-> F.raise(f(e1))
}

object TransformToLaws {
  def apply[F[_], G[_], E1, E2](implicit ev: TransformTo[F, G, E1, E2]): TransformToLaws[F, G, E1, E2] =
    new TransformToLaws[F, G, E1, E2] { def F: TransformTo[F, G, E1, E2] = ev }
}
