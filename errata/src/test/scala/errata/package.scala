import cats.Eq
import errata.laws.IsEq
import org.scalacheck.Prop
import org.scalacheck.util.Pretty

package object errata {
  // Reproduced from https://github.com/typelevel/cats/blob/5c5fe56b98feca6da9f466ecfaaa356ce8ae69ad/kernel-laws/shared/src/main/scala/cats/kernel/laws/discipline/package.scala#
  implicit def catsLawsIsEqToProp[A](isEq: IsEq[A])(implicit ev: Eq[A], pp: A => Pretty): Prop =
    isEq match {
      case IsEq(x, y) =>
        if (ev.eqv(x, y)) Prop.proved
        else
          Prop.falsified :| {
            val exp = Pretty.pretty[A](y, Pretty.Params(0))
            val act = Pretty.pretty[A](x, Pretty.Params(0))
            s"Expected: $exp\n" + s"Received: $act"
          }
    }
}
