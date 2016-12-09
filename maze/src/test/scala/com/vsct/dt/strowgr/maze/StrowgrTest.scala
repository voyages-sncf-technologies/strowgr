package com.vsct.dt.strowgr.maze

import com.typesafe.scalalogging.StrictLogging
import com.vsct.dt.strowgr.maze.Ambassador.AmbassadorNode
import com.vsct.dt.strowgr.maze.Backend.BackendApp
import com.vsct.dt.strowgr.maze.Consul.ConsulNode
import com.vsct.dt.strowgr.maze.Nsq.{NsqAdmin, NsqLookup, Nsqd}
import com.vsct.dt.strowgr.maze.Sidekick.SidekickNode
import com.vsct.dt.strowgr.maze.Strowgr.{AdminNode, CreateEntryPoint}
import fr.vsct.dt.maze.TechnicalTest
import fr.vsct.dt.maze.core.Commands.{exec, print, waitFor, waitUntil}
import fr.vsct.dt.maze.core.Predef._

import scala.concurrent.duration._
import scala.language.postfixOps

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

  override protected def beforeEach(): Unit = {
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
    consulNode.init()

    adminNsq.start()
    sidekickNsq.start()
    sidekickSlaveNsq.start()
    backendNsq.start()
    nsqUi.start()

    waitFor(1 seconds)

    // Create required topics
    exec(backendNsq.httpPost("/topic/create?topic=register_server", "", "text/plain"))
    exec(adminNsq.httpPost("/topic/create?topic=delete_requested_preproduction", "", "text/plain"))
    exec(adminNsq.httpPost("/topic/create?topic=commit_requested_preproduction", "", "text/plain"))
    exec(sidekickNsq.httpPost("/topic/create?topic=commit_completed_preproduction", "", "text/plain"))
    exec(sidekickNsq.httpPost("/topic/create?topic=commit_failed_preproduction", "", "text/plain"))
    exec(sidekickSlaveNsq.httpPost("/topic/create?topic=commit_slave_completed_preproduction", "", "text/plain"))

    backend.start()
    strowgrAdmin.start()
    sidekick.start()
    sidekickSlave.start()

    waitUntil(strowgrAdmin.isReady()) butNoLongerThan (30 seconds)

    exec(strowgrAdmin.createHap("preproduction", "preproduction", sidekick.hostname, "preproduction"))

    logger.info(s"Done creating all in ${System.currentTimeMillis() - start}ms, test can start.")
  }

  // Comment / uncomment the requested lines to run or ignore the test
  "a strowgr architecture" should "start normally" in {
//  ignore should "start normally" in {

    exec(backend.shellExecution("/bin/mkdir", "-p", "/usr/share/nginx/html/haproxy"))
    backend.createFile("/usr/share/nginx/html/haproxy/default.conf", readResource("haproxy/default.conf"))

    print(strowgrAdmin.createEntrypoint(CreateEntryPoint(
      context = Map("application" -> "TEST", "platform" -> "TEST", "templateUri" -> s"http://${backend.hostname}/haproxy/default.conf")
    )))

    waitUntil(consulNode.portOfFrontend("TEST", "TEST", "FRONTEND").isSuccess) butNoLongerThan (10 seconds)
    val port: Int = exec(consulNode.portOfFrontend("TEST", "TEST", "FRONTEND"))

    var ambassador: AmbassadorNode = 1.node named "ambassador" constructedLike new AmbassadorNode(sidekick.internalIp, port) buildSingle()
    ambassador.start()
    waitUntil(ambassador.httpGet("/stats").status is 200) butNoLongerThan (10 seconds)
    ambassador.clear()

    exec(backendNsq.registerServer("TEST", "TEST", backend.hostname, backend.ip, backend.servicePort, "BACKEND"))

    val registerDuration = waitUntil(
      strowgrAdmin.currentConfiguration("TEST", "TEST").get.hasServer(
        ip = backend.ip,
        port = backend.servicePort,
        onBackend = "BACKEND")
    ) butNoLongerThan (2 minutes)

    exec(backendNsq.registerServer("TEST", "TEST", backend.hostname, backend.ip, 443, "BACKEND"))

    val registerDuration2 = waitUntil(
      strowgrAdmin.currentConfiguration("TEST", "TEST").get.hasServer(
        ip = backend.ip,
        port = 443,
        onBackend = "BACKEND")
    ) butNoLongerThan (2 minutes)

    logger.info(s"register 1 took ${registerDuration.toMillis}ms")
    logger.info(s"register 2 took ${registerDuration2.toMillis}ms")

    print(strowgrAdmin.createEntrypoint(CreateEntryPoint(
      context = Map("application" -> "TEST", "platform" -> "TEST2", "templateUri" -> s"http://${backend.hostname}/haproxy/default.conf")
    )))

    waitUntil(consulNode.portOfFrontend("TEST", "TEST2", "FRONTEND").isSuccess) butNoLongerThan (10 seconds)
    val port2: Int = exec(consulNode.portOfFrontend("TEST", "TEST2", "FRONTEND"))

    ambassador = 1.node named "ambassador" constructedLike new AmbassadorNode(sidekick.internalIp, port2) buildSingle()
    ambassador.start()
    val createEndpointDelay2 = waitUntil(ambassador.httpGet("/stats").status is 200) butNoLongerThan (10 seconds)
    ambassador.clear()
    logger.info(s"Create endpoint TEST2 took ${createEndpointDelay2.toMillis}ms")

    logger.info("rock and roll")
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
  }
}
