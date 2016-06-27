/*
 *  Copyright (C) 2016 VSCT
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package com.vsct.dt.strowgr.admin.gui.cli;

import com.vsct.dt.strowgr.admin.gui.configuration.StrowgrConfiguration;
import com.vsct.dt.strowgr.admin.nsq.producer.NSQHttpClient;
import com.vsct.dt.strowgr.admin.repository.consul.ConsulRepository;
import io.dropwizard.cli.ConfiguredCommand;
import io.dropwizard.setup.Bootstrap;
import net.sourceforge.argparse4j.inf.Namespace;
import net.sourceforge.argparse4j.inf.Subparser;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Optional;

public class InitializationCommand extends ConfiguredCommand<StrowgrConfiguration> {
    private static final Logger LOGGER = LoggerFactory.getLogger(InitializationCommand.class);

    public InitializationCommand() {
        super("init", "initialize strowgr admin partners (consul, nsq, etc...)");
    }


    @Override
    public void configure(Subparser subparser) {
        super.configure(subparser);
        subparser.addArgument("-hn", "--haproxy-name")
                .dest("haproxy-name")
                .type(String.class)
                .required(false)
                .setDefault("default-name")
                .help("haproxy name for initialization of topic");

        subparser.addArgument("-v", "--vip")
                .dest("vip")
                .type(String.class)
                .required(false)
                .help("initialize vip for given haproxy by --haproxy-name option");
    }

    @Override
    protected void run(Bootstrap bootstrap, Namespace namespace, StrowgrConfiguration strowgrConfiguration) throws Exception {
        CloseableHttpClient httpClient = HttpClients.createDefault();

        NSQHttpClient nsqHttpClient = new NSQHttpClient("http://" + strowgrConfiguration.getNsqProducerFactory().getHost() + ":" + strowgrConfiguration.getNsqProducerFactory().getHttpPort(), httpClient);
        ConsulRepository consulRepository = strowgrConfiguration.getConsulRepositoryFactory().build();

        // ports
        Optional<Boolean> portsInitialized = consulRepository.initPorts();
        if (portsInitialized.orElse(Boolean.FALSE)) {
            LOGGER.info("key/value for ports is initialized in repository");
        } else {
            LOGGER.warn("key/value for ports can't be initialized (already done?).");
        }

        // initialize haproxy producer queue
        for (String prefix : Arrays.asList("commit_requested_", "delete_requested_")) {
            String topicName = prefix + namespace.getString("haproxy-name");
            if (nsqHttpClient.createTopic(topicName)) {
                LOGGER.info("topic {} has been initialized on nsqd", topicName);
            } else {
                LOGGER.info("topic {} can't be initialized on nsqd", topicName);
            }
        }

        // initialize vip of an haproxy cluster
        if (namespace.get("vip") != null) {
            consulRepository.setHaproxyVip(namespace.get("haproxy-name"), namespace.get("vip"));
        }
    }
}
