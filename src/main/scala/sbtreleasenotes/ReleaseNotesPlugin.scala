package sbtreleasenotes

import sbt.*
import sbt.Keys.*
import sbtrelease.ReleasePlugin
import sbtreleasenotes.ReleaseNotesTransformations.*

import java.io.File
import scala.util.matching.Regex

object ReleaseNotesPlugin extends AutoPlugin {

  override def requires: Plugins = ReleasePlugin

  override def trigger = allRequirements

  object autoImport {
    val releaseNotesFile =
      settingKey[File]("Absolute path of the release notes file (it should have the .md extension).")
    val releaseNotesNextVersionPlaceholder = settingKey[String](
      "Placeholder in the project release notes file that will be replaced with the new released changes."
    )
    val releaseNotesDraftFolder = settingKey[File]("Absolute path of the release notes draft folder.")
    val releaseNotesDraftTemplate = settingKey[String](
      "Template for the generated draft `.md` file containing the contribution to the release notes."
    )
    val releaseNotesDraftExcludes =
      settingKey[Seq[Regex]]("Regexes for files in the draft folder that should be ignored.")
    val releaseNotesFailIfNotUpdated =
      settingKey[Boolean]("If true, the build will fail when we cannot update the release notes")
    val releaseNotesDraftCommitRegexes =
      settingKey[Seq[Regex]]("Regexes for commits that should be part of the release notes.")
    val releaseNotesBranch = settingKey[String]("Main repository branch where we release new code.")
    val releaseNotesUpdateCommitMessage = settingKey[String]("Commit message for the release notes update.")

    lazy val releaseNotesDraft = taskKey[File]("Draft the release notes update.")
    lazy val releaseNotesUpdate =
      taskKey[File]("Update the release notes file including the contributions from the draft `.md` files.")
  }

  import autoImport.*

  override lazy val globalSettings: Seq[Setting[_]] = Seq(
    releaseNotesNextVersionPlaceholder := "$NEXT_VERSION",
    releaseNotesDraftTemplate := "### Changes\n$CHANGES",
    releaseNotesDraftExcludes := Seq.empty,
    releaseNotesFailIfNotUpdated := false,
    releaseNotesDraftCommitRegexes := Seq(""".*""").map(_.r),
    releaseNotesBranch := "main"
  )

  override lazy val buildSettings: Seq[Setting[_]] = Seq(
    releaseNotesFile := baseDirectory.value / "release-notes.md",
    releaseNotesDraftFolder := baseDirectory.value / "release-notes",
    releaseNotesDraft := draftReleaseNotesTask.value
  )

  override lazy val projectSettings: Seq[Setting[_]] = Seq(
    releaseNotesUpdate := updateReleaseNotesTask.value,
    releaseNotesUpdate / releaseNotesUpdateCommitMessage := s"Update release notes for version ${version.value}"
  )

}
