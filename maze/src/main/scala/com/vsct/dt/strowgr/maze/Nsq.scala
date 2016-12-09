package com.vsct.dt.strowgr.maze

import com.github.dockerjava.api.command.CreateContainerCmd
import fr.vsct.dt.maze.core.Execution
import fr.vsct.dt.maze.helpers.Http.{HttpEnabled, HttpResponse}
import fr.vsct.dt.maze.topology.SingleContainerClusterNode

object Nsq {

  private val image: String = "nsqio/nsq:v0.3.7"

  class Nsqd(lookup: String) extends SingleContainerClusterNode with HttpEnabled {
    override def serviceContainer: CreateContainerCmd = image
      .withCmd("/nsqd", s"--lookupd-tcp-address=$lookup:4160", s"--broadcast-address=$hostname", s"-tcp-address=$hostname:4150")

    override def servicePort: Int = 4151

    def postMessage(topic: String, message: String, contentType: String = "application/json;charset=UTF-8"): Execution[HttpResponse] = {
      httpPost(s"/pub?topic=$topic", message, contentType)
    }

    def registerServer(application: String, platform: String, host: String, ip: String, port: Int, backend: String, context: String = ""): Execution[HttpResponse] = {
      postMessage(
        topic = "register_server",
        message =
          s"""{
             |  "header": {
             |    "application":"$application",
             |    "platform": "$platform"
             |  },
             |  "server": {
             |    "id": "$host-$port-$backend",
             |    "backendId":"$backend",
             |    "ip":"$ip",
             |    "port": $port,
             |    "context":{$context}
             |  }
             |}""".stripMargin)
    }
  }

  class NsqLookup extends SingleContainerClusterNode {
    override def serviceContainer: CreateContainerCmd = image.withCmd("/nsqlookupd", s"-http-address=$hostname:4161", s"-tcp-address=$hostname:4160")

    override def servicePort: Int = 4161
  }

  class NsqAdmin(lookup: String) extends SingleContainerClusterNode {
    override def serviceContainer: CreateContainerCmd = image.withCmd("/nsqadmin", s"-http-address=$hostname:4171", s"--lookupd-http-address=$lookup:4161")

    override def servicePort: Int = 4171
  }

}
