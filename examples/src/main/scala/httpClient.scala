/*
 * Copyright 2023 Uma Zalakain
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import cats.effect.{ExitCode, IO, IOApp}
import cats.effect.std.Console
import cats.syntax.all.*
import cats.{Applicative, MonadThrow}
import errata.*
import errata.syntax.all.*
import errata.instances.*

/*
This example demonstrates the interoperability between this project and cats errors.
- application top-level uses cats errors
- http client uses cats errors
- application logic uses _errata_ only

 +-------------------+
 |      IO (cats)    |
 +-------------------+
 | https  |    app   |
 | client |   logic  |
 | (cats) | (errata) |
 +--------+----------+
 */

object httpClient extends IOApp {
  // Http4s client (raises cats errors with MonadThrow)
  trait HttpClient[F[_]] {
    def run[A]: F[A]
  }
  object HttpClient {
    def apply[F[_]](implicit F: MonadThrow[F]): HttpClient[F] =
      new HttpClient[F] {
        override def run[A]: F[A] =
          F.raiseError(new Throwable("Some kind of error"))
      }
  }

  // Application-wide custom error types
  sealed trait AppError
  case class RestAPIError(th: Throwable) extends AppError
  case class GraphQLError(th: Throwable) extends AppError

  // The http client produces effects of type F
  // TransformTo[F, G, Throwable, AppError] guarantees that:
  //   all errors of type Throwable in F are transformed into errors of type AppError in G
  // HandleTo[G, H, AppError] guarantees that:
  //   all errors of type AppError are handled and gone from H
  // The lack of an instance Raise[H, E] guarantees that:
  //   the resulting effect H raises no errors at all
  def appLogic[F[_], G[_]: Applicative, H[_]: Applicative: Console, A](
      httpClient: HttpClient[F]
  )(implicit
      transformTo: TransformTo[F, G, Throwable, AppError],
      handleTo: HandleTo[G, H, AppError]
  ): H[Unit] = {
    val apiResponse: G[A] = httpClient.run[A].transform(RestAPIError.apply)
    val graphqlResponse: G[A] = httpClient.run[A].transform(GraphQLError.apply)
    (apiResponse, graphqlResponse)
      .mapN {
        // Handle happy case
        case (_, _) => ()
      }
      .handleWith[H, AppError] {
        // Handle errors
        case RestAPIError(th) =>
          Console[H].println(s"REST API error: ${th.getMessage}")
        case GraphQLError(th) =>
          Console[H].println(s"GraphQL error: ${th.getMessage}")
      }
  }

  def run(args: List[String]): IO[ExitCode] = {
    // Fully cats compatible
    // Automatically derives instances of TransformTo[IO, IO, Throwable, AppError] and HandleTo[IO, IO, AppError]
    implicit val appErrors: Errors[IO, AppError] = errorsThrowable(classTag[AppError])
    IO.println("Expecting a properly handled error") *>
      appLogic[IO, IO, IO, Unit](HttpClient[IO]).as(ExitCode.Success)
  }
}
