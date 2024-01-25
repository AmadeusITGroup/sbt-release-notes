import sbtrelease.ReleaseStateTransformations._
import sbtreleasenotes.ReleaseNotesPlugin._
import sbtreleasenotes.ReleaseNotesTransformations._
import scala.sys.process.Process

lazy val root = (project in file("."))
  .settings(
    version := "0.0.1",
    scalaVersion := "2.12.18",
    releaseSettings,
    TaskKey[Unit]("checkMessage") := {
      val process = Process("git", Seq("log", "-1", "--pretty=%B"))
      val expected = "This is a custom commit message for version 0.0.1"
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
  releaseNotesFile := baseDirectory.value / "release/release-notes-custom.md",
  releaseNotesNextVersionPlaceholder := "__next__",
  releaseNotesDraftFolder := baseDirectory.value / "drafts/release-notes-custom",
  releaseNotesDraftTemplate := "### New things\n\n$CHANGES\n",
  releaseNotesDraftExcludes := Seq("""README\.md""").map(_.r),
  releaseNotesFailIfNotUpdated := true,
  releaseNotesDraftCommitRegexes := Seq(""".*\[mytag\].*""").map(_.r),
  releaseNotesBranch := "mymain",
  releaseNotesUpdate / releaseNotesUpdateCommitMessage := s"This is a custom commit message for version ${(ThisBuild / version).value}",
  releaseVersionBump := sbtrelease.Version.Bump.NextStable
)
