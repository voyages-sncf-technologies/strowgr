/*
 * Copyright (C) 2016 VSCT
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.vsct.dt.strowgr.maze

import com.github.dockerjava.api.command.CreateContainerCmd
import fr.vsct.dt.maze.core.Predef._
import fr.vsct.dt.maze.core.Predicate
import fr.vsct.dt.maze.helpers.Http.HttpEnabled
import fr.vsct.dt.maze.topology.SingleContainerClusterNode

object  Sidekick {

  class SidekickNode(lookup: String, nsqHost: String, mono: Boolean = false) extends SingleContainerClusterNode with HttpEnabled{
    override def serviceContainer: CreateContainerCmd = "strowgr/sidekick:0.2.6"
      .withCmd("-verbose=true")
      .withEnv(s"LOOKUP_ADDR=$lookup:4161", s"PRODUCER_ADDR=$nsqHost:4150", s"PRODUCER_REST_ADDR=http://$nsqHost:4151", "CLUSTER_ID=preproduction", s"VIP=$hostname", s"ID=$hostname")
    override def servicePort: Int = 50000

    def isReady(): Predicate = {
      httpGet("/id") isOk
    }.labeled("sidekick is ready")
  }

}
