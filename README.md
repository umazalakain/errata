# Error handling made precise

Because error handling should belong in the types.

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

As a consequence of all this, **services based on error handling à la cats are brittle and unnecessarily prone to runtime crashes**.
It becomes impossible to track which modules raise what errors, and the compiler cannot help us ensuring that errors are appropriately dealt with.

## Solution

This project exposes the error handling capabilities provided by [ToFu](https://github.com/tofu-tf/tofu/).
As such their code is at times shared verbatim.

TODO

## Installation

TODO