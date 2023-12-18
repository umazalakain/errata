package errata

import cats.{Applicative, Functor}


object syntax {
  implicit class HandleToSyntax[F[_], G[_], E, A](fa: F[A])(implicit F: HandleTo[F, G, E]) {
    def handleWith(f: E => G[A]): G[A] = F.handleWith(fa)(f)
    def handle(f: E => A)(implicit AG: Applicative[G]): G[A] = F.handle(fa)(f)
    def restore(implicit FF: Functor[F], AG: Applicative[G]): G[Option[A]] = F.restore(fa)
    def attempt(implicit FF: Functor[F], AG: Applicative[G]): G[Either[E, A]] = F.attempt(fa)
  }

  implicit class HandleSyntax[F[_], E, A](fa: F[A])(implicit F: Handle[F, E]) extends HandleToSyntax[F, F, E, A](fa) {
    def tryHandleWith(f: E => Option[F[A]]): F[A] = F.tryHandleWith(fa)(f)
    def tryHandle(f: E => Option[A])(implicit FF: Applicative[F]): F[A] = F.tryHandle(fa)(f)
    def recoverWith(pf: PartialFunction[E, F[A]]): F[A] = F.recoverWith(fa)(pf)
    def recover(pf: PartialFunction[E, A])(implicit AF: Applicative[F]): F[A] = F.recover(fa)(pf)
    def restoreWith(ra: => F[A]): F[A] = F.restoreWith(fa)(ra)
  }

  implicit class HandleByRecoverSyntax[F[_], E, A](fa: F[A])(implicit F: Handle.ByRecover[F, E]) extends HandleSyntax[F, E, A](fa) {
    def recWith(pf: PartialFunction[E, F[A]]): F[A] = F.recWith(fa)(pf)
  }

  implicit class TransformToSyntax[F[_], G[_], E1, E2, A](fa: F[A])(implicit F: TransformTo[F, G, E1, E2]) extends HandleToSyntax[F, G, E1, A](fa) {
    def transform(f: E1 => E2): G[A] = F.transform(fa)(f)
  }

  implicit class ErrorsSyntax[F[_], E, A](fa: F[A])(implicit F: Errors[F, E]) extends HandleSyntax[F, E, A](fa) {
    def transform(f: E => E): F[A] = F.transform(fa)(f)
  }
}