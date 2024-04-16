import build._

Global / onChangedBuildSource := ReloadOnSourceChanges

val msgpack4zPlay = crossProject(JSPlatform, JVMPlatform, NativePlatform)
  .in(file("."))
  .settings(
    Common.settings,
    scalapropsCoreSettings,
    name := msgpack4zPlayName,
    libraryDependencies ++= Seq(
      "org.playframework" %%% "play-json" % "3.0.2",
      "com.github.xuwei-k" %%% "msgpack4z-core" % "0.6.2",
      "com.github.xuwei-k" %%% "msgpack4z-native" % "0.4.0" % "test",
      "com.github.scalaprops" %%% "scalaprops" % "0.9.1" % "test",
    )
  )
  .nativeSettings(
    scalapropsNativeSettings
  )
  .jsSettings(
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
  )
  .jvmSettings(
    libraryDependencies ++= Seq(
      "com.github.xuwei-k" % "msgpack4z-java" % "0.4.0" % "test",
      "com.github.xuwei-k" % "msgpack4z-java06" % "0.2.0" % "test",
    )
  )

val msgpack4zPlayJVM = msgpack4zPlay.jvm
val msgpack4zPlayJS = msgpack4zPlay.js
val msgpack4zPlayNative = msgpack4zPlay.native

Common.settings
commands += Command.command("testSequential") {
  List(msgpack4zPlayJVM, msgpack4zPlayJS, msgpack4zPlayNative).map(_.id + "/test") ::: _
}
PgpKeys.publishLocalSigned := {}
PgpKeys.publishSigned := {}
publishLocal := {}
publish := {}
Compile / publishArtifact := false
