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

import cats.Eq
import errata.laws.IsEq
import org.scalacheck.Prop
import org.scalacheck.util.Pretty

package object errata {
  // Reproduced from https://github.com/typelevel/cats/blob/5c5fe56b98feca6da9f466ecfaaa356ce8ae69ad/kernel-laws/shared/src/main/scala/cats/kernel/laws/discipline/package.scala#
  implicit def catsLawsIsEqToProp[A](isEq: IsEq[A])(implicit ev: Eq[A], pp: A => Pretty): Prop =
    isEq match {
      case IsEq(x, y) =>
        if (ev.eqv(x, y)) Prop.proved
        else
          Prop.falsified :| {
            val exp = Pretty.pretty[A](y, Pretty.Params(0))
            val act = Pretty.pretty[A](x, Pretty.Params(0))
            s"Expected: $exp\n" + s"Received: $act"
          }
    }
}
