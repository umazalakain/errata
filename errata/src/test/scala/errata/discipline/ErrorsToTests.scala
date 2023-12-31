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

import cats.{Applicative, Eq, Monad}
import errata.catsLawsIsEqToProp
import errata.ErrorsTo
import errata.laws.ErrorsToLaws
import org.scalacheck.{Arbitrary, Prop}
import org.scalacheck.Prop.forAll
import org.typelevel.discipline.Laws

trait ErrorsToTests[F[_], G[_], E] extends RaiseTests[F, E] with HandleToTests[F, G, E] {

  def laws: ErrorsToLaws[F, G, E]

  def errorsTo[A](implicit
      MonadF: Monad[F],
      ApplicativeG: Applicative[G],
      EqFA: Eq[F[A]],
      EqGA: Eq[G[A]],
      EqGOptionA: Eq[G[Option[A]]],
      EqGEitherEA: Eq[G[Either[E, A]]],
      ArbitraryA: Arbitrary[A],
      ArbitraryE: Arbitrary[E],
      ArbitraryAToFA: Arbitrary[A => F[A]],
      ArbitraryEToGA: Arbitrary[E => G[A]],
      ArbitraryEToA: Arbitrary[E => A]
  ): RuleSet = {
    new RuleSet {
      override def name: String = "ErrorsTo"
      override def bases: Seq[(String, Laws#RuleSet)] = Nil
      override def parents: Seq[RuleSet] = Seq(raise[A], handleTo[A])
      override def props: Seq[(String, Prop)] = Seq(
        "handleWith f . raise e = f" -> forAll(laws.raiseHandleWith[A] _),
        "handle f . raise e = pure f" -> forAll(laws.raiseHandle[A] _),
        "restore . raise e = pure none" -> forAll(laws.raiseRestore[A] _),
        "attempt . raise e = pure left e" -> forAll(laws.raiseAttempt[A, E] _),
        "restore . fromOption e opt = pure opt" -> forAll(laws.fromEitherAttempt[A, E] _),
        "attempt . fromEither e eth = pure eth" -> forAll(laws.fromOptionRestore[A] _)
      )
    }
  }

}

object ErrorsToTests {
  def apply[F[_], G[_], E](implicit ev: ErrorsTo[F, G, E]): ErrorsToTests[F, G, E] =
    new ErrorsToTests[F, G, E] { def laws: ErrorsToLaws[F, G, E] = ErrorsToLaws[F, G, E] }
}
