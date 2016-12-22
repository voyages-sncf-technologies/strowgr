package com.vsct.dt.strowgr.maze

import com.github.dockerjava.api.command.CreateContainerCmd
import fr.vsct.dt.maze.core.Predicate
import fr.vsct.dt.maze.helpers.Http.HttpEnabled
import fr.vsct.dt.maze.topology.SingleContainerClusterNode
import fr.vsct.dt.maze.core.Predef._

object  Sidekick {

  class SidekickNode(lookup: String, nsqHost: String, mono: Boolean = false) extends SingleContainerClusterNode with HttpEnabled{
    override def serviceContainer: CreateContainerCmd = "strowgr/sidekick:0.2.4"
      .withCmd("-verbose=true")
      .withEnv(s"LOOKUP_ADDR=$lookup:4161", s"PRODUCER_ADDR=$nsqHost:4150", s"PRODUCER_REST_ADDR=http://$nsqHost:4151", s"VIP=$hostname", s"ID=$hostname")
    override def servicePort: Int = 50000

    def isReady(): Predicate = {
      httpGet("/id") isOk
    }.labeled("sidekick is ready")
  }

}
