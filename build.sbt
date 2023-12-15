val scala2Version = "2.13.12"
val scala3Version = "3.3.1"

lazy val errors = project
  .in(file("errors"))
  .settings(
    name := "errors",
    version := "0.1.0",
    organization := "errors",

    addCompilerPlugin("org.typelevel" % "kind-projector_2.13.12" % "0.13.2"),
    libraryDependencies ++= List(
      "org.typelevel" %% "cats-core" % "2.10.0",
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
    scalaVersion := scala2Version,
    publish / skip := true,
    libraryDependencies ++= List(
    "org.typelevel" %% "cats-effect" % "3.5.2",
    )
  )

lazy val root = project
  .in(file("."))
  .settings(
    crossScalaVersions := Nil
  )
  .aggregate(errors, examples)