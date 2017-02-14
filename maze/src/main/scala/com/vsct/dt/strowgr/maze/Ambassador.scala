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
