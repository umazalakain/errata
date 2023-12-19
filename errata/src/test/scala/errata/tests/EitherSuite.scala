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

import cats.Id
import errata.discipline.*
import errata.instances.*
import munit.DisciplineSuite

class EitherSuite extends DisciplineSuite {
  checkAll("Either[String,Int]", RaiseTests[Either[String, _], String].raise[Int])
  checkAll("Either[String,Int]", HandleToTests[Either[String, _], Id, String].handleTo[Int])
  checkAll("Either[String,Int]", ErrorsToTests[Either[String, _], Id, String].errorsTo[Int])
  checkAll("Either[String,Int]", TransformToTests[Either[String, _], Either[String, _], String, String].transformTo[Int])
  checkAll("Either[String,Int]", HandleTests[Either[String, _], String].handle[Int])
  checkAll("Either[String,Int]", ErrorsTests[Either[String, _], String].errors[Int])
}
