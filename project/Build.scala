import sbt._, Keys._

object build extends Build {

  private val msgpack4zPlayName = "msgpack4z-play"
  val modules = msgpack4zPlayName :: Nil

  lazy val msgpack4z = Project("msgpack4z-play", file(".")).settings(
    Common.settings: _*
  ).settings(
    name := msgpack4zPlayName,
    libraryDependencies ++= (
      ("com.typesafe.play" %% "play-json" % "2.3.8") ::
      ("com.github.xuwei-k" %% "msgpack4z-core" % "0.1.2") ::
      ("org.scalacheck" %% "scalacheck" % "1.12.2" % "test") ::
      ("com.github.xuwei-k" % "msgpack4z-java07" % "0.1.3" % "test").exclude("org.msgpack", "msgpack-core") ::
      ("org.msgpack" % "msgpack-core" % "0.7.0-p8" % "test") ::
      ("com.github.xuwei-k" % "msgpack4z-java06" % "0.1.0" % "test") ::
      ("com.github.xuwei-k" %% "msgpack4z-native" % "0.1.0" % "test") ::
      Nil
    )
  ).settings(
    Sxr.subProjectSxr(Compile, "classes.sxr"): _*
  )

}
