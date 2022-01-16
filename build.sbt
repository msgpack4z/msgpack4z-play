import build._

Global / onChangedBuildSource := ReloadOnSourceChanges

val msgpack4zPlay = crossProject(JSPlatform, JVMPlatform).in(file(".")).settings(
  Common.settings,
  scalapropsCoreSettings,
  name := msgpack4zPlayName,
  libraryDependencies ++= (
    ("com.typesafe.play" %%% "play-json" % "2.9.2" cross CrossVersion.for3Use2_13) ::
    ("com.github.xuwei-k" %%% "msgpack4z-core" % "0.5.2") ::
    ("com.github.xuwei-k" %%% "msgpack4z-native" % "0.3.8" % "test") ::
    ("com.github.scalaprops" %%% "scalaprops" % "0.8.4" % "test") ::
    Nil
  )
).jsSettings(
  scalacOptions ++= {
    CrossVersion.partialVersion(scalaVersion.value) match {
      case Some((2, _)) =>
        val a = (LocalRootProject / baseDirectory).value.toURI.toString
        val g = "https://raw.githubusercontent.com/msgpack4z/msgpack4z-play/" + Common.tagOrHash.value
        Seq(s"-P:scalajs:mapSourceURI:$a->$g/")
      case _ =>
        Nil
    }
  },
  scalaJSLinkerConfig ~= { _.withSemantics(_.withStrictFloats(true)) },
  Test / scalaJSStage := FastOptStage
).jvmSettings(
  libraryDependencies ++= (
    ("com.github.xuwei-k" % "msgpack4z-java" % "0.3.6" % "test") ::
    ("com.github.xuwei-k" % "msgpack4z-java06" % "0.2.0" % "test") ::
    Nil
  )
)

val msgpack4zPlayJVM = msgpack4zPlay.jvm
val msgpack4zPlayJS = msgpack4zPlay.js

val root = Project("root", file(".")).settings(
  Common.settings,
  commands += Command.command("testSequential"){
    List(msgpack4zPlayJVM, msgpack4zPlayJS).map(_.id + "/test") ::: _
  },
  PgpKeys.publishLocalSigned := {},
  PgpKeys.publishSigned := {},
  publishLocal := {},
  publish := {},
  Compile / publishArtifact := false
).aggregate(
  msgpack4zPlayJS, msgpack4zPlayJVM
)
