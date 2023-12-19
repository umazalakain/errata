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

import cats.{Applicative, Eq}
import errata.{Handle, Raise, catsLawsIsEqToProp}
import errata.laws.HandleLaws
import org.scalacheck.Arbitrary
import org.scalacheck.Prop.forAll

trait HandleTests[F[_], E] extends HandleToTests[F, F, E] {

  def laws: HandleLaws[F, E]

  def handle[A](implicit
    ApplicativeF: Applicative[F],
    ArbitraryA: Arbitrary[A],
    EqFA: Eq[F[A]],
    RaiseFE: Raise[F, E],
    ArbitraryE: Arbitrary[E],
    ArbitraryFA: Arbitrary[F[A]],
    ArbitraryEToOptionFA: Arbitrary[E => Option[F[A]]],
    ArbitraryPartialEToFA: Arbitrary[PartialFunction[E, F[A]]],
    ArbitraryPartialEToA: Arbitrary[PartialFunction[E, A]],
    ArbitraryEToOptionA: Arbitrary[E => Option[A]],
    EqGOptionA: Eq[F[Option[A]]],
    EqGEitherEA: Eq[F[Either[E, A]]],
    ArbitraryEToGA: Arbitrary[E => F[A]],
    ArbitraryEToA: Arbitrary[E => A]
  ) =
    new DefaultRuleSet(
      name = "Handle",
      parent = Some(handleTo[A]),
      "tryHandleWith f (pure a) = pure a" -> forAll(laws.pureTryHandleWith[A] _),
      "tryHandleWith f (raise e) = getOrElse (f e) (raise e)" -> forAll(laws.raiseTryHandleWith[A] _),
      "tryHandle f (pure a) = pure a" -> forAll(laws.pureTryHandle[A] _),
      "tryHandle f (raise e) = fold (f e) (raise e) pure" -> forAll(laws.raiseTryHandle[A] _),
      "recoverWith f (pure a) = pure a" -> forAll(laws.pureRecoverWith[A] _),
      "recoverWith f (raise e) = getOrElse (f e) (raise e)" -> forAll(laws.raiseRecoverWith[A] _),
      "recover f (pure a) = pure a" -> forAll(laws.pureRecover[A] _),
      "recover f (raise e) = fold (f e) (raise e) pure" -> forAll(laws.raiseRecover[A] _),
      "restoreWith fa (pure a) = pure a" -> forAll(laws.pureRestoreWith[A] _),
      "restoreWith fa (raise e) = fa" -> forAll(laws.raiseRestoreWith[A] _),
    )

}

object HandleTests {
  def apply[F[_], E](implicit ev: Handle[F, E]): HandleTests[F, E] =
    new HandleTests[F, E] { def laws: HandleLaws[F, E] = HandleLaws[F, E] }
}