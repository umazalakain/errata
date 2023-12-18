import cats.effect.IO
import cats.syntax.all.*
import cats.{Applicative, MonadThrow}
import errors.*
import errors.syntax.*

/*
This example demonstrates the interoperability between this project and cats errors.
- application top-level uses cats errors
- http client uses cats errors
- application logic uses _errors_ only

 +-------------------+
 |      IO (cats)    |
 +-------------------+
 | https  |    app   |
 | client |   logic  |
 | (cats) | (errors) |
 +--------+----------+
 */

object httpClient {
  // Http4s client (raises cats errors with MonadThrow)
  trait HttpClient[F[_]] {
    def run[A]: F[A]
  }
  object HttpClient {
    def apply[F[_]](implicit F: MonadThrow[F]): HttpClient[F] = new HttpClient[F] {
      override def run[A]: F[A] = F.raiseError(new Throwable("Some kind of error"))
    }
  }

  // Application-wide custom error types
  sealed trait AppError
  case class HTTPClientError(th: Throwable) extends AppError
  case object OtherKindOfError extends AppError

  // The http client produces effects of type F
  // TransformTo[F, G, Throwable, HttpClientError] guarantees that:
  //   all errors of type Throwable in F are transformed into errors of type HttpClientError in G
  // HandleTo[G, H, AppError] guarantees that:
  //   all errors of type AppError are handled and gone from H
  // The lack of an instance Raise[H, E] guarantees that:
  //   the resulting effect H raises no errors at all
  def appLogic[F[_], G[_]: Applicative, H[_]: Applicative, A](httpClient: HttpClient[F])(implicit
    transformTo: TransformTo[F, G, Throwable, HTTPClientError],
    handleTo: HandleTo[G, H, AppError]
  ): H[Unit] = {
    httpClient
      .run
      .transform(HTTPClientError.apply)
      .map {
        // Handle happy case
        (_: A) => ()
      }
      .handleWith {
        // Handle errors
        case HTTPClientError(th) => ().pure[H]
        case OtherKindOfError => ().pure[H]
      }
  }

  def main[A]: IO[Unit] = {
    // Fully cats compatible
    // Automatically derives instances of TransformTo[IO, IO, Throwable, HttpClientError] and HandleTo[IO, IO, AppError]
    import errors.instances.*
    appLogic[IO, IO, IO, A](HttpClient[IO])
  }
}