val scala2Version = "2.13.12"
val scala3Version = "3.3.1"

lazy val commonSettings = Seq(
  libraryDependencies ++= {
    scalaVersion.value match {
      case `scala2Version` => List(
        compilerPlugin("org.typelevel" % "kind-projector" % "0.13.2" cross CrossVersion.full)
      )
      case _ => Nil
    }
  }
)

lazy val errors = project
  .in(file("errors"))
  .settings(
    commonSettings,
    name := "errors",
    version := "0.1.0",
    organization := "errors",

    libraryDependencies ++= List(
      "org.typelevel" %% "cats-core" % "2.10.0",
      "org.typelevel" %% "discipline-munit" % "1.0.9" % Test
    ),

    // To make the default compiler and REPL use Dotty
    scalaVersion := scala3Version,

    // To cross compile with Scala 3 and Scala 2
    crossScalaVersions := Seq(scala3Version, scala2Version)
  )

lazy val examples = project
  .in(file("examples"))
  .dependsOn(errors)
  .settings(
    commonSettings,
    scalaVersion := scala2Version,
    publish / skip := true,
    libraryDependencies ++= List(
    "org.typelevel" %% "cats-effect" % "3.5.2",
    )
  )

lazy val root = project
  .in(file("."))
  .settings(
    commonSettings,
    crossScalaVersions := Nil
  )
  .aggregate(errors, examples)