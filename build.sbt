enablePlugins(SbtPlugin)

organization := "io.github.generoso"
homepage := Some(url("https://github.com/AmadeusITGroup/sbt-release-notes"))
licenses := List("Apache-2.0" -> url("http://www.apache.org/licenses/LICENSE-2.0"))
developers := List(
  Developer(
    "generoso",
    "Generoso Pagano",
    "generosojk@gmail.com",
    url("https://generoso.github.io/")
  )
)

name := "sbt-release-notes"
versionScheme := Some("semver-spec")
scalaVersion := "2.12.19"

scalacOptions := Seq(
  "-encoding",
  "UTF-8",
  "-deprecation",
  "-feature",
  "-unchecked",
  "-Xfuture",
  "-Ypartial-unification",
  "-target:jvm-1.8"
)

// sbt plugin dependencies
addSbtPlugin("com.github.sbt" % "sbt-release" % "1.4.0")

// scripted tests configuration
scriptedBufferLog := false
scriptedLaunchOpts ++= Seq("-Xmx1024M", "-server", "-Dplugin.version=" + version.value)

// coverage configuration
coverageFailOnMinimum := true
coverageMinimumStmtTotal := 100
coverageMinimumBranchTotal := 100
coverageExcludedPackages := ""

