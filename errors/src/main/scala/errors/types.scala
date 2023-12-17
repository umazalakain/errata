package errors

import cats.{Applicative, Functor}
import cats.syntax.all._

import scala.annotation.implicitNotFound

/** Allows to raise `E` inside type `F`.
 */
@implicitNotFound("""can't understand how to raise ${E} inside ${F}
provide an instance of Raise[${F}, ${E}] or cats.ApplicativeError[${F}, ${E}]""")
trait Raise[F[_], E] {
  def raise[A](err: E): F[A]

  def fromEither[A](x: Either[E, A])(implicit F: Applicative[F]): F[A] =
    x.fold(raise, F.pure)

  def fromOption[A](e: E)(x: Option[A])(implicit F: Applicative[F]): F[A] =
    x.fold(raise[A](e))(F.pure)
}

/** Allows to recover after an error of type ${E} in a ${F} transiting to a ${G} as a result. A `G` can either be the
 * same as a `F` or some "subconstructor" having less errors semantically.
 */
@implicitNotFound("""can't understand how to recover from ${E} in the type ${F} to the subtype ${G}
provide an instance of HandleTo[${F}, ${G}, ${E}] or cats.ApplicativeError[${F}, ${E}]""")
trait HandleTo[F[_], G[_], E] {
  def handleWith[A](fa: F[A])(f: E => G[A]): G[A]

  def handle[A](fa: F[A])(f: E => A)(implicit G: Applicative[G]): G[A] =
    handleWith(fa)(e => G.pure(f(e)))

  def restore[A](fa: F[A])(implicit F: Functor[F], G: Applicative[G]): G[Option[A]] =
    handle(F.map(fa)(_.some))(_ => None)

  def attempt[A](fa: F[A])(implicit F: Functor[F], G: Applicative[G]): G[Either[E, A]] =
    handle(F.map(fa)(_.asRight[E]))(_.asLeft)
}

/** Allows to recover after an error of type ${E} in a ${F}.
 */
@implicitNotFound("""can't understand how to recover from ${E} in the type ${F}
provide an instance of Handle[${F}, ${E}] or cats.ApplicativeError[${F}, ${E}]""")
trait Handle[F[_], E] extends HandleTo[F, F, E] {

  def tryHandleWith[A](fa: F[A])(f: E => Option[F[A]]): F[A]

  def handleWith[A](fa: F[A])(f: E => F[A]): F[A] =
    tryHandleWith(fa)(e => Some(f(e)))

  def tryHandle[A](fa: F[A])(f: E => Option[A])(implicit F: Applicative[F]): F[A] =
    tryHandleWith(fa)(e => f(e).map(F.pure))

  def recoverWith[A](fa: F[A])(pf: PartialFunction[E, F[A]]): F[A] =
    tryHandleWith(fa)(pf.lift)

  def recover[A](fa: F[A])(pf: PartialFunction[E, A])(implicit F: Applicative[F]): F[A] =
    tryHandle(fa)(pf.lift)

  def restoreWith[A](fa: F[A])(ra: => F[A]): F[A] =
    handleWith(fa)(_ => ra)
}

object Handle {
  trait ByRecover[F[_], E] extends Handle[F, E] {
    def recWith[A](fa: F[A])(pf: PartialFunction[E, F[A]]): F[A]

    override def tryHandleWith[A](fa: F[A])(f: E => Option[F[A]]): F[A] =
      recWith(fa)(f.unlift)
  }
}

/** Allows to throw and handle errors of type ${E} in a ${F} transiting to a ${G} when recovering. A `G` can either be
 * the same as `F` or some "subconstructor" having less errors semantically.
 */
@implicitNotFound("""can't understand how to deal with errors ${E} in the type ${F} with the subtype ${G}
provide an instance of ErrorsTo[${F}, ${G}, ${E}] or cats.ApplicativeError[${F}, ${E}]""")
trait ErrorsTo[F[_], G[_], E] extends Raise[F, E] with HandleTo[F, G, E]

/** Transforms errors of type ${E1} in ${F} into errors of type ${E2} in ${G}.
 * Allows to handle errors of type ${E1} in an ${F} transitioning to a ${G} when recovering.
 * Allows to raise errors of type ${E2} in a ${G}.
 */
@implicitNotFound("""can't understand how to transform errors ${E1} in ${F} into errors ${E2} in ${G}
provide an instance of TransformTo[${F}, ${G}, ${E1}, ${E2}] or cats.ApplicativeError[${F}, ${E1}] and cats.ApplicativeError[${G}, ${E2}]""")
trait TransformTo[F[_], G[_], E1, E2] extends HandleTo[F, G, E1] with Raise[G, E2] {
  def transform[A](fa: F[A])(f: E1 => E2): G[A] =
    handleWith(fa)(f `andThen` raise)
}


/** Allows to throw and handle errors of type ${E} in a ${F}.
 */
@implicitNotFound("""can't understand how to deal with errors ${E} in the type ${F}
provide an instance of Errors[${F}, ${E}] or cats.ApplicativeError[${F}, ${E}]""")
trait Errors[F[_], E] extends Raise[F, E] with Handle[F, E] with ErrorsTo[F, F, E] {
  def adaptError[A](fa: F[A])(pf: PartialFunction[E, E]): F[A] =
    recoverWith(fa)(pf.andThen(raise[A] _))
}