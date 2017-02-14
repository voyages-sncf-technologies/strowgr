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
package com.vsct.dt.strowgr

import com.sun.xml.internal.messaging.saaj.util.ByteOutputStream
import com.vsct.dt.strowgr.maze.Strowgr.EntryPointConfiguration
import fr.vsct.dt.maze.core.Predef._
import fr.vsct.dt.maze.core.{Execution, Predicate, Result}

package object maze {

  implicit class EntryPointConfigurationExecution(val self: Execution[EntryPointConfiguration]) extends AnyVal {

    def hasServer(onBackend: String, ip: String, port: Int): Predicate = {
      self.toPredicate(s"${self.label} has server $ip:$port on backend $onBackend?") {
        case e@EntryPointConfiguration(_, _, _, _, _, backends, _) =>
          backends.find { b => b.id == onBackend && b.servers.exists { s => s.ip == ip && s.port.toInt == port } }
            .map(_ => Result.success)
            .getOrElse(Result.failure(s"cannot find server $ip:$port on backend $onBackend for configuration ${e.toString}"))
      }
    }

  }

  def readResource(path: String): String = {

    val buffer = new ByteOutputStream()
    val in = Thread.currentThread().getContextClassLoader.getResourceAsStream(path)
    val tmp = Array.ofDim[Byte](2048)

    while (in.available() > 0) {
      val nb = in.read(tmp)
      buffer.write(tmp, 0, nb)
    }

    new String(buffer.getBytes, "utf-8")
  }

  implicit class TopicInformationExecution(val self: Execution[TopicInformation]) extends AnyVal {

    def hasServer(hostname: String): Predicate = self.toPredicate(s"${self.label} has server $hostname?") {
      case configuration if Option(configuration.nodes).getOrElse(List()).exists(_.hostname == hostname) => Result.success
      case configuration => Result.failure(
        s"$hostname wasn't found on topic, found: ${Option(configuration.nodes).getOrElse(List()).map(_.hostname).mkString(",")}"
      )
    }

    def hasChannel(name: String): Predicate = self.toPredicate(s"${self.label} has channel $name?") {
      case configuration if Option(configuration.channels).getOrElse(List()).exists(_.channelName == name) => Result.success
      case configuration => Result.failure(
        s"channel with $name wasn't found on topic, found: ${Option(configuration.channels).getOrElse(List()).map(_.channelName).mkString(",")}"
      )
    }

  }

}
