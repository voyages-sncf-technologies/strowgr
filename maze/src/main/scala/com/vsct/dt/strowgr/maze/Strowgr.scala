package com.vsct.dt.strowgr.maze

import com.github.dockerjava.api.command.CreateContainerCmd
import com.typesafe.scalalogging.StrictLogging
import fr.vsct.dt.maze.core.Predef._
import fr.vsct.dt.maze.core.{Execution, Predicate}
import fr.vsct.dt.maze.helpers.Http.{HttpEnabled, HttpResponse}
import fr.vsct.dt.maze.topology.SingleContainerClusterNode

import scala.language.postfixOps

object Strowgr extends StrictLogging {

  val defaultTemplateUrl = "http://gitlab.socrate.vsct.fr/dt/haproxy-templates-horsprod/raw/1.0-DEFAULT/DEFAULT/haproxy_default_template.conf"

  def defaultParams(application: String, platform: String): Map[String, String] = {
    Map[String, String](
      "application" -> application,
      "platform" -> platform,
      "templateUri" -> defaultTemplateUrl
    )
  }

  class AdminNode(consulHost: String, lookupHost: String, nsqHost: String) extends SingleContainerClusterNode with HttpEnabled {
    override def serviceContainer: CreateContainerCmd = "strowgr/admin:latest"
      .withEntrypoint("java", s"-Ddw.repository.host=$consulHost", s"-Ddw.nsqLookup.host=$lookupHost", s"-Ddw.nsqProducer.host=$nsqHost", "-jar", "/app.jar", "server", "/server.yaml")

    override def servicePort: Int = 8080

    def createHap(id: String, name: String, binding: String, platform: String, autoReload: Boolean = true): Execution[HttpResponse] = {
      httpPut(s"/api/haproxy/$id",
        s"""{"name":"$name","bindings":{"0":"$binding"},"platform":"$platform","autoreload":$autoReload}""",
        "application/json;charset=UTF-8")
    }

    def isReady(): Predicate = {
      httpGet("/api/admin/version") isOk
    }.labeled("admin is ready")

    def createEntrypoint(configuration: CreateEntryPoint): Execution[HttpResponse] = {

      def frontend(end: EndConfiguration): String = {
        end.toList.map {
          case (i, values) =>
            val jsonValues = values.map {
              case (left, right) => s""" "$left": "$right" """
            }

            s"""{"id": "$i", "context": ${jsonValues.mkString("{", ", ", "}")}}"""
        }.mkString("[", ", ", "]")
      }

      def backend(end: EndConfiguration): String = {
        end.toList.map {
          case (i, values) =>
            val jsonValues = values.map {
              case (left, right) => s""" "$left": "$right" """
            }

            s"""{"id": "$i", "servers": [], "context": ${jsonValues.mkString("{", ", ", "}")}}"""
        }.mkString("[", ", ", "]")
      }

      val jsonValues = configuration.context.map {
        case (left, right) => s""" "$left": "$right" """
      }

      val data =
        s"""{
           | "haproxy": "${configuration.haproxy}",
           | "hapUser": "${configuration.user}",
           | "hapVersion": "${configuration.hapVersion}",
           | "bindingId": ${configuration.binding},
           | "frontends": ${frontend(configuration.frontEnds)},
           | "backends": ${backend(configuration.backends)},
           | "context": ${jsonValues.mkString("{", ", ", "}")}
           |}""".stripMargin

      logger.debug("Creating entrypoint with value: {}", data)

      httpPut(s"/api/entrypoints/${configuration.context("application").toUpperCase()}/${configuration.context("platform").toUpperCase()}", data, "application/json")
    }

    private def configuration(application: String, platform: String, state: String): Execution[Option[EntryPointConfiguration]] = {
      httpGet(s"/api/entrypoints/$application/$platform/$state")
        .responseAs(classOf[EntryPointConfiguration])
        .map {
          case configuration: EntryPointConfiguration => Some(configuration)
          case _ => None
        }.recoverWith(None)
    }.labeled(s"$state configuration for $application/$platform")

    def currentConfiguration(application: String, platform: String): Execution[Option[EntryPointConfiguration]] =
      configuration(application, platform, "current")

    def pendingConfiguration(application: String, platform: String): Execution[Option[EntryPointConfiguration]] =
      configuration(application, platform, "pending")

    def committingConfiguration(application: String, platform: String): Execution[Option[EntryPointConfiguration]] =
      configuration(application, platform, "committing")



  }

  case class EntryPointConfiguration(
                                    haproxy: String,
                                    hapUser: String,
                                    hapVersion: String,
                                    bindingId: Int,
                                    frontends: List[EntryPointFrontend],
                                    backends: List[EntryPointBackend],
                                    context: Map[String, String])

  case class EntryPointFrontend(id: String, context: Map[String, String])
  case class EntryPointBackend(id: String, servers: List[EntryPointBackendServer], context: Map[String, String])
  case class EntryPointBackendServer(id: String, hostname: String, ip: String, port: String, context: Map[String, String], contextOverride: Map[String, String])

  type Context = Map[String, String]
  type EndConfiguration = Map[String, Context]

  case class CreateEntryPoint(haproxy: String = "preproduction",
                              user: String = "root",
                              hapVersion: String = "1.5.18",
                              binding: Int = 0,
                              frontEnds: EndConfiguration = Map("FRONTEND" -> Map()),
                              backends: EndConfiguration = Map("BACKEND" -> Map()),
                              context: Context = defaultParams("TEST", "TEST"))

}
