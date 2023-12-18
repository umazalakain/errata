package errata.discipline

import cats.{Applicative, Eq}
import errata.catsLawsIsEqToProp
import errata.HandleTo
import errata.laws.HandleToLaws
import org.scalacheck.Arbitrary
import org.scalacheck.Prop.forAll
import org.typelevel.discipline.Laws

trait HandleToTests[F[_], G[_], E] extends Laws {

  def laws: HandleToLaws[F, G, E]

  def handleTo[A](implicit
    ApplicativeF: Applicative[F],
    ApplicativeG: Applicative[G],
    EqGA: Eq[G[A]],
    EqGOptionA: Eq[G[Option[A]]],
    EqGEitherEA: Eq[G[Either[E, A]]],
    ArbitraryA: Arbitrary[A],
    ArbitraryEToGA: Arbitrary[E => G[A]],
    ArbitraryEToA: Arbitrary[E => A]
  ) =
    new DefaultRuleSet(
      name = "HandleTo",
      parent = None,
      "handleWith . pure = pure" -> forAll(laws.pureHandleWith[A] _),
      "handle . pure = pure" -> forAll(laws.pureHandle[A] _),
      "restore . pure = pure . some" -> forAll(laws.pureRestore[A] _),
      "attempt . pure = pure . right" -> forAll(laws.pureAttempt[A] _),
    )

}

object HandleToTests {
  def apply[F[_], G[_], E](implicit ev: HandleTo[F, G, E]): HandleToTests[F, G, E] =
    new HandleToTests[F, G, E] { def laws: HandleToLaws[F, G, E] = HandleToLaws[F, G, E] }
}