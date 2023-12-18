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
      "fromOption . some  = pure" -> forAll(laws.someFromOption[A] _),
    )

}

object RaiseTests {
  def apply[F[_], E](implicit ev: Raise[F, E]): RaiseTests[F, E] =
    new RaiseTests[F, E] { def laws: RaiseLaws[F, E] = RaiseLaws[F, E] }
}