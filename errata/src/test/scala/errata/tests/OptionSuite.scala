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

class OptionSuite extends DisciplineSuite {
  checkAll("Option[Int]", RaiseTests[Option, Unit].raise[Int])
  checkAll("Option[Int]", HandleToTests[Option, Id, Unit].handleTo[Int])
  checkAll("Option[Int]", ErrorsToTests[Option, Id, Unit].errorsTo[Int])
  checkAll("Option[Int]", TransformToTests[Option, Option, Unit, Unit].transformTo[Int])
  checkAll("Option[Int]", HandleTests[Option, Unit].handle[Int])
  checkAll("Option[Int]", ErrorsTests[Option, Unit].errors[Int])
}
