val demoAkkaStreamsPi = project
  .in(file("."))
  .enablePlugins(AutomateHeaderPlugin, GitVersioning)

organization    := "de.heikoseeberger"
name            := "demo-akka-streams-pi"
git.baseVersion := "0.1.0"
licenses        += ("Apache-2.0", url("http://www.apache.org/licenses/LICENSE-2.0"))

scalaVersion   := "2.11.7"
scalacOptions ++= List(
  "-unchecked",
  "-deprecation",
  "-language:_",
  "-target:jvm-1.8",
  "-encoding", "UTF-8"
)

unmanagedSourceDirectories.in(Compile) := List(scalaSource.in(Compile).value)
unmanagedSourceDirectories.in(Test)    := List(scalaSource.in(Test).value)

libraryDependencies ++= List(
  "com.typesafe.akka" %% "akka-actor"               % "2.4.0-RC3",
  "com.typesafe.akka" %% "akka-stream-experimental" % "1.0",
  "org.scalacheck"    %% "scalacheck"               % "1.12.5" % "test",
  "org.scalatest"     %% "scalatest"                % "2.2.5"  % "test"
)

initialCommands := """|import de.heikoseeberger.demoakkastreamspi._""".stripMargin

import scalariform.formatter.preferences._
preferences := preferences.value
  .setPreference(AlignSingleLineCaseStatements, true)
  .setPreference(AlignSingleLineCaseStatements.MaxArrowIndent, 100)
  .setPreference(DoubleIndentClassDeclaration, true)

import de.heikoseeberger.sbtheader.license.Apache2_0
HeaderPlugin.autoImport.headers := Map("scala" -> Apache2_0("2015", "Heiko Seeberger"))
