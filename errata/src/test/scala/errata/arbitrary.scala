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
