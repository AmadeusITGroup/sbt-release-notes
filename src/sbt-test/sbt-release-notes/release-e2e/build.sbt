import sbtrelease.ReleaseStateTransformations._
import sbtreleasenotes.ReleaseNotesPlugin._
import sbtreleasenotes.ReleaseNotesTransformations._
import scala.sys.process.Process

lazy val root = (project in file("."))
  .settings(
    version := "0.0.1",
    scalaVersion := "2.13.12",
    releaseSettings,
    TaskKey[Unit]("checkMessage") := {
      val process = Process("git", Seq("log", "-1", "--pretty=%B"))
      val expected = "Update release notes for version 0.0.1"
      val out = (process !!)
      if (out.trim != expected) sys.error(s"unexpected commit message: $out")
      ()
    }
  )

val releaseSettings = Seq(
  releaseProcess := Seq[ReleaseStep](
    checkSnapshotDependencies,
    inquireVersions,
    setReleaseVersion,
    commitReleaseVersion,
    updateReleaseNotes
  ),
  releaseNotesFailIfNotUpdated := true
)
