import sbt.Keys._
import sbt._

/**
  * @author roy  create time 2019/06/05
  *         sbt打包多环境插件,打包命令
  *         dev:package
  *         prod:package
  *
  *
  */
object BuildEnvironmentsPlugin extends AutoPlugin {
  override def requires = plugins.JvmPlugin

  override def trigger = allRequirements

  val fff1 = (f: File) => {
    println("fff1==" + f.getAbsolutePath)
    f.getAbsolutePath.contains("resources")
  }

  object autoImport {
    lazy val Fuzz = config("dev") extend (Compile)
    lazy val Prod = config("prod") extend (Compile)
    lazy val Stag = config("stag") extend (Compile)

  }

  import autoImport._

  lazy val baseSettings: Seq[Def.Setting[_]] =
    Classpaths.configSettings ++
      Defaults.configSettings ++
      Seq(
        managedResourceDirectories := (managedResourceDirectories in Compile).value,
        managedSourceDirectories := (managedSourceDirectories in Compile).value,
        unmanagedSourceDirectories := (unmanagedSourceDirectories in Compile).value,
        unmanagedResourceDirectories := (unmanagedResourceDirectories in Compile).value,
      )

  lazy val devSettings: Seq[Def.Setting[_]] =
    baseSettings ++ Seq(
      //      excludeFilter in(Compile, unmanagedResources) := new SimpleFileFilter(fff1),
      excludeFilter in unmanagedResources := {
        //        val resources = (baseDirectory.value / "src" / "main" / "resources").getCanonicalPath
        new SimpleFileFilter(_.getAbsolutePath.endsWith("resources"))
      },
      unmanagedResourceDirectories += baseDirectory.value / "src" / Fuzz.name / "resourcesd"
    )
  lazy val prodSettings: Seq[Def.Setting[_]] =
    baseSettings ++ Seq(
      unmanagedResourceDirectories += baseDirectory.value / "src" / Prod.name / "resourcesd",
      excludeFilter in unmanagedResources := {
        new SimpleFileFilter(_.getAbsolutePath.endsWith("resources"))
      }
    )

  lazy val stagSettings: Seq[Def.Setting[_]] =
    baseSettings ++ Seq(
      unmanagedResourceDirectories += baseDirectory.value / "src" / Stag.name / "resourcesd",
      excludeFilter in unmanagedResources := {
        new SimpleFileFilter(_.getAbsolutePath.endsWith("resources"))
      }
    )

  override lazy val projectSettings = inConfig(Fuzz)(devSettings) ++ inConfig(Prod)(prodSettings)++ inConfig(Stag)(stagSettings)
}