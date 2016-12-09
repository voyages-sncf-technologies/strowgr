package com.vsct.dt.strowgr.maze

import com.github.dockerjava.api.command.CreateContainerCmd
import com.github.dockerjava.api.model._
import fr.vsct.dt.maze.helpers.Http.HttpEnabled
import fr.vsct.dt.maze.topology.SingleContainerClusterNode

object Ambassador {

  class AmbassadorNode(targetIp: String, targetPort: Int) extends SingleContainerClusterNode with HttpEnabled {

    override def serviceContainer: CreateContainerCmd = "verb/socat"
      .withCmd(s"TCP-LISTEN:$servicePort,fork,bind=0.0.0.0", s"TCP:$targetIp:$targetPort")
      .withExposedPorts(new ExposedPort(12345, InternetProtocol.TCP))

    override def servicePort: Int = 12345
  }

}
