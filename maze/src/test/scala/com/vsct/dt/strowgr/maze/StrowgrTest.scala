package com.vsct.dt.strowgr.maze

import com.typesafe.scalalogging.StrictLogging
import com.vsct.dt.strowgr.maze.Ambassador.AmbassadorNode
import com.vsct.dt.strowgr.maze.Backend.BackendApp
import com.vsct.dt.strowgr.maze.Consul.ConsulNode
import com.vsct.dt.strowgr.maze.Nsq.{NsqAdmin, NsqLookup, NsqTail, Nsqd}
import com.vsct.dt.strowgr.maze.Sidekick.SidekickNode
import com.vsct.dt.strowgr.maze.Strowgr.{AdminNode, CreateEntryPoint}
import fr.vsct.dt.maze.TechnicalTest
import fr.vsct.dt.maze.core.Commands
import fr.vsct.dt.maze.core.Commands.{exec, print, waitFor, waitUntil}
import fr.vsct.dt.maze.core.Predef._
import fr.vsct.dt.maze.topology.DockerClusterNode

import scala.concurrent.duration._
import scala.language.postfixOps
import scala.util.Try

class StrowgrTest extends TechnicalTest with StrictLogging {

  var consulNode: ConsulNode = _
  var lookupNode: NsqLookup = _
  var sidekickNsq: Nsqd = _
  var sidekickSlaveNsq: Nsqd = _
  var adminNsq: Nsqd = _
  var backendNsq: Nsqd = _
  var nsqUi: NsqAdmin = _
  var strowgrAdmin: AdminNode = _
  var sidekick: SidekickNode = _
  var sidekickSlave: SidekickNode = _
  var backend: BackendApp = _

  var extraContainers: List[DockerClusterNode] = _

  override protected def beforeEach(): Unit = {
    extraContainers = List()

    logger.info("Creating strowgr infrastructure...")
    val start = System.currentTimeMillis()

    consulNode = 1.node named "consul" constructedLike new ConsulNode buildSingle()
    lookupNode = 1.node named "nsqlookup" constructedLike new NsqLookup buildSingle()
    adminNsq = 1.node named "adminNsq" constructedLike new Nsqd(lookupNode.hostname) buildSingle()
    sidekickNsq = 1.node named "sidekickNsq" constructedLike new Nsqd(lookupNode.hostname) buildSingle()
    sidekickSlaveNsq = 1.node named "sidekickSlaveNsq" constructedLike new Nsqd(lookupNode.hostname) buildSingle()
    backendNsq = 1.node named "backendNsq" constructedLike new Nsqd(lookupNode.hostname) buildSingle()
    nsqUi = 1.node named "nsqUi" constructedLike new NsqAdmin(lookupNode.hostname) buildSingle()
    sidekick = 1.node named "sidekick" constructedLike new SidekickNode(lookupNode.hostname, sidekickNsq.hostname) buildSingle()
    sidekickSlave = 1.node named "sidekick" constructedLike new SidekickNode(lookupNode.hostname, sidekickSlaveNsq.hostname) buildSingle()
    strowgrAdmin = 1.node named "strowgrAdmin" constructedLike new AdminNode(consulNode.hostname, lookupNode.hostname, adminNsq.hostname) buildSingle()
    backend = 1.node named "backend" constructedLike new BackendApp(backendNsq.hostname) buildSingle()

    consulNode.start()
    lookupNode.start()
    waitUntil(consulNode.isReady()) butNoLongerThan (10 seconds)
    consulNode.init(sidekick.hostname)

    adminNsq.start()
    sidekickNsq.start()
    sidekickSlaveNsq.start()
    backendNsq.start()
    nsqUi.start()

    // Create required topics
    waitUntil(backendNsq.httpPost("/topic/create?topic=register_server", "", "text/plain") isOk) butNoLongerThan (5 seconds)
    waitUntil(adminNsq.httpPost("/topic/create?topic=delete_requested_preproduction", "", "text/plain") isOk) butNoLongerThan (5 seconds)
    waitUntil(adminNsq.httpPost("/topic/create?topic=commit_requested_preproduction", "", "text/plain") isOk) butNoLongerThan (5 seconds)
    waitUntil(sidekickNsq.httpPost("/topic/create?topic=commit_completed_preproduction", "", "text/plain") isOk) butNoLongerThan (5 seconds)
    waitUntil(sidekickNsq.httpPost("/topic/create?topic=commit_failed_preproduction", "", "text/plain") isOk) butNoLongerThan (5 seconds)
    waitUntil(sidekickSlaveNsq.httpPost("/topic/create?topic=commit_slave_completed_preproduction", "", "text/plain") isOk) butNoLongerThan (5 seconds)

    waitUntil(nsqUi.topicInfo("register_server").hasServer(backendNsq.hostname)) butNoLongerThan (10 seconds)
    waitUntil(nsqUi.topicInfo("delete_requested_preproduction").hasServer(adminNsq.hostname)) butNoLongerThan (10 seconds)
    waitUntil(nsqUi.topicInfo("commit_requested_preproduction").hasServer(adminNsq.hostname)) butNoLongerThan (10 seconds)
    waitUntil(nsqUi.topicInfo("commit_completed_preproduction").hasServer(sidekickNsq.hostname)) butNoLongerThan (10 seconds)
    waitUntil(nsqUi.topicInfo("commit_failed_preproduction").hasServer(sidekickNsq.hostname)) butNoLongerThan (10 seconds)
    waitUntil(nsqUi.topicInfo("commit_slave_completed_preproduction").hasServer(sidekickSlaveNsq.hostname)) butNoLongerThan (10 seconds)

    backend.start()
    strowgrAdmin.start()
    sidekick.start()
    sidekickSlave.start()

    waitUntil(strowgrAdmin.isReady()) butNoLongerThan (30 seconds)

    // Ensure all required channels are created
    waitUntil(nsqUi.topicInfo("register_server").hasChannel("admin")) butNoLongerThan (30 seconds)
    waitUntil(nsqUi.topicInfo("commit_failed_preproduction").hasChannel("admin")) butNoLongerThan (30 seconds)
    waitUntil(nsqUi.topicInfo("commit_failed_preproduction").hasChannel(sidekick.hostname)) butNoLongerThan (30 seconds)
    waitUntil(nsqUi.topicInfo("commit_completed_preproduction").hasChannel("admin")) butNoLongerThan (30 seconds)
    waitUntil(nsqUi.topicInfo("commit_completed_preproduction").hasChannel(sidekick.hostname)) butNoLongerThan (30 seconds)
    waitUntil(nsqUi.topicInfo("commit_completed_preproduction").hasChannel(sidekickSlave.hostname)) butNoLongerThan (30 seconds)
    waitUntil(nsqUi.topicInfo("commit_requested_preproduction").hasChannel(sidekick.hostname)) butNoLongerThan (30 seconds)
    waitUntil(nsqUi.topicInfo("commit_requested_preproduction").hasChannel(sidekickSlave.hostname)) butNoLongerThan (30 seconds)
    waitUntil(nsqUi.topicInfo("commit_slave_completed_preproduction").hasChannel(sidekick.hostname)) butNoLongerThan (30 seconds)
    waitUntil(nsqUi.topicInfo("commit_slave_completed_preproduction").hasChannel(sidekickSlave.hostname)) butNoLongerThan (30 seconds)
    waitUntil(nsqUi.topicInfo("delete_requested_preproduction").hasChannel(sidekick.hostname)) butNoLongerThan (30 seconds)
    waitUntil(nsqUi.topicInfo("delete_requested_preproduction").hasChannel(sidekickSlave.hostname)) butNoLongerThan (30 seconds)

    logger.info(s"Done creating all in ${System.currentTimeMillis() - start}ms, test can start.")
  }

  // Comment / uncomment the requested lines to run or ignore the test
  "a strowgr architecture" should "start normally" in {
    exec(backend.shellExecution("/bin/mkdir", "-p", "/usr/share/nginx/html/haproxy"))
    backend.createFile("/usr/share/nginx/html/haproxy/default.conf", readResource("haproxy/default.conf"))

    logger.info("Creating endpoint TEST/TEST...")
    print(strowgrAdmin.createEntrypoint(CreateEntryPoint(
      context = Map("application" -> "TEST", "platform" -> "TEST", "templateUri" -> s"http://${backend.hostname}/haproxy/default.conf")
    )))

    waitUntil(consulNode.portOfFrontend("TEST", "TEST", "FRONTEND").isSuccess) butNoLongerThan (30 seconds)
    val port: Int = exec(consulNode.portOfFrontend("TEST", "TEST", "FRONTEND"))

    var ambassador: AmbassadorNode = 1.node named "ambassador" constructedLike new AmbassadorNode(sidekick.internalIp, port) buildSingle()
    extraContainers = ambassador :: extraContainers
    ambassador.start()
    waitUntil(ambassador.httpGet("/stats").status is 200) butNoLongerThan (30 seconds)

    logger.info("Entrypoint TEST/TEST successfully created.")

    logger.info("Register first server")
    exec(backendNsq.registerServer("TEST", "TEST", backend.hostname, backend.ip, backend.servicePort, "BACKEND"))

    val registerDuration = waitUntil(
      strowgrAdmin.currentConfiguration("TEST", "TEST").get.hasServer(
        ip = backend.ip,
        port = backend.servicePort,
        onBackend = "BACKEND")
    ) butNoLongerThan (20 minutes)

    logger.info("Register second server")
    exec(backendNsq.registerServer("TEST", "TEST", backend.hostname, backend.ip, 443, "BACKEND"))

    val registerDuration2 = waitUntil(
      strowgrAdmin.currentConfiguration("TEST", "TEST").get.hasServer(
        ip = backend.ip,
        port = 443,
        onBackend = "BACKEND")
    ) butNoLongerThan (20 minutes)

    logger.info(s"register 1 took ${registerDuration.toMillis}ms")
    logger.info(s"register 2 took ${registerDuration2.toMillis}ms")

    print(strowgrAdmin.createEntrypoint(CreateEntryPoint(
      context = Map("application" -> "TEST", "platform" -> "TEST2", "templateUri" -> s"http://${backend.hostname}/haproxy/default.conf")
    )))

    waitUntil(consulNode.portOfFrontend("TEST", "TEST2", "FRONTEND").isSuccess) butNoLongerThan (30 seconds)
    val port2: Int = exec(consulNode.portOfFrontend("TEST", "TEST2", "FRONTEND"))

    ambassador = 1.node named "ambassador" constructedLike new AmbassadorNode(sidekick.internalIp, port2) buildSingle()
    extraContainers = ambassador :: extraContainers
    ambassador.start()
    val createEndpointDelay2 = waitUntil(ambassador.httpGet("/stats").status is 200) butNoLongerThan (30 seconds)
    logger.info(s"Create endpoint TEST2 took ${createEndpointDelay2.toMillis}ms")

    logger.info("rock and roll")
  }

  "configuration" should "not be committed if it's not valid" in {
    exec(backend.shellExecution("/bin/mkdir", "-p", "/usr/share/nginx/html/haproxy"))
    waitFor(2 seconds)
    backend.createFile("/usr/share/nginx/html/haproxy/notvalid.conf", readResource("haproxy/notvalid.conf"))

    val nsqTail = 1.node named "nsqTail" constructedLike new NsqTail(lookupNode.hostname, "commit_failed_preproduction") buildSingle()
    extraContainers = nsqTail :: extraContainers
    nsqTail.start()

    waitFor(3 seconds)

    print(strowgrAdmin.createEntrypoint(CreateEntryPoint(
      context = Map("application" -> "TEST", "platform" -> "TEST", "templateUri" -> s"http://${backend.hostname}/haproxy/notvalid.conf")
    )))

    waitUntil(
        nsqTail.logs.map(_.filter(_.startsWith("""{"header":"""))).length > 0
    ) butNoLongerThan(10 seconds)

    print(nsqTail.logs)

  }

  override protected def afterEach(): Unit = {
    strowgrAdmin.clear()
    sidekick.clear()
    sidekickSlave.clear()
    nsqUi.clear()
    adminNsq.clear()
    sidekickNsq.clear()
    sidekickSlaveNsq.clear()
    backendNsq.clear()
    lookupNode.clear()
    consulNode.clear()
    backend.clear()

    extraContainers.foreach { c => Try(c.clear()) }
  }
}
