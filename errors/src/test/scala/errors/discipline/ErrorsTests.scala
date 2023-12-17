package errors.discipline

import cats.{Applicative, Eq}
import errors.{Errors, Raise, catsLawsIsEqToProp}
import errors.laws.ErrorsLaws
import org.scalacheck.Arbitrary
import org.scalacheck.Prop.forAll

trait ErrorsTests[F[_], E] extends RaiseTests[F, E] with HandleTests[F, E] with ErrorsToTests[F, F, E] with TransformToTests[F, F, E, E] {

  def laws: ErrorsLaws[F, E]

  def errors[A](implicit
    ApplicativeF: Applicative[F],
    ArbitraryA: Arbitrary[A],
    EqFA: Eq[F[A]],
    ArbitraryE: Arbitrary[E],
    ArbitraryPartialEToE: Arbitrary[PartialFunction[E, E]],
  ) =
    new DefaultRuleSet(
      name = "Errors",
      parent = None,
      "adaptError f (pure a) = pure a" -> forAll(laws.pureAdaptError[A] _),
      "adaptError f (raise e) = raise (f e)" -> forAll(laws.raiseAdaptError[A] _),
    )

}

object ErrorsTests {
  def apply[F[_], E](implicit ev: Errors[F, E]): ErrorsTests[F, E] =
    new ErrorsTests[F, E] { def laws: ErrorsLaws[F, E] = ErrorsLaws[F, E] }
}