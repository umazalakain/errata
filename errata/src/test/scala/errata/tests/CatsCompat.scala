package errata.tests

import cats.effect.IO
import cats.effect.unsafe.implicits.global
import errata.*
import errata.syntax.all.*
import errata.instances.handleThrowable
import errata.instances.raiseThrowable
import errata.instances.raiseHandleErrors
import errata.instances.errorByCatsError
import scala.reflect.ClassTag

class CatsCompat extends munit.FunSuite {
  sealed trait Error
  case object ClientError extends Error
  val value: IO[Unit] = Raise[IO, Error].raise[Unit](ClientError)

  test("Cats effect instances unwrap errors on handle") {
    assertEquals(
      value
        .map(_ => false)
        .handle[IO, Error](_ => true)
        .unsafeRunSync(),
      true
    )
  }

  test("Handling subtypes of the type of thrown errors results in a meaningful error") {
    intercept[UnexpectedClassTag[ClientError.type, Error]] {
      value
        .map(_ => false)
        .handle[IO, ClientError.type](_ => true)
        .unsafeRunSync()
    }
  }

  test("An Errors instance with a custom error class can be put together from cats and Raise and Handle wrappers") {
    summon[Errors[IO, Error]]
  }
}
