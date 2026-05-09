import build._

Global / onChangedBuildSource := ReloadOnSourceChanges

val scalaVersions = Seq("2.12.21", "2.13.18", "3.3.7")

val msgpack4zPlay = projectMatrix
  .defaultAxes()
  .in(file("."))
  .settings(
    Common.settings,
    scalapropsCoreSettings,
    name := msgpack4zPlayName,
    libraryDependencies ++= Seq(
      "com.github.xuwei-k" %%% "msgpack4z-core" % "0.6.2",
      "com.github.xuwei-k" %%% "msgpack4z-native" % "0.4.0" % "test",
      "com.github.scalaprops" %%% "scalaprops" % "0.10.1" % "test",
    )
  )
  .nativePlatform(
    scalaVersions,
    Def.settings(
      libraryDependencies += "org.playframework" %%% "play-json" % "3.1.0-M10",
      scalapropsNativeSettings,
    )
  )
  .jsPlatform(
    scalaVersions,
    Def.settings(
      libraryDependencies += "org.playframework" %%% "play-json" % "3.0.6",
      scalacOptions += {
        val a = (LocalRootProject / baseDirectory).value.toURI.toString
        val g = "https://raw.githubusercontent.com/msgpack4z/msgpack4z-play/" + Common.tagOrHash.value
        val key = {
          if (scalaBinaryVersion.value == "3") {
            "-scalajs-mapSourceURI"
          } else {
            "-P:scalajs:mapSourceURI"
          }
        }
        s"${key}:$a->$g/"
      },
      Test / scalaJSStage := FastOptStage
    ),
  )
  .jvmPlatform(
    scalaVersions,
    Def.settings(
      libraryDependencies ++= Seq(
        "org.playframework" %%% "play-json" % "3.0.6",
        "com.github.xuwei-k" % "msgpack4z-java" % "0.4.0" % "test",
        "com.github.xuwei-k" % "msgpack4z-java06" % "0.2.0" % "test",
      ),
    ),
  )

Common.settings
autoScalaLibrary := false
TaskKey[Unit]("testSequential") := Def
  .sequential(
    msgpack4zPlay
      .allProjects()
      .map(_._1)
      .sortBy(_.id)
      .flatMap(p =>
        Seq(
          Def.task(streams.value.log.info(s"start ${p.id} test")),
          p / Test / test
        )
      )
  )
  .value
PgpKeys.publishLocalSigned := {}
PgpKeys.publishSigned := {}
publishLocal := {}
publish := {}
Compile / publishArtifact := false
Compile / scalaSource := (LocalRootProject / baseDirectory).value / "dummy"
Test / scalaSource := (LocalRootProject / baseDirectory).value / "dummy"
