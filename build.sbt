import build._

Common.settings

scalapropsSettings

name := msgpack4zPlayName

scalapropsVersion := "0.3.4"

libraryDependencies ++= (
  ("com.typesafe.play" %% "play-json" % "2.5.1") ::
  ("com.github.xuwei-k" %% "msgpack4z-core" % "0.3.5") ::
  ("com.github.xuwei-k" % "msgpack4z-java" % "0.3.4" % "test") ::
  ("com.github.xuwei-k" % "msgpack4z-java06" % "0.2.0" % "test") ::
  ("com.github.xuwei-k" %% "msgpack4z-native" % "0.3.1" % "test") ::
  Nil
)

Sxr.settings
