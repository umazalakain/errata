/*
 * Copyright 2023 Uma Zalakain
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package errata

import cats.data.Validated
import cats.{Applicative, Functor}
import cats.syntax.all.*

import scala.annotation.implicitNotFound

/** Allows to raise `E` inside type `F`.
  */
@implicitNotFound(
  """can't understand how to raise ${E} inside ${F}
provide an instance of Raise[${F}, ${E}] or cats.ApplicativeError[${F}, ${E}]"""
)
trait Raise[F[_], -E] {
  def raise[A](err: E): F[A]

  def fromEither[A](x: Either[E, A])(implicit F: Applicative[F]): F[A] =
    x.fold(raise, F.pure)

  def fromValidated[A](x: Validated[E, A])(implicit F: Applicative[F]): F[A] =
    x.fold(raise, F.pure)

  def fromOption[A](e: E)(x: Option[A])(implicit F: Applicative[F]): F[A] =
    x.fold(raise[A](e))(F.pure)
}

object Raise {
  def apply[F[_], E](implicit ev: Raise[F, E]): Raise[F, E] = ev
}

/** Allows to recover after an error of type ${E} in a ${F} transiting to a ${G} as a result. A `G` can either be the
  * same as a `F` or some "subconstructor" having less errors semantically.
  */
@implicitNotFound(
  """can't understand how to recover from ${E} in the type ${F} to the subtype ${G}
provide an instance of HandleTo[${F}, ${G}, ${E}] or cats.ApplicativeError[${F}, ${E}]"""
)
trait HandleTo[F[_], G[_], +E] {
  def handleWith[A](fa: F[A])(f: E => G[A]): G[A]

  def handle[A](fa: F[A])(f: E => A)(implicit G: Applicative[G]): G[A] =
    handleWith(fa)(e => G.pure(f(e)))

  def restore[A](
      fa: F[A]
  )(implicit F: Functor[F], G: Applicative[G]): G[Option[A]] =
    handle(F.map(fa)(_.some))(_ => None)

  def attempt[A, EE >: E](
      fa: F[A]
  )(implicit F: Functor[F], G: Applicative[G]): G[Either[EE, A]] =
    handle(F.map(fa)(_.asRight[EE]))(_.asLeft)
}

object HandleTo {
  def apply[F[_], G[_], E](implicit ev: HandleTo[F, G[_], E]): HandleTo[F, G[_], E] = ev
}

/** Allows to recover after an error of type ${E} in a ${F}.
  */
@implicitNotFound(
  """can't understand how to recover from ${E} in the type ${F}
provide an instance of Handle[${F}, ${E}] or cats.ApplicativeError[${F}, ${E}]"""
)
trait Handle[F[_], +E] extends HandleTo[F, F, E] {

  def tryHandleWith[A](fa: F[A])(f: E => Option[F[A]]): F[A]

  override def handleWith[A](fa: F[A])(f: E => F[A]): F[A] =
    tryHandleWith(fa)(e => Some(f(e)))

  def tryHandle[A](fa: F[A])(f: E => Option[A])(implicit
      F: Applicative[F]
  ): F[A] =
    tryHandleWith(fa)(e => f(e).map(F.pure))

  def recoverWith[A](fa: F[A])(pf: PartialFunction[E, F[A]]): F[A] =
    tryHandleWith(fa)(pf.lift)

  def recover[A](fa: F[A])(pf: PartialFunction[E, A])(implicit
      F: Applicative[F]
  ): F[A] =
    tryHandle(fa)(pf.lift)

  def restoreWith[A](fa: F[A])(ra: => F[A]): F[A] =
    handleWith(fa)(_ => ra)
}

object Handle {
  def apply[F[_], E](implicit ev: Handle[F, E]): Handle[F, E] = ev

  trait ByRecover[F[_], E] extends Handle[F, E] {
    def recWith[A](fa: F[A])(pf: PartialFunction[E, F[A]]): F[A]

    override def tryHandleWith[A](fa: F[A])(f: E => Option[F[A]]): F[A] =
      recWith(fa)(f.unlift)
  }
}

/** Allows to throw and handle errors of type ${E} in a ${F} transiting to a ${G} when recovering. A `G` can either be
  * the same as `F` or some "subconstructor" having less errors semantically.
  */
@implicitNotFound(
  """can't understand how to deal with errors ${E} in the type ${F} with the subtype ${G}
provide an instance of ErrorsTo[${F}, ${G}, ${E}] or cats.ApplicativeError[${F}, ${E}]"""
)
trait ErrorsTo[F[_], G[_], E] extends Raise[F, E] with HandleTo[F, G, E]

object ErrorsTo {
  def apply[F[_], G[_], E](implicit ev: ErrorsTo[F, G, E]): ErrorsTo[F, G, E] = ev
}

/** Transforms errors of type ${E1} in ${F} into errors of type ${E2} in ${G}. Allows to handle errors of type ${E1} in
  * an ${F} transitioning to a ${G} when recovering. Allows to raise errors of type ${E2} in a ${G}.
  */
@implicitNotFound(
  """can't understand how to transform errors ${E1} in ${F} into errors ${E2} in ${G}
provide an instance of TransformTo[${F}, ${G}, ${E1}, ${E2}] or cats.ApplicativeError[${F}, ${E1}] and cats.ApplicativeError[${G}, ${E2}]"""
)
trait TransformTo[F[_], G[_], +E1, -E2] extends HandleTo[F, G, E1] with Raise[G, E2] {
  def transform[A](fa: F[A])(f: E1 => E2): G[A] =
    handleWith(fa)(f `andThen` raise)
}

object TransformTo {
  def apply[F[_], G[_], E1, E2](implicit ev: TransformTo[F, G, E1, E2]): TransformTo[F, G, E1, E2] = ev
}

/** Allows to throw and handle errors of type ${E} in a ${F}.
  */
@implicitNotFound(
  """can't understand how to deal with errors ${E} in the type ${F}
provide an instance of Errors[${F}, ${E}] or cats.ApplicativeError[${F}, ${E}]"""
)
trait Errors[F[_], E] extends Raise[F, E] with Handle[F, E] with ErrorsTo[F, F, E] with TransformTo[F, F, E, E]

object Errors {
  def apply[F[_], E](implicit ev: Errors[F, E]): Errors[F, E] = ev
}
