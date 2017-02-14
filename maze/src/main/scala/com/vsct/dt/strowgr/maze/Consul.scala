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

    def init(vip: String): Unit = {
      exec(write("ports", "{}"))
      exec(write("haproxyversions", """["1.4.22","1.4.27","1.5.18"]"""))
      exec(write("haproxy/preproduction/autoreload", "true"))
      exec(write("haproxy/preproduction/disabled", "false"))
      exec(write("haproxy/preproduction/name", "preproduction"))
      exec(write("haproxy/preproduction/platform", "preproduction"))
      exec(write("haproxy/preproduction/vip", vip))
      exec(write("haproxy/preproduction/binding/0", vip))
    }

    def isReady(): Predicate = {
      write("health", "ok").status is 200
    }.labeled("Consul node is ready")

    def portOfFrontend(application: String, platform: String, frontend: String): Execution[Int] = {
      read("ports").responseAs(classOf[Map[String, Int]]).get(s"$application/$platform-$frontend")
    }
  }


}
