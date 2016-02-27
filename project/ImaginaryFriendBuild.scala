// Copyright 2016 Sam Halliday
// Licence: http://www.apache.org/licenses/LICENSE-2.0
import sbt._
import Keys._
import SonatypeSupport._

object ImaginaryFriendBuild extends Build {

  lazy override val settings = super.settings ++ Seq(
    scalaVersion := "2.11.7",
    organization := "com.fommil",
    name := "imaginary-friend",
    version := "1.0.0-SNAPSHOT"
  )

  def p(name: String): Project = Project(name, file(name)).settings(
    Sensible.settings ++ sonatype("fommil", "imaginary-friend", Apache2)
  ).settings(
    updateOptions := updateOptions.value.withCachedResolution(true),
    libraryDependencies ++= Sensible.testLibs() ++ Seq(
      "org.scala-lang" % "scala-compiler" % scalaVersion.value
    )
  )

  lazy val pctest = p("pctest").settings(
    libraryDependencies ++= Seq(
      "org.scala-lang" % "scala-compiler" % scalaVersion.value,
      "org.typelevel" %% "macro-compat" % "1.1.0"
    ) ++ Sensible.macroParadise
  )

  lazy val blackbox = p("blackbox").settings(
  )

  lazy val whitebox = p("whitebox").settings(
    libraryDependencies ++= Seq(
      "org.typelevel" %% "macro-compat" % "1.1.0"
    ) ++ Sensible.macroParadise
  )

  lazy val plugin = p("plugin").settings(
    scalacOptions in Test <++= (packageBin in Compile) map { jar =>
      // needs timestamp to force recompile
      Seq("-Xplugin:" + jar.getAbsolutePath, "-Jdummy=" + jar.lastModified)
    }
  )

}
