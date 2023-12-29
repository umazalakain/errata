package object errata {
  type RaiseThrow[F[_]] = Raise[F, Throwable]
  type HandleToThrow[F[_], G[_]] = HandleTo[F, G, Throwable]
  type HandleThrow[F[_]] = Handle[F, Throwable]
  type ErrorsToThrow[F[_], G[_]] = ErrorsTo[F, G, Throwable]
  type ErrorsThrow[F[_]] = Errors[F, Throwable]
}
