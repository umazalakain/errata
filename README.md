# Error handling made precise

## Rationale

The [approach](https://typelevel.org/cats/typeclasses/applicativemonaderror.html) to error handling taken by `cats` suffers from several shortcomings.
Assume a method with signature `def method[F[_]: ApplicativeError[*, AppError], A](fa: F[A]): F[A]`.
It might be the case that `method` throws errors, hence why `ApplicativeError[F, AppError]` is necessary.
Or, it might not throw any errors, but rather handle them.
From the signature alone, we cannot deduce whether `method` raises errors, handles them, or does both.

Error handling by itself is imprecise too. `ApplicativeError.attempt` transforms an `F[A]` into an `F[Either[E, A]]`,
which has two error channels: `Either[E, *]` where errors are reported as `Either.Left`; and the `F` itself, which is still capable of raising errors.

Moreover, managing multiple error types is highly impractical.
Using two implicits `ApplicativeError[F, E1]` and `ApplicativeError[F, E2]` results in ambiguous implicit resolution,
since both implicits extend `Applicative[F]`.
Indeed, one can substitute both with `ApplicativeThrow[F]` -- desugars to `ApplicativeError[F, Throwable]` -- and make both `E1` and `E2` extend `Throwable`.
However, in this case too we lose precision in error handling: we must deal with all errors of type `Throwable`, not just `E1` and `E2`.

## Solution

TODO

## Installation

TODO