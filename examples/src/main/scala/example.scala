import cats.syntax.all.*
import cats.{Applicative, ApplicativeError, ApplicativeThrow, Functor}
import errors.*
import errors.instances.*

object Examples {
  sealed trait AppError
  case object ClientError extends AppError

  // Raises errors of type AppError, does not handle errors
  def raiseAppError[F[_]](implicit F: Raise[F, AppError]): F[Unit] =
    F.raise(ClientError)

  // Handles all errors of type AppError, does not raise errors
  def handleAppError[F[_]: Functor, G[_]: Applicative](input: F[Unit])(implicit F: HandleTo[F, G, AppError]): G[Unit] =
    F.attempt(input).void

  // Combine the above two
  // Saises errors of type AppError
  // Handles all errors of type AppError
  def raiseAndHandleAppError[F[_]: Functor, G[_]: Applicative](implicit F: ErrorsTo[F, G, AppError]): G[Unit] =
    handleAppError[F, G](raiseAppError[F])

  // Given cats.ApplicativeError[F, AppError], derives ErrorTo[F, F, AppError]
  def catsIntegration0[F[_]](implicit F: ApplicativeError[F, AppError]): F[Unit] =
    raiseAndHandleAppError[F, F]

  // Given cats.ApplicativeThrow[F], derives ErrorTo[F, F, AppError]
  // It does this by wrapping AppError into a Throwable
  def catsIntegration1[F[_]: ApplicativeThrow]: F[Unit] =
    raiseAndHandleAppError[F, F]
}