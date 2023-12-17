package errors.tests

import cats.Id
import errors.discipline.{HandleToTests, RaiseTests}
import errors.instances.optionInstance
import munit.DisciplineSuite

class OptionSuite extends DisciplineSuite {
  checkAll("Option[Int]", RaiseTests[Option, Unit].raise[Int])
  checkAll("Option[Int]", HandleToTests[Option, Id, Unit].raise[Int])

}
