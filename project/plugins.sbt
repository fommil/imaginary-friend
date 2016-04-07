addSbtPlugin("org.scalariform" % "sbt-scalariform" % "1.5.1")

// really to test the gen-ensime-project code
scalacOptions ++= Seq("-unchecked", "-deprecation")

ivyLoggingLevel := UpdateLogging.Quiet

addSbtPlugin("com.github.alexarchambault" % "coursier-sbt-plugin" % "1.0.0-M10")
