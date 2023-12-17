# sbt-release-notes 

![CI](https://github.com/generoso/sbt-plugin-shell/workflows/CI/badge.svg)
[![codecov](https://codecov.io/gh/AmadeusITGroup/sbt-release-notes/graph/badge.svg?token=X7uHgfmZir)](https://codecov.io/gh/AmadeusITGroup/sbt-release-notes)

## Overview

This plugin provides a Release Step for [sbt-release](https://github.com/sbt/sbt-release/) to automatically update the release notes file.

### Why an automated release notes update?

The plugin targets scenarios where:
- the version is computed automatically during the [sbt-release](https://github.com/sbt/sbt-release/) release process
- the release notes file is kept in the same repository as the code
- there are many, frequent, and potentially concurrent contributions to the repository

In such scenarios, updating the release notes file manually can be quite frustrating, especially for very active development teams,
where there are often concurrent Pull Requests (PRs).

Indeed, if many PRs are ready to be merged:
- we do not know yet the version that will be released with a given PR (as it is computed dynamically by the release process and therefore depends on the merge order)
- we will end up having conflicts on the release notes file, as all PRs need to modify the same section of the same file

This plugin aims at removing the above pain points, thus simplifying the development team's life :)

### How does it work?

First of all, let's make a distinction between the following two files:
- the project release notes `.md` file
- a draft temporary `.md` file containing the contribution to the release notes for the current release

The workflow is then the following:
- The contributor puts a draft temporary `.md` file with the content to be added to the release notes in the folder defined by the `releaseNotesDraftFolder` setting.
  - Such draft file can be initialized from commit messages using the provided task `releaseNotesDraft`.
- During the [sbt-release](https://github.com/sbt/sbt-release/) release process, the plugin adds the content of the draft file mentioned above to the project release notes file, defined by the `releaseNotesFile` setting.
- At the end of the release process, the draft `.md` file gets deleted.

This approach (using temporary draft files) has been chosen to avoid conflicts on the release notes file in case of concurrent pull requests.
Indeed, only the plugin is touching the project release notes, while different contributors work on different temporary files.

### Ok, but how does it really work?

Let's see the plugin in action with a quick demo.

Release notes before:
```
# Release notes

$NEXT_VERSION

## 1.0.0

### Changes
- Added a first feature
```

Development and Release process:
```
# ... some development going on here ...
$ git commit -m "Added a new awesome feature"
$ sbt releaseNotesDraft 
$ git commit -am "Update release notes"
$ sbt 'release with-defaults' # let's imagine the new version is 1.1.0
```

Release notes after:
```
# Release notes

$NEXT_VERSION

## 1.1.0

### Changes
- Added a new awesome feature

## 1.0.0

### Changes
- Added a first feature
```

## How to use

### Activate the plugin

In you `project/plugins.sbt` add:

```scala
addSbtPlugin("io.github.generoso" % "sbt-release-notes" % <version>)
```

### Update your build.sbt

Plug the `updateReleaseNotes` Release Step into your `releaseProcess` and configure the plugin settings according to your preferences.

```scala
import sbtrelease.ReleaseStateTransformations._
import sbtreleasenotes.ReleaseNotesPlugin._

// ...

// sample release process
releaseProcess := Seq[ReleaseStep](
  inquireVersions,
  setReleaseVersion,
  commitReleaseVersion,
  updateReleaseNotes, // <------ update the release notes after committing the release version
  tagRelease,
  pushChanges,
  publishArtifacts,
  setNextVersion,
  commitNextVersion,
  pushChanges
)

// sample configuration
releaseNotesFile := file("CHANGELOG.md") // that's your release notes file
releaseNotesDraftFolder := file("drafts") // drafts folder
releaseNotesDraftExcludes := Seq("""README\.md""").map(_.r) // skip the README.md file in the drafts folder
releaseNotesFailIfNotUpdated := true // fail in case no release notes update is possible
releaseNotesDraftCommitRegexes := Seq(""".*\[release\].*""").map(_.r) // only consider commits with "[release]" to create the draft
releaseNotesBranch := "master" // consider "master" as main branch
```

### Set up your repository

- Create the folder defined by `releaseNotesDraftFolder`
  - by default `release-notes`, at the root of the repository
- Create the release notes file as defined by `releaseNotesFile`
  - by default `release-notes.md`, at the root of the repo 
  - the file must not be inside the `releaseNotesDraftFolder`
  - the file must contain the placeholder defined in `releaseNotesNextVersionPlaceholder`, that will be replaced by the released version changelog
  - for example, keeping the default placeholder, you can initialize the file with the following content:
    ```
    # Release notes
    
    $NEXT_VERSION
    ```

### Enjoy :)

Run your release process (manually or in your CI) as described in the quick demo above.

## More details

### Available settings

* `releaseNotesFile`
  * Absolute path of the release notes `.md` file (default `baseDirectory.value / "release-notes.md"`).
* `releaseNotesNextVersionPlaceholder`
  * Placeholder in the project release notes file that will be replaced with the new released changes coming from the draft `.md` file.
    It can be a plain string without special characters, or a shell-like variable starting with `$` (default `$NEXT_VERSION`).
* `releaseNotesDraftFolder`
  * Absolute path of the folder where the draft `.md` files containing the contribution to the release notes should be created (default `baseDirectory.value / "release-notes"`).
* `releaseNotesDraftTemplate`
  * Template for the generated draft `.md` file containing the contribution to the release notes. It must contain the `$CHANGES` placeholder. The default is:
    ```
    ### Changes
    $CHANGES
    ```
* `releaseNotesDraftExcludes`
  * Regular expressions for files in the draft folder that should be ignored (nothing is ignored by default).
* `releaseNotesDraftCommitRegexes`
  * Regular expressions for commits that should be part of the release notes (default `.*`). See the `releaseNotesDraft` task.
* `releaseNotesBranch`
  * Main repository branch where we release new code (default `main`).
* `releaseNotesFailIfNotUpdated`
  * If set to `true` the build will fail if no update to the release notes is found in the draft folder (default `false`).
* `releaseNotesUpdate / releaseNotesUpdateCommitMessage`
  * Commit message used when committing the update to the release notes file (default `s"Update release notes for version ${version.value}"`).
    If overridden, it must be scoped at the `releaseNotesUpdate` task scope.

### Available tasks

* `releaseNotesDraft`
  * Initialize the draft `.md` file corresponding to the release notes update, based on the commits that are in the
    current branch and not yet in the configured `releaseNotesBranch`, matching the regular expressions defined
    in `releaseNotesDraftCommitRegexes`. This task can be run manually, if you want to modify the draft generated before committing
    it, or it can be run by your CI, if you want to rely entirely on the commits matching the configured regular expressions.
* `releaseNotesUpdate`
  * Update the release notes file including the contributions from the draft `.md` files present in the `releaseNotesDraftFolder` folder.
    It allows to perform the update to the release notes outside of the [sbt-release](https://github.com/sbt/sbt-release/) release process.
    Note that this task will commit the deletion of the draft files and the update to the release notes (as it would happen during a normal
    release process), so if you are running this task just for testing purposes, remember to drop such commit.

### Release step

* `updateReleaseNotes`
  * Run the `releaseNotesUpdate` task within a [sbt-release](https://github.com/sbt/sbt-release/) release step. 
    Refer to the task documentation for more details.

## Limitations
 
- The plugin only works with Git repositories for the moment. 
- The repository setup has to be done (once) manually (see corresponding section above).

Contributions are welcome :)

## Contributing

### Build

```
sbt scripted                           # run tests
sbt coverageOn scripted coverageReport # run tests with coverage checks on
```

The build will fail if the 100% code coverage is not reached.

### Formatting

The project uses `ScalaFMT`. For IntelliJ IDEA, you can set up the formatting as follows:
```
Settings -> Editor -> Code Style -> Scala -> Formatter: ScalaFMT
```

### Code of conduct

We strive to maintain a welcoming and inclusive environment for everyone involved in this project, and we do not 
tolerate any form of harassment or disrespectful behavior.

## License

This project is licensed under the Apache License 2.0 - see the [LICENSE](LICENSE) file for details.

## Acknowledgements

Special thanks to [Mathieu Trampont](https://github.com/mtrampont) for providing exceptionally valuable feedback.