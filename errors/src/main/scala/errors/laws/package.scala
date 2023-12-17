package errors

package object laws {
  final case class IsEq[A](lhs: A, rhs: A)

  implicit class IsEqSyntax[A](lhs: A) {
    def <->(rhs: A): IsEq[A] = IsEq(lhs, rhs)
  }
}
