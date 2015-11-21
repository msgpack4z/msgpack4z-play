import sbt._, Keys._
import scalaprops.ScalapropsPlugin.autoImport._

object build extends Build {

  private val msgpack4zPlayName = "msgpack4z-play"
  val modules = msgpack4zPlayName :: Nil

  lazy val msgpack4z = Project("msgpack4z-play", file(".")).settings(
    Common.settings
  ).settings(
    scalapropsSettings
  ).settings(
    name := msgpack4zPlayName,
    scalapropsVersion := "0.1.16",
    libraryDependencies ++= (
      ("com.typesafe.play" %% "play-json" % "2.3.10") ::
      ("com.github.xuwei-k" %% "msgpack4z-core" % "0.1.2") ::
      ("com.github.xuwei-k" % "msgpack4z-java07" % "0.1.3" % "test").exclude("org.msgpack", "msgpack-core") ::
      ("org.msgpack" % "msgpack-core" % "0.7.0-p9" % "test") ::
      ("com.github.xuwei-k" % "msgpack4z-java06" % "0.1.1" % "test") ::
      ("com.github.xuwei-k" %% "msgpack4z-native" % "0.1.0" % "test") ::
      Nil
    )
  ).settings(
    Sxr.subProjectSxr(Compile, "classes.sxr"): _*
  )

}
