import sbt._, Keys._
import sbtrelease.Git
import sbtrelease.ReleasePlugin.autoImport.ReleaseStep

object UpdateReadme {

  val updateReadmeTask: State => State = { state =>
    val extracted = Project.extract(state)
    val v = extracted.get(version)
    val org = extracted.get(organization)
    val modules = build.modules
    val snapshotOrRelease = if (extracted.get(isSnapshot)) "snapshots" else "releases"
    val readme = "README.md"
    val readmeFile = file(readme)
    val newReadme =
      IO.readLines(readmeFile)
        .map { line =>
          val matchReleaseOrSnapshot = line.contains("SNAPSHOT") == v.contains("SNAPSHOT")
          def n = modules(modules.indexWhere(line.contains))
          val libraryDependenciesLine = line.startsWith("libraryDependencies") && matchReleaseOrSnapshot
          if (libraryDependenciesLine && line.contains(" %% ")) {
            s"""libraryDependencies += "${org}" %% "$n" % "$v""""
          } else if (libraryDependenciesLine && line.contains(" %%% ")) {
            s"""libraryDependencies += "${org}" %%% "$n" % "$v""""
          } else line
        }
        .mkString("", "\n", "\n")
    IO.write(readmeFile, newReadme)
    val git = new Git(extracted.get(baseDirectory))
    git.add(readme) ! state.log
    git.commit(message = "update " + readme, sign = false, signOff = false) ! state.log
    sys.process.Process("git diff HEAD^") ! state.log
    state
  }

  val updateReadmeProcess: ReleaseStep = updateReadmeTask
}
