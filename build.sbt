val scala2Version = "2.13.12"
val scala3Version = "3.3.1"

name := "errata"
description := "Error handling made precise. Because error handling belongs in the types."
ThisBuild / tlBaseVersion := "0.4"
ThisBuild / organization := "info.umazalakain"
ThisBuild / organizationName := "Uma Zalakain"
ThisBuild / startYear := Some(2023)
ThisBuild / licenses := Seq(License.Apache2)
ThisBuild / developers := List(tlGitHubDev("umazalakain", "Uma Zalakain"))
ThisBuild / tlFatalWarnings := false

lazy val commonSettings = Seq(
  scalacOptions ++= {
    CrossVersion.partialVersion(scalaVersion.value) match {
      case Some((3, _))       => Seq("-Ykind-projector:underscores")
      case Some((2, 12 | 13)) => Seq("-Xsource:3", "-P:kind-projector:underscore-placeholders")
      case _                  => Nil
    }
  }
)

lazy val root = tlCrossRootProject.aggregate(errata, examples)

lazy val errata = project
  .in(file("errata"))
  .settings(
    commonSettings,
    scalaVersion := scala3Version,
    crossScalaVersions := Seq(scala3Version, scala2Version),
    libraryDependencies ++= List(
      "org.typelevel" %% "cats-core" % "2.10.0",
      "org.typelevel" %% "discipline-munit" % "1.0.9" % Test
    )
  )

lazy val examples = project
  .in(file("examples"))
  .dependsOn(errata)
  .disablePlugins(MimaPlugin)
  .settings(
    commonSettings,
    scalaVersion := scala3Version,
    crossScalaVersions := Seq(scala3Version, scala2Version),
    publish / skip := true,
    libraryDependencies ++= List(
      "org.typelevel" %% "cats-effect" % "3.5.2"
    ),
    Compile / run / fork := true
  )
