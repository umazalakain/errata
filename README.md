# Error handling made precise

## Rationale

The [approach](https://typelevel.org/cats/typeclasses/applicativemonaderror.html) to error handling taken by `cats` suffers from several shortcomings.
Assume a method with signature `def method[F[_]: ApplicativeError[*, AppError], A](fa: F[A]): F[A]`.
It might be the case that `method` throws errors, hence why `ApplicativeError[F, AppError]` is necessary.
Or, it might not throw any errors, and rather handle them.
From the signature alone, we cannot deduce whether `method` raises errors, handles them, or does both.

By itself, error handling is imprecise too. `ApplicativeError.attempt` transforms an `F[A]` into an `F[Either[E, A]]`,
which has two error channels: `Either[E, *]` where errors are reported as `Left`; and `F` itself, which is still capable of raising errors.

Moreover, managing multiple error types is highly impractical:
using multiple implicits `ApplicativeError[F, E1]` and `ApplicativeError[F, E2]` results in ambiguous implicit resolution, since both extend `Applicative[F]`.
Indeed, one can substitute them both with `ApplicativeThrow[F]` (`ApplicativeError[F, Throwable]`) and make both `E1` and `E2` extend `Throwable`.
In this case too we lose precision in error handling: we lose information about what types of errors we raise, and must deal with all errors of type `Throwable`.

## Solution

TODO

## Installation

TODO