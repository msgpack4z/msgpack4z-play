import sbt._, Keys._
import sbtrelease.ReleasePlugin
import sbtrelease.ReleasePlugin.autoImport._
import sbtrelease.ReleaseStateTransformations._
import com.jsuereth.sbtpgp.PgpKeys

object Common {

  private[this] val tagName = Def.setting {
    s"v${if (releaseUseGlobalVersion.value) (ThisBuild / version).value else version.value}"
  }

  val tagOrHash = Def.setting {
    if (isSnapshot.value) gitHash() else tagName.value
  }

  private[this] def gitHash(): String = sys.process.Process("git rev-parse HEAD").lineStream_!.head

  private[this] val unusedWarnings = Seq(
    "-Ywarn-unused",
  )

  val settings = Seq(
    ReleasePlugin.extraReleaseCommands
  ).flatten ++ Seq(
    publishTo := (if (isSnapshot.value) None else localStaging.value),
    commands += Command.command("updateReadme")(UpdateReadme.updateReadmeTask),
    releaseCrossBuild := true,
    releaseTagName := tagName.value,
    releaseProcess := Seq[ReleaseStep](
      checkSnapshotDependencies,
      inquireVersions,
      runClean,
      runTest,
      setReleaseVersion,
      commitReleaseVersion,
      UpdateReadme.updateReadmeProcess,
      tagRelease,
      ReleaseStep(
        action = { state =>
          val extracted = Project extract state
          extracted.runAggregated(extracted.get(thisProjectRef) / (Global / PgpKeys.publishSigned), state)
        },
        enableCrossBuild = true
      ),
      releaseStepCommand("sonaRelease"),
      setNextVersion,
      commitNextVersion,
      UpdateReadme.updateReadmeProcess,
      pushChanges
    ),
    organization := "com.github.xuwei-k",
    homepage := Some(url("https://github.com/msgpack4z")),
    licenses := Seq("MIT License" -> url("http://www.opensource.org/licenses/mit-license.php")),
    scalacOptions ++= Seq(
      "-release:11",
      "-deprecation",
      "-unchecked",
      "-language:existentials",
      "-language:higherKinds",
      "-language:implicitConversions",
    ),
    scalacOptions ++= {
      CrossVersion.partialVersion(scalaVersion.value) match {
        case Some((2, _)) =>
          Seq("-Xlint") ++ unusedWarnings
        case _ =>
          Nil
      }
    },
    (Compile / doc / scalacOptions) ++= {
      val tag = tagOrHash.value
      CrossVersion.partialVersion(scalaVersion.value) match {
        case Some((2, _)) =>
          Seq(
            "-sourcepath",
            (LocalRootProject / baseDirectory).value.getAbsolutePath,
            "-doc-source-url",
            s"https://github.com/msgpack4z/msgpack4z-play/tree/${tag}€{FILE_PATH}.scala"
          )
        case _ =>
          Nil
      }
    },
    pomExtra :=
      <developers>
        <developer>
          <id>xuwei-k</id>
          <name>Kenji Yoshida</name>
          <url>https://github.com/xuwei-k</url>
        </developer>
      </developers>
      <scm>
        <url>git@github.com:msgpack4z/msgpack4z-play.git</url>
        <connection>scm:git:git@github.com:msgpack4z/msgpack4z-play.git</connection>
        <tag>{tagOrHash.value}</tag>
      </scm>,
    description := "msgpack4z play-json binding",
    pomPostProcess := { node =>
      import scala.xml._
      import scala.xml.transform._
      def stripIf(f: Node => Boolean) = new RewriteRule {
        override def transform(n: Node) =
          if (f(n)) NodeSeq.Empty else n
      }
      val stripTestScope = stripIf { n => n.label == "dependency" && (n \ "scope").text == "test" }
      new RuleTransformer(stripTestScope).transform(node)(0)
    }
  ) ++ Seq(Compile, Test).flatMap(c => c / console / scalacOptions ~= { _.filterNot(unusedWarnings.toSet) })

}
