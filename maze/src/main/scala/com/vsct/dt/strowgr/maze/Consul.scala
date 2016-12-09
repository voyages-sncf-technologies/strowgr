package com.vsct.dt.strowgr.maze

import com.github.dockerjava.api.command.CreateContainerCmd
import fr.vsct.dt.maze.core.Commands.exec
import fr.vsct.dt.maze.core.Predef._
import fr.vsct.dt.maze.core.{Execution, Predicate}
import fr.vsct.dt.maze.helpers.Http.{HttpEnabled, HttpResponse}
import fr.vsct.dt.maze.topology.SingleContainerClusterNode

object Consul {

  class ConsulNode extends SingleContainerClusterNode with HttpEnabled {

    override def serviceContainer: CreateContainerCmd = "gliderlabs/consul-server".withCmd("-bootstrap")
    override def servicePort: Int = 8500


    def write(key: String, value: String): Execution[HttpResponse] = {
      httpPut(s"/v1/kv/$key", value, "text/plain")
    }

    def read(key: String): Execution[HttpResponse] = {
      httpGet(s"/v1/kv/$key?raw")
    }

    def init(): Unit = {
      exec(write("ports", "{}"))
      exec(write("haproxyversions", """["1.4.22","1.4.27","1.5.18"]"""))
    }

    def isReady(): Predicate = {
      write("health", "ok").status is 200
    }.labeled("Consul node is ready")

    def portOfFrontend(application: String, platform: String, frontend: String): Execution[Int] = {
      read("ports").responseAs(classOf[Map[String, Int]]).get(s"$application/$platform-$frontend")
    }
  }


}
