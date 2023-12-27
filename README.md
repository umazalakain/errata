# Error handling made precise

Because error handling belongs in the types.

## Installation

This project compiles to Scala 3.x and Scala 2.13.

Add it as an sbt dependency:
```scala
libraryDependencies += "info.umazalakain" % "errata" %% "0.1.0"
```

## Rationale

The [approach to error handling taken by cats](https://typelevel.org/cats/typeclasses/applicativemonaderror.html) suffers from several shortcomings.
Assume a method with signature `def method[F[_]: ApplicativeError[*, AppError], A](fa: F[A]): F[A]`.
It might be the case that `method` raises errors, hence why `ApplicativeError[F, AppError]` is necessary.
Or, it might not raise any errors, and rather handle them.
From the signature alone, we cannot deduce whether `method` raises errors, handles them, or does both.

By itself, error handling is imprecise too.
`ApplicativeError.attempt` transforms an `F[A]` into an `F[Either[E, A]]`, which has two error channels: `Either[E, *]` where errors are reported as `Left`; and `F` itself, still capable of raising errors.
_The fact that errors were handled is not reflected in the effect types._

Moreover, managing multiple error types is highly impractical:
using multiple implicits `ApplicativeError[F, E1]` and `ApplicativeError[F, E2]` results in ambiguous implicit resolution, since both extend `Applicative[F]`.
Indeed, one can extend `E1` and `E2` with `Throwable` and substitute both `ApplicativeError[F, E1]` and `ApplicativeError[F, E2]` with `ApplicativeThrow[F]` (`ApplicativeError[F, Throwable]`).
In this case, we lose information about the types of errors we raise, and must now deal with all errors of type `Throwable`.

> :warning: **As a consequence of all of the above, services based on error handling à la cats are brittle and unnecessarily prone to runtime crashes:
> it becomes impossible to track which modules raise what errors, and the compiler cannot ensure that errors are appropriately dealt with**.

## Solution

This project exposes the error handling capabilities provided by [ToFu](https://github.com/tofu-tf/tofu/).
As such, their code is at times shared verbatim.

We differentiate between programs that _raise_ errors, and programs that _handle_ them.

### Raising errors

The type `Raise[F, E]` tells us that we know how to raise errors of type `E` inside an effect `F[_]`.
```scala
trait Raise[F[_], E] {
  def raise[A](err: E): F[A]
}
```

### Handling errors

The type `HandleTo[F, G, E]` tells us that we know how to transform an effect `F[_]` into an effect `G[_]` by handling all errors of type `E`.
```scala
trait HandleTo[F[_], G[_], E] {
  def handleWith[A](fa: F[A])(f: E => G[A]): G[A]
}
```
For any type `A`, we are able to transform _all_ values of type `F[A]` into values of type `G[A]` _merely_ by handling the error cases with `E => G[A]`.
That is, _all_ errors of type `E` are dealt with by the time we reach `G`.

### Bundles

We provide further convenience methods and bundles of types.
You may check them out in more detail [here](errata/src/main/scala/errata/types.scala).

- `Handle[F[_], E]`: equivalent to `HandleTo[F, F, E]`, plus convenience methods.
- `ErrorsTo[F[_], G[_], E]`: equivalent to `Raise[F, E]` plus `HandleTo[F, G, E]` --- often what you want.
- `Errors[F[_], E]`: equivalent to `ErrorsTo[F, F, E]` plus convenience methods.
- `TransformTo[F[_], G[_], E1, E2]`: equivalent to `HandleTo[F, G, E1]` plus `Raise[G, E2]`, plus convenience methods.

### Example instances

As an example, we derive instances `Raise` and `HandleTo` for the concrete type `Either[E, _]`.
We provide these instances by combining them into one single instance `ErrorsTo[Either[E, _], Id, E]`.
This shows that:
- for any value type `A` we can use `Either[E, A]` to represent errors; and that
- for any value type `A` we can transform values of type `Either[E, A]` into values of type `Id[A]` (aka `A`) by handling errors of type `E`.

```scala
final implicit def eitherInstance[E]: ErrorsTo[Either[E, _], Id, E] =
  new ErrorsTo[Either[E, _], Id, E] {
    override def raise[A](err: E): Either[E, A] =
      Left(err)
    override def handleWith[A](fa: Either[E, A])(f: E => Id[A]): Id[A] =
      fa.fold(f, identity)
  }
```

## Interoperability with cats

Full interoperability with cats and its `ApplicativeError` and `ApplicativeThrow` is provided in `errata.instances.*`.
Check out [the examples](examples/src/main/scala/).


## Algebraic laws and testing

All instances must satisfy certain [algebraic laws](/errata/src/main/scala/errata/laws/) to be considered well behaved.

We use [discipline](https://github.com/typelevel/discipline) to perform quickcheck-style checking of these laws.
The laws are grouped into [discipline bundles](/errata/src/test/scala/errata/discipline/) and [tested against concrete types](/errata/src/test/scala/errata/tests/).
Given a custom concrete type and its corresponding error raising/handling instances, you can verify they are lawful by running them on the existing discipline bundles.

To execute the tests simply run `sbt test`.

## Example

```scala
import cats.effect.{ExitCode, IO, IOApp}
import cats.effect.std.Console
import cats.syntax.all.*
import cats.{Applicative, MonadThrow}
import errata.*
import errata.syntax.*

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
    import errata.instances.*
    IO.println("Expecting a properly handled error") *>
      appLogic[IO, IO, IO, Unit](HttpClient[IO]).as(ExitCode.Success)
  }
}
```