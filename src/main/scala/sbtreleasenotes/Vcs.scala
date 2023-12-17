package sbtreleasenotes

import sbt.State
import sbtrelease.Git

import java.io.File

/* Vcs */

class Vcs(st: State) {
  private val vcs = sbtrelease.Vcs
    .detect(st.baseDir)
    .flatMap {
      case git: Git => Some(git)
      case _ => None
    }
    .getOrElse(sys.error("Aborting release. Working directory is not a Git repository."))

  def add(file: File): Unit = {
    vcs.add(file.getPath).run().exitValue()
  }

  def commit(msg: String): Unit = {
    vcs.commit(message = msg, sign = false, signOff = false).run().exitValue()
  }

  def currentBranch: String = vcs.cmd("rev-parse", "--abbrev-ref", "HEAD").lineStream.mkString("")

  def newCommits(main: String): List[String] = {
    vcs.cmd("log", s"$main..${currentBranch}", "--pretty=format:%s").lineStream.toList
  }
}
