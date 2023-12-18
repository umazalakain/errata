val scala2Version = "2.13.12"
val scala3Version = "3.3.1"

lazy val commonSettings = Seq(
  libraryDependencies ++= {
    CrossVersion.partialVersion(scalaVersion.value) match {
      case Some((2, _)) => Seq(
        compilerPlugin("org.typelevel" % "kind-projector" % "0.13.2" cross CrossVersion.full)
      )
      case _ => Nil
    }
  },

  scalacOptions ++= {
    CrossVersion.partialVersion(scalaVersion.value) match {
      case Some((3, _)) => Seq("-Ykind-projector:underscores")
      case Some((2, 12 | 13)) => Seq("-Xsource:3", "-P:kind-projector:underscore-placeholders")
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
    scalaVersion := scala3Version,
    crossScalaVersions := Seq(scala3Version, scala2Version),
    publish / skip := true,
    libraryDependencies ++= List(
    "org.typelevel" %% "cats-effect" % "3.5.2",
    ),
    Compile / run / fork := true
  )

lazy val root = project
  .in(file("."))
  .settings(
    commonSettings,
    crossScalaVersions := Nil
  )
  .aggregate(errors, examples)