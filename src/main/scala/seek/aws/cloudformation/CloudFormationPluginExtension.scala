package seek.aws
package cloudformation

import java.io.File

import cats.effect.IO
import org.gradle.api.Project
import org.gradle.api.file.FileCollection
import seek.aws.LazyProperty._
import seek.aws.config.LookupProject
import simulacrum.typeclass

import scala.collection.JavaConverters._

class CloudFormationPluginExtension(implicit project: Project) {
  import HasLazyProperties._

  private[cloudformation] val stackName = lazyProperty[String]("stackName")
  def stackName(v: Any): Unit = stackName.set(v)

  private[cloudformation] val templateFile = lazyProperty[File]("templateFile")
  def templateFile(v: Any): Unit = templateFile.set(v)

  private[cloudformation] val policyFile = lazyProperty[File]("policyFile")
  def policyFile(v: Any): Unit = policyFile.set(v)

  private var _parameters: Map[String, Any] = Map.empty
  def parameters(v: java.util.Map[String, Any]): Unit = _parameters = v.asScala.toMap

  private var _tags: Map[String, Any] = Map.empty
  def tags(v: java.util.Map[String, Any]): Unit = _tags = v.asScala.toMap

  private var lookupTags: List[String] = List.empty
  def tags(v: java.util.List[String]): Unit = lookupTags = v.asScala.toList

  private[cloudformation] def parameters: IO[Map[String, String]] =
    renderValues(_parameters)

  private[cloudformation] def tags: IO[Map[String, String]] =
    renderValues(_tags).flatMap { ts =>
      lookupTags.foldLeft(IO.pure(ts)) { (z, t) =>
        for {
          zz <- z
          tv <- LookupProject.lookup(project, pascalToCamelCase(t))
        } yield zz + (t -> tv)
      }
    }
}

@typeclass trait HasCloudFormationPluginExtension[A] {
  def cfnExt(a: A): CloudFormationPluginExtension
}

