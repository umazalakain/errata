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

package errata.tests

import cats.effect.IO
import cats.effect.unsafe.implicits.global
import errata.*
import errata.syntax.all.*
import errata.instances.*

class CatsCompat extends munit.FunSuite {
  sealed trait Error
  case object ClientError extends Error

  implicit val errors: Errors[IO, Error] = errorsThrowable(classTag[Error])
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
    implicit val errors: Errors[IO, ClientError.type] = errorsThrowable(classTag[ClientError.type])
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
