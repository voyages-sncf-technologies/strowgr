package com.vsct.dt.strowgr.maze

import com.github.dockerjava.api.command.CreateContainerCmd
import com.typesafe.scalalogging.StrictLogging
import fr.vsct.dt.maze.topology.SingleContainerClusterNode

object Backend extends StrictLogging {

  // The register method is at the nsq level, since the nginx image doesn't have stuff like curl.
  class BackendApp(nsqHost: String) extends SingleContainerClusterNode {
    override def serviceContainer: CreateContainerCmd = "nginx"
    override def servicePort: Int = 80
  }

}
