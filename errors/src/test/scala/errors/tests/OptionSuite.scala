package errors.tests

import cats.Id
import errors.discipline.*
import errors.instances.*
import munit.DisciplineSuite

class OptionSuite extends DisciplineSuite {
  checkAll("Option[Int]", RaiseTests[Option, Unit].raise[Int])
  checkAll("Option[Int]", HandleToTests[Option, Id, Unit].handleTo[Int])
  checkAll("Option[Int]", ErrorsToTests[Option, Id, Unit].errorsTo[Int])
  checkAll("Option[Int]", TransformToTests[Option, Option, Unit, Unit].transformTo[Int])
  checkAll("Option[Int]", HandleTests[Option, Unit].handle[Int])
  checkAll("Option[Int]", ErrorsTests[Option, Unit].errors[Int])
}
