package errors

import cats.{Applicative, ApplicativeError, ApplicativeThrow, Id, Monad}
import cats.data.{EitherT, OptionT, ReaderT}

import scala.reflect.ClassTag

object instances {

  final case class WrappedError[E](tag: ClassTag[E], value: E) extends Throwable
  final implicit def handleThrowable[F[_], E](implicit F: Handle[F, Throwable], etag: ClassTag[E]): Handle[F, E] =
    new Handle[F, E] {
      override def tryHandleWith[A](fa: F[A])(f: E => Option[F[A]]): F[A] =
        F.tryHandleWith(fa) {
          case WrappedError(tag, value) if tag == etag => f(value.asInstanceOf[E])
          case _ => None
        }
    }
  final implicit def raiseThrowable[F[_], E](implicit F: Raise[F, Throwable], etag: ClassTag[E]): Raise[F, E] =
    new Raise[F, E] {
      override def raise[A](err: E): F[A] = F.raise(WrappedError(etag, err))
    }
  final implicit def errorsThrowable[F[_], E](implicit F: Errors[F, Throwable], etag: ClassTag[E]): Errors[F, E] =
    new Errors[F, E] {
      override def raise[A](err: E): F[A] = F.raise(WrappedError(etag, err))
      override def tryHandleWith[A](fa: F[A])(f: E => Option[F[A]]): F[A] =
        F.tryHandleWith(fa) {
          case WrappedError(tag, value) if tag == etag => f(value.asInstanceOf[E])
          case _ => None
        }
    }

  // If using cats.ApplicativeError on the outer layer but want to use errors
  final implicit def errorByCatsError[F[_], E](implicit F: ApplicativeError[F, E]): Errors[F, E] =
    new Errors[F, E] {
      override def tryHandleWith[A](fa: F[A])(f: E => Option[F[A]]): F[A] =
        F.recoverWith(fa)(f.unlift)
      override def raise[A](err: E): F[A] =
        F.raiseError(err)
    }

  object fromCats {
    // If using errors on the outer layer but want to use cats.ApplicativeError (uncommon)
    final implicit def catsErrorByError[F[_], E](implicit FE: Errors[F, E], A: Applicative[F]): ApplicativeError[F, E] =
      new ApplicativeError[F, E] {
        override def raiseError[A](e: E): F[A] =
          FE.raise(e)
        override def handleErrorWith[A](fa: F[A])(f: E => F[A]): F[A] =
          FE.handleWith(fa)(f)
        override def ap[A, B](ff: F[A => B])(fa: F[A]): F[B] =
          A.ap(ff)(fa)
        override def pure[A](x: A): F[A] =
          A.pure(x)
      }
  }

  final implicit def readerTErrors[F[_], R, E](implicit F: Errors[F, E]): Errors[ReaderT[F, R, _], E] =
    new Errors[ReaderT[F, R, _], E] {
      def raise[A](err: E): ReaderT[F, R, A] =
        ReaderT.liftF(F.raise(err))

      def tryHandleWith[A](fa: ReaderT[F, R, A])(f: E => Option[ReaderT[F, R, A]]): ReaderT[F, R, A] =
        ReaderT(r => F.tryHandleWith(fa.run(r))(e => f(e).map(_.run(r))))

      def restore[A](fa: ReaderT[F, R, A])(implicit AF: Applicative[F]): ReaderT[F, R, Option[A]] =
        ReaderT(r => F.restore(fa.run(r)))

      def lift[A](fa: ReaderT[F, R, A]): ReaderT[F, R, A] = fa
    }

  final implicit def eitherTInstance[F[_], E](implicit F: Monad[F]): ErrorsTo[EitherT[F, E, _], F, E] =
    new ErrorsTo[EitherT[F, E, _], F, E] {
      override def raise[A](err: E): EitherT[F, E, A] =
        EitherT.leftT(err)
      override def handleWith[A](fa: EitherT[F, E, A])(f: E => F[A]): F[A] =
        fa.foldF[A](f, F.pure)
    }

  final implicit def optionTInstance[F[_]](implicit F: Monad[F]): ErrorsTo[OptionT[F, _], F, Unit] =
    new ErrorsTo[OptionT[F, _], F, Unit] {
      override def raise[A](err: Unit): OptionT[F, A] =
        OptionT.none
      override def handleWith[A](fa: OptionT[F, A])(f: Unit => F[A]): F[A] =
        fa.getOrElseF(f(()))
    }

  final implicit def eitherInstance[E]: ErrorsTo[Either[E, _], Id, E] =
    new ErrorsTo[Either[E, _], Id, E] {
      override def raise[A](err: E): Either[E, A] =
        Left(err)
      override def handleWith[A](fa: Either[E, A])(f: E => Id[A]): Id[A] =
        fa.fold(f, identity)
    }

  final implicit val optionInstance: ErrorsTo[Option, Id, Unit] =
    new ErrorsTo[Option, Id, Unit] {
      override def raise[A](err: Unit): Option[A] =
        None
      override def handleWith[A](fa: Option[A])(f: Unit => Id[A]): Id[A] =
        fa.getOrElse(f(()))
    }
}
