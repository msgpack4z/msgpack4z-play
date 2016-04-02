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
    scalapropsVersion := "0.3.1",
    libraryDependencies ++= (
      ("com.typesafe.play" %% "play-json" % "2.5.0") ::
      ("com.github.xuwei-k" %% "msgpack4z-core" % "0.3.2") ::
      ("com.github.xuwei-k" % "msgpack4z-java07" % "0.2.0" % "test") ::
      ("com.github.xuwei-k" % "msgpack4z-java06" % "0.2.0" % "test") ::
      ("com.github.xuwei-k" %% "msgpack4z-native" % "0.3.0" % "test") ::
      Nil
    )
  ).settings(
    Sxr.subProjectSxr(Compile, "classes.sxr"): _*
  )

}
