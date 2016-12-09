package com.vsct.dt.strowgr.maze

import com.github.dockerjava.api.command.CreateContainerCmd
import fr.vsct.dt.maze.topology.SingleContainerClusterNode

object  Sidekick {

  class SidekickNode(lookup: String, nsqHost: String, mono: Boolean = false) extends SingleContainerClusterNode {
    override def serviceContainer: CreateContainerCmd = "strowgr/sidekick:0.2.3"
      .withCmd("-verbose=true")
      .withEnv(s"LOOKUP_ADDR=$lookup:4161", s"PRODUCER_ADDR=$nsqHost:4150", s"PRODUCER_REST_ADDR=http://$nsqHost:4151", s"VIP=$hostname", s"ID=$hostname")
    override def servicePort: Int = 50000
  }

}
