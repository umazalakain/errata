# Error handling made precise

Because error handling belongs in the types.

## Installation

This project has not been publicly released yet. Stay tuned!

## Rationale

The [approach to error handling taken by cats](https://typelevel.org/cats/typeclasses/applicativemonaderror.html) suffers from several shortcomings.
Assume a method with signature `def method[F[_]: ApplicativeError[*, AppError], A](fa: F[A]): F[A]`.
It might be the case that `method` throws errors, hence why `ApplicativeError[F, AppError]` is necessary.
Or, it might not throw any errors, and rather handle them.
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
The type `Raise[F, E]` tells us that we know how to errors of type `E` inside an effect `F[_]`.
The type `HandleTo[F, G, E]` tells us that we know how to handle errors of type `E` inside an effect `F[_]`, where `G[_]` is the effect after errors have been handled.
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
- `Handle[F[_], E]`: equivalent to `Handle[F, F, E]`, plus convenience methods.
- `ErrorsTo[F[_], G[_], E]`: equivalent to `Raise[F, E]` plus `HandleTo[F, G, E]` --- this is usually what you want.
- `Errors[F[_], E]`: equivalent to `ErrorsTo[F, F, E]` plus convenience methods.

## Interoperability with cats

Full interoperability with cats and its `ApplicativeError` and `ApplicativeThrow` is provided in `errors.instances`.

```scala
import cats.syntax.all._
import cats.{Applicative, ApplicativeError, ApplicativeThrow, Functor}
import errors._
import errors.instances._

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
```

Check out [more examples](examples/src/main/scala/example.scala).
