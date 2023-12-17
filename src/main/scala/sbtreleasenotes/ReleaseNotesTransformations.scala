package sbtreleasenotes

import sbt.*
import sbt.Keys.*
import sbtrelease.ReleasePlugin.autoImport.{ReleaseStep, releaseStepTask}
import sbtreleasenotes.ReleaseNotesPlugin.autoImport.*

import java.io.PrintWriter
import scala.io.Source
import scala.util.matching.Regex

object ReleaseNotesTransformations {

  val MdExtension = ".md"

  /* update release notes release step */

  lazy val updateReleaseNotes: ReleaseStep = releaseStepTask(releaseNotesUpdate)

  /* update release notes task implementation */

  lazy val updateReleaseNotesTask = Def.task {
    val draftFiles = getDraftFiles(releaseNotesDraftFolder.value, releaseNotesDraftExcludes.value)
    val newContent = extractNewContent(draftFiles)
    val vcs = new Vcs(state.value)
    val commitMessage = (releaseNotesUpdate / releaseNotesUpdateCommitMessage).value
    if (newContent.trim.stripLineEnd != "") {
      // replace next version placeholder with new content
      val source = Source.fromFile(releaseNotesFile.value)
      val fileContent = source.mkString
      val placeholder = releaseNotesNextVersionPlaceholder.value
      val replacement = s"$placeholder\n\n## ${version.value}\n\n$newContent"
      val replacedContents =
        fileContent.replaceFirst(Regex.quoteReplacement(placeholder), Regex.quoteReplacement(replacement))
      val writer = new PrintWriter(releaseNotesFile.value)
      writer.write(replacedContents)
      writer.close()
      source.close()

      // delete draft files
      draftFiles.foreach(_.delete())

      // add/commit release notes file and draft deletion
      vcs.add(releaseNotesFile.value)
      vcs.add(releaseNotesDraftFolder.value)
      vcs.commit(commitMessage)
    } else if (releaseNotesFailIfNotUpdated.value) {
      sys.error("Aborting release. Release-notes file was not updated and releaseNotesFailIfNotUpdated = true.")
    }
    releaseNotesFile.value
  }

  private def getDraftFiles(draftFolder: File, excludes: Seq[Regex]): Array[File] = {
    if (draftFolder.isDirectory) {
      draftFolder
        .listFiles(_.getName.toLowerCase.endsWith(MdExtension))
        .filterNot(file => excludes.exists(matches(_, file.getName)))
    } else {
      Array.empty
    }
  }

  private def extractNewContent(draftFiles: Array[File]): String = {
    val fileContents = draftFiles.map { file =>
      val source = Source.fromFile(file)
      val content = source.getLines().mkString("\n").trim.stripLineEnd
      source.close()
      content
    }
    fileContents.mkString("\n")
  }

  /* draft release notes task implementation */

  lazy val draftReleaseNotesTask = Def.task {
    // create draft dir if it does not exist
    createReleaseNotesFolder(releaseNotesDraftFolder.value)
    // compute draft content from commits
    val vcs = new Vcs(state.value)
    val releaseCommits =
      vcs.newCommits(releaseNotesBranch.value).filter(c => releaseNotesDraftCommitRegexes.value.exists(matches(_, c)))
    val changes = Regex.quoteReplacement(releaseCommits.map(c => s"- $c").mkString("\n"))
    val draftContent = releaseNotesDraftTemplate.value.replaceFirst(Regex.quoteReplacement("$CHANGES"), changes)
    // write draft content in release file
    val releaseNotesDraftFileName = s"release_${vcs.currentBranch}.md"
    val releaseNotesDraftFile = releaseNotesDraftFolder.value / releaseNotesDraftFileName
    val writer = new PrintWriter(releaseNotesDraftFile)
    writer.write(draftContent)
    writer.close()
    releaseNotesDraftFile
  }

  private def createReleaseNotesFolder(folder: File): Unit = {
    if (!folder.exists()) {
      folder.mkdirs()
    }
  }

  def matches(regex: Regex, s: String): Boolean =
    regex.pattern.matcher(s).matches

}
