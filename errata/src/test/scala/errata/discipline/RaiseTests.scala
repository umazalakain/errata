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

package errata.discipline

import cats.{Eq, Monad}
import errata.catsLawsIsEqToProp
import errata.Raise
import errata.laws.RaiseLaws
import org.scalacheck.Arbitrary
import org.scalacheck.Prop.forAll
import org.typelevel.discipline.Laws

trait RaiseTests[F[_], E] extends Laws {

  def laws: RaiseLaws[F, E]

  def raise[A](implicit
      MonadF: Monad[F],
      EqFA: Eq[F[A]],
      ArbitraryA: Arbitrary[A],
      ArbitraryE: Arbitrary[E],
      ArbitraryFA: Arbitrary[A => F[A]]
  ) =
    new DefaultRuleSet(
      name = "Raise",
      parent = None,
      "flatMap . raise    = raise" -> forAll(laws.raiseFlatmap[A] _),
      "fromEither . left  = raise" -> forAll(laws.leftFromEither[A] _),
      "fromEither . right = pure" -> forAll(laws.rightFromEither[A] _),
      "fromOption . none  = raise" -> forAll(laws.noneFromOption[A] _),
      "fromOption . some  = pure" -> forAll(laws.someFromOption[A] _)
    )

}

object RaiseTests {
  def apply[F[_], E](implicit ev: Raise[F, E]): RaiseTests[F, E] =
    new RaiseTests[F, E] { def laws: RaiseLaws[F, E] = RaiseLaws[F, E] }
}
