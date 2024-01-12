import sbtrelease.ReleaseStateTransformations._
import sbtreleasenotes.ReleaseNotesPlugin._
import sbtreleasenotes.ReleaseNotesTransformations._


lazy val root = (project in file("."))
  .settings(
    version := "0.0.1",
    scalaVersion := "2.13.12",
    releaseSettings
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
