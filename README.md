# Error handling made precise

Because error handling belongs in the types.

## Installation

This project compiles to both Scala 3.x and Scala 2.13.
This project has not been publicly released yet. Stay tuned!

## Rationale

The [approach to error handling taken by cats](https://typelevel.org/cats/typeclasses/applicativemonaderror.html) suffers from several shortcomings.
Assume a method with signature `def method[F[_]: ApplicativeError[*, AppError], A](fa: F[A]): F[A]`.
It might be the case that `method` raises errors, hence why `ApplicativeError[F, AppError]` is necessary.
Or, it might not raise any errors, and rather handle them.
From the signature alone, we cannot deduce whether `method` raises errors, handles them, or does both.

By itself, error handling is imprecise too. `ApplicativeError.attempt` transforms an `F[A]` into an `F[Either[E, A]]`,
which has two error channels: `Either[E, *]` where errors are reported as `Left`; and `F` itself, which is still capable of raising errors.

Moreover, managing multiple error types is highly impractical:
using multiple implicits `ApplicativeError[F, E1]` and `ApplicativeError[F, E2]` results in ambiguous implicit resolution, since both extend `Applicative[F]`.
Indeed, one can extend `E1` and `E2` with `Throwable` and substitute both `ApplicativeError[F, E1]` and `ApplicativeError[F, E2]` with `ApplicativeThrow[F]` (`ApplicativeError[F, Throwable]`).
In this case we lose information about the types of errors we raise, and must suddenly deal with all errors of type `Throwable`.

> :warning: **As a consequence of all of the above, services based on error handling à la cats are brittle and unnecessarily prone to runtime crashes:
> it becomes impossible to track which modules raise what errors, and the compiler cannot ensure that errors are appropriately dealt with**.

## Solution

This project exposes the error handling capabilities provided by [ToFu](https://github.com/tofu-tf/tofu/).
As such, their code is at times shared verbatim.

We differentiate between programs that _raise_ errors, and programs that _handle_ them.
The type `Raise[F, E]` tells us that we know how to raise errors of type `E` inside an effect `F[_]`.
The type `HandleTo[F, G, E]` tells us that we know how to handle errors of type `E` inside an effect `F[_]`, and `G[_]` is the effect after errors have been handled.
That is, we know that `F` may raise errors of type `E`, but `G` is free of any such constraints: _all errors of type `E` must be handled_ before transforming the effect `F` into the effect `G`.
(One can however choose to lose precision and instantiate the output effect to be the input effect.)

```scala
trait Raise[F[_], E] {
  def raise[A](err: E): F[A]
}
trait HandleTo[F[_], G[_], E] {
  def handleWith[A](fa: F[A])(f: E => G[A]): G[A]
}
```

We provide further convenience methods and bundles of types:
- `Handle[F[_], E]`: equivalent to `HandleTo[F, F, E]`, plus convenience methods.
- `ErrorsTo[F[_], G[_], E]`: equivalent to `Raise[F, E]` plus `HandleTo[F, G, E]` --- often what you want.
- `Errors[F[_], E]`: equivalent to `ErrorsTo[F, F, E]` plus convenience methods.
- `TransformTo[F[_], G[_], E1, E2]`: equivalent to `HandleTo[F, G, E1]` plus `Raise[G, E2]`, plus convenience methods.

## Interoperability with cats

Full interoperability with cats and its `ApplicativeError` and `ApplicativeThrow` is provided in `errors.instances.*`.
Check out [the examples](examples/src/main/scala/).


## Testing

Errors uses [discipline](https://github.com/typelevel/discipline) for quickcheck-style testing of algebraic laws.
[The laws](/errors/src/main/scala/errors/laws/) are grouped in [discipline bundles](/errors/src/test/scala/errors/discipline/) and [tested against concrete types](/errors/src/test/scala/errors/tests/).
Given a custom concrete type and its corresponding error raising/handling instances, you can verify them as lawful by executing against it the existing discipline bundles.
To execute the tests simply run `sbt test`.

## Example

```scala
import cats.effect.{ExitCode, IO, IOApp}
import cats.effect.std.Console
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

object httpClient extends IOApp {
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
  case class RestAPIError(th: Throwable) extends AppError
  case class GraphQLError(th: Throwable) extends AppError

  // The http client produces effects of type F
  // TransformTo[F, G, Throwable, AppError] guarantees that:
  //   all errors of type Throwable in F are transformed into errors of type AppError in G
  // HandleTo[G, H, AppError] guarantees that:
  //   all errors of type AppError are handled and gone from H
  // The lack of an instance Raise[H, E] guarantees that:
  //   the resulting effect H raises no errors at all
  def appLogic[F[_], G[_]: Applicative, H[_]: Applicative: Console, A](httpClient: HttpClient[F])(implicit
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
      .handleWith {
        // Handle errors
        case RestAPIError(th) => Console[H].println(s"REST API error: ${th.getMessage}")
        case GraphQLError(th) => Console[H].println(s"GraphQL error: ${th.getMessage}")
      }
  }

  def run(args: List[String]): IO[ExitCode] = {
    // Fully cats compatible
    // Automatically derives instances of TransformTo[IO, IO, Throwable, HttpClientError] and HandleTo[IO, IO, AppError]
    import errors.instances.*
    IO.println("Expecting a properly handled error") *>
      appLogic[IO, IO, IO, Unit](HttpClient[IO]).as(ExitCode.Success)
  }
}
```