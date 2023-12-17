package errors.tests

import cats.Id
import errors.discipline.{ErrorsToTests, HandleToTests, RaiseTests, TransformToTests}
import errors.instances.*
import munit.DisciplineSuite

class OptionSuite extends DisciplineSuite {
  checkAll("Option[Int]", RaiseTests[Option, Unit].raise[Int])
  checkAll("Option[Int]", HandleToTests[Option, Id, Unit].handleTo[Int])
  checkAll("Option[Int]", ErrorsToTests[Option, Id, Unit].errorsTo[Int])
  checkAll("Option[Int]", TransformToTests[Option, Option, Unit, Unit].transformTo[Int])
}
