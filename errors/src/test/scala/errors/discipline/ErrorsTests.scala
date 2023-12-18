package errors.discipline

import cats.{Eq, Monad}
import errors.{Errors, Raise}
import errors.laws.ErrorsLaws
import org.scalacheck.{Arbitrary, Prop}
import org.typelevel.discipline.Laws

trait ErrorsTests[F[_], E] extends RaiseTests[F, E] with HandleTests[F, E] with ErrorsToTests[F, F, E] with TransformToTests[F, F, E, E] {

  def laws: ErrorsLaws[F, E]

  def errors[A](implicit
    MonadF: Monad[F],
    EqFA: Eq[F[A]],
    EqFOptionA: Eq[F[Option[A]]],
    EqFEitherEA: Eq[F[Either[E, A]]],
    ArbitraryA: Arbitrary[A],
    ArbitraryE: Arbitrary[E],
    ArbitraryAToFA: Arbitrary[A => F[A]],
    ArbitraryEToFA: Arbitrary[E => F[A]],
    ArbitraryEToA: Arbitrary[E => A],
    ArbitraryE1ToE2: Arbitrary[E => E],
    RaiseFE1: Raise[F, E]
  ): RuleSet = {
    new RuleSet {
      override def name: String = "Errors"
      override def bases: Seq[(String, Laws#RuleSet)] = Nil
      override def parents: Seq[RuleSet] = Seq(errorsTo[A], transformTo[A])
      override def props: Seq[(String, Prop)] = Nil
    }
  }

}

object ErrorsTests {
  def apply[F[_], E](implicit ev: Errors[F, E]): ErrorsTests[F, E] =
    new ErrorsTests[F, E] { def laws: ErrorsLaws[F, E] = ErrorsLaws[F, E] }
}