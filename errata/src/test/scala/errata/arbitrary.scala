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

import cats.data.Func
import org.scalacheck.{Arbitrary, Cogen}

// Reproduced from https://github.com/typelevel/cats/blob/5c5fe56b98feca6da9f466ecfaaa356ce8ae69ad/laws/src/main/scala/cats/laws/discipline/arbitrary.scala
object arbitrary {
  implicit def catsLawsArbitraryForFunc[F[_], A, B](implicit
    AA: Arbitrary[A],
    CA: Cogen[A],
    F: Arbitrary[F[B]]
  ): Arbitrary[Func[F, A, B]] =
    Arbitrary(Arbitrary.arbitrary[A => F[B]].map(Func.func))
}
