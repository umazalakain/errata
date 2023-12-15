import cats.syntax.all._
import cats.{Applicative, ApplicativeError, ApplicativeThrow, Functor}
import errors._
import errors.instances._

sealed trait AppError
object AppError {
  case object ClientError extends AppError
  case object DecodingError extends AppError
}

object Examples {
  def possibleError[F[_]](implicit F: Raise[F, AppError]): F[Unit] =
    F.raise(AppError.ClientError)

  def capturesErrors[F[_]: Functor, G[_]: Applicative](implicit F: ErrorsTo[F, G, AppError]): G[Either[AppError, Unit]] =
    F.attempt(possibleError[F])

  def catsIntegration0[F[_]](implicit F: ApplicativeError[F, AppError]): F[Unit] =
    capturesErrors[F, F].void

  def catsIntegration1[F[_]: ApplicativeThrow]: F[Unit] =
    capturesErrors[F, F].void
}