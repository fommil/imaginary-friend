// Copyright 2016 Sam Halliday
// Licence: http://www.apache.org/licenses/LICENSE-2.0
import sbt._
import Keys._
import SonatypeSupport._
import com.typesafe.sbt.SbtScalariform.ScalariformKeys
import scalariform.formatter.preferences._

object ImaginaryFriendBuild extends Build {

  lazy override val settings = super.settings ++ Seq(
    scalaVersion := "2.10.6",
    organization := "com.fommil",
    name := "imaginary-friend",
    version := "1.0.0-SNAPSHOT"
  )

  def p(name: String): Project = Project(name, file(name)).settings(sharedSettings)

  lazy val sharedSettings = Sensible.settings ++ sonatype("fommil", "imaginary-friend", Apache2) ++ Seq(
    ScalariformKeys.preferences := FormattingPreferences().setPreference(AlignSingleLineCaseStatements, true),
    updateOptions := updateOptions.value.withCachedResolution(true),
    libraryDependencies ++= Sensible.testLibs() ++ Seq(
      "org.scala-lang" % "scala-compiler" % scalaVersion.value
    )
  )

  lazy val pctest = p("pctest").settings(
    libraryDependencies ++= Seq(
      "org.scala-lang" % "scala-compiler" % scalaVersion.value,
      "org.typelevel" %% "macro-compat" % "1.1.0"
    )
  )

  lazy val blackbox = p("blackbox").settings(
  )

  lazy val whitebox = p("whitebox").settings(
    libraryDependencies ++= Seq(
      "org.typelevel" %% "macro-compat" % "1.1.0",
      Sensible.macroParadise
    )
  )

  lazy val plugin = p("plugin").settings(
    scalacOptions in Test <++= (packageBin in Compile) map { jar =>
      // needs timestamp to force recompile
      Seq("-Xplugin:" + jar.getAbsolutePath, "-Jdummy=" + jar.lastModified)
    }
  )

  lazy val root = Project("root", file(".")).settings(sharedSettings).aggregate(
    pctest, blackbox, whitebox, plugin
  )

}
