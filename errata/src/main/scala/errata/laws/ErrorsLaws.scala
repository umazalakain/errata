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

package errata.laws

import errata.*

trait ErrorsLaws[F[_], E]
    extends RaiseLaws[F, E]
    with HandleLaws[F, E]
    with ErrorsToLaws[F, F, E]
    with TransformToLaws[F, F, E, E] {
  implicit def F: Errors[F, E]
}

object ErrorsLaws {
  def apply[F[_], E](implicit ev: Errors[F, E]): ErrorsLaws[F, E] =
    new ErrorsLaws[F, E] { def F: Errors[F, E] = ev }
}
