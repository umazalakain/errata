package errors.tests

import cats.Id
import errors.TransformTo
import errors.discipline.{ErrorsToTests, HandleToTests, RaiseTests, TransformToTests}
import munit.DisciplineSuite

class OptionSuite extends DisciplineSuite {
  {
    import errors.instances.optionInstance
    checkAll("Option[Int]", RaiseTests[Option, Unit].raise[Int])
    checkAll("Option[Int]", HandleToTests[Option, Id, Unit].handleTo[Int])
    checkAll("Option[Int]", ErrorsToTests[Option, Id, Unit].errorsTo[Int])
  }

  {
    import errors.instances.optionInstance1
    checkAll("Option[Int]", TransformToTests[Option, Option, Unit, Unit].transformTo[Int])
  }
}
