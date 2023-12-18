package errors.laws

import errors.*

trait ErrorsLaws[F[_], E] extends RaiseLaws[F, E] with HandleLaws[F, E] with ErrorsToLaws[F, F, E] with TransformToLaws[F, F, E, E] {
  implicit def F: Errors[F, E]
}

object ErrorsLaws {
  def apply[F[_], E](implicit ev: Errors[F, E]): ErrorsLaws[F, E] =
    new ErrorsLaws[F, E] { def F: Errors[F, E] = ev }
}
