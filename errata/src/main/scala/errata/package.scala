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

package object errata {
  type RaiseThrow[F[_]] = Raise[F, Throwable]
  type HandleToThrow[F[_], G[_]] = HandleTo[F, G, Throwable]
  type HandleThrow[F[_]] = Handle[F, Throwable]
  type ErrorsToThrow[F[_], G[_]] = ErrorsTo[F, G, Throwable]
  type ErrorsThrow[F[_]] = Errors[F, Throwable]
}
