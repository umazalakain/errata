package errors.tests

import cats.Id
import errors.TransformTo
import errors.discipline.{ErrorsToTests, HandleToTests, RaiseTests, TransformToTests}
import munit.DisciplineSuite

class EitherSuite extends DisciplineSuite {
  {
    import errors.instances.eitherInstance
    checkAll("Either[String,Int]", RaiseTests[Either[String, _], String].raise[Int])
    checkAll("Either[String,Int]", HandleToTests[Either[String, _], Id, String].handleTo[Int])
    checkAll("Either[String,Int]", ErrorsToTests[Either[String, _], Id, String].errorsTo[Int])
  }

  {
    import errors.instances.eitherInstance1
    checkAll("Either[String,Int]", TransformToTests[Either[String, _], Either[String, _], String, String].transformTo[Int])
  }
}
