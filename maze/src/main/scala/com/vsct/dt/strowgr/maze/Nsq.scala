package com.vsct.dt.strowgr.maze

import com.fasterxml.jackson.annotation.JsonProperty
import com.github.dockerjava.api.command.CreateContainerCmd
import fr.vsct.dt.maze.core.Execution
import fr.vsct.dt.maze.helpers.Http.{HttpEnabled, HttpResponse}
import fr.vsct.dt.maze.topology.SingleContainerClusterNode
import fr.vsct.dt.maze.core.Predef._

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

  class NsqAdmin(lookup: String) extends SingleContainerClusterNode with HttpEnabled {
    override def serviceContainer: CreateContainerCmd = image.withCmd("/nsqadmin", s"-http-address=$hostname:4171", s"--lookupd-http-address=$lookup:4161")

    override def servicePort: Int = 4171

    def topicInfo(topicName: String): Execution[TopicInformation] = {
      httpGet(s"/api/topics/$topicName").responseAs(classOf[TopicInformation])
    }
  }


  class NsqTail(lookup: String, topic: String) extends   SingleContainerClusterNode {
    override def serviceContainer: CreateContainerCmd = image.withCmd("/nsq_tail", s"--lookupd-http-address=$lookup:4161", s"--channel=$hostname", s"--topic=$topic")
    override def servicePort: Int = 1234
  }
}

case class TopicInformation(
                             @JsonProperty("node") node: String,
                             @JsonProperty("hostname") hostname: String,
                             @JsonProperty("topic_name") topicName: String,
                             @JsonProperty("depth") depth: Int,
                             @JsonProperty("memory_depth") memoryDepth: Int,
                             @JsonProperty("backend_depth") backendDepth: Int,
                             @JsonProperty("message_count") messageCount: Int,
                             @JsonProperty("paused") paused: Boolean,
                             @JsonProperty("message") message: String,
                             @JsonProperty("nodes") nodes: List[TopicInformation],
                             @JsonProperty("channels") channels: List[TopicInformationChannel],
                             @JsonProperty("e2e_processing_latency") e2eProcessingLatency: LatencyInformation
                           )

case class TopicInformationChannel(
                                    @JsonProperty("node") node: String,
                                    @JsonProperty("hostname") hostname: String,
                                    @JsonProperty("topic_name") topicName: String,
                                    @JsonProperty("channel_name") channelName: String,
                                    @JsonProperty("depth") depth: Int,
                                    @JsonProperty("memory_depth") memoryDepth: Int,
                                    @JsonProperty("backend_depth") backendDepth: Int,
                                    @JsonProperty("in_flight_count") inFlightCount: Int,
                                    @JsonProperty("deferred_count") deferredCount: Int,
                                    @JsonProperty("requeue_count") requeueCount: Int,
                                    @JsonProperty("timeout_count") timeoutCount: Int,
                                    @JsonProperty("message_count") messageCount: Int,
                                    @JsonProperty("nodes") nodes: List[TopicInformationChannelNode],
                                    @JsonProperty("clients") clients: List[TopicInformationClient],
                                    @JsonProperty("paused") paused: Boolean,
                                    @JsonProperty("e2e_processing_latency") e2eProcessingLatency: LatencyInformation
                                  )

case class LatencyInformation(count: Int, percentiles: String, topic: String, channel: String, host: String)

case class TopicInformationClient(
                                   @JsonProperty("node") node: String,
                                   @JsonProperty("remote_address") remoteAddress: String,
                                   @JsonProperty("name") name: String,
                                   @JsonProperty("version") version: String,
                                   @JsonProperty("client_id") clientId: String,
                                   @JsonProperty("hostname") hostname: String,
                                   @JsonProperty("user_agent") userAgent: String,
                                   @JsonProperty("connect_ts") connectTs: Long,
                                   @JsonProperty("connected") connected: Long,
                                   @JsonProperty("in_flight_count") inFlightCount: Int,
                                   @JsonProperty("ready_count") readyCount: Int,
                                   @JsonProperty("finish_count") finishCount: Int,
                                   @JsonProperty("requeue_count") requeueCount: Int,
                                   @JsonProperty("message_count") messageCount: Int,
                                   @JsonProperty("sample_rate") sampleRate: Int,
                                   @JsonProperty("deflate") deflate: Boolean,
                                   @JsonProperty("snappy") snappy: Boolean,
                                   @JsonProperty("authed") authed: Boolean,
                                   @JsonProperty("auth_identity") authIdentity: String,
                                   @JsonProperty("auth_identity_url") authIdentityUrl: String,
                                   @JsonProperty("tls") tls: Boolean,
                                   @JsonProperty("tls_cipher_suite") tlsCipherSuite: String,
                                   @JsonProperty("tls_version") tlsVersion: String,
                                   @JsonProperty("tls_negotiated_protocol") tlsNegotiatedProtocol: String,
                                   @JsonProperty("tls_negotiated_protocol_is_mutual") tlsNegotiatedProtocolIsMutual: Boolean
                                 )

case class TopicInformationChannelNode(
                                        @JsonProperty("node") node: String,
                                        @JsonProperty("hostname") hostname: String,
                                        @JsonProperty("topic_name") topicName: String,
                                        @JsonProperty("channel_name") channelName: String,
                                        @JsonProperty("depth") depth: Int,
                                        @JsonProperty("memory_depth") memoryDepth: Int,
                                        @JsonProperty("backend_depth") backendDepth: Int,
                                        @JsonProperty("in_flight_count") inFlightCount: Int,
                                        @JsonProperty("deferred_count") deferredCount: Int,
                                        @JsonProperty("requeue_count") requeueCount: Int,
                                        @JsonProperty("timeout_count") timeoutCount: Int,
                                        @JsonProperty("message_count") messageCount: Int,
                                        @JsonProperty("paused") paused: Boolean,
                                        @JsonProperty("nodes") nodes: List[TopicInformationChannelNode],
                                        @JsonProperty("clients") clients: List[TopicInformationClient],
                                        @JsonProperty("e2e_processing_latency") e2eProcessingLatency: LatencyInformation
                                      )