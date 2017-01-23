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
import com.vsct.dt.strowgr.admin.repository.consul.ConsulRepository;
import io.dropwizard.cli.ConfiguredCommand;
import io.dropwizard.setup.Bootstrap;
import net.sourceforge.argparse4j.inf.Namespace;
import net.sourceforge.argparse4j.inf.Subparser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class InitializationCommand extends ConfiguredCommand<StrowgrConfiguration> {
    private static final Logger LOGGER = LoggerFactory.getLogger(InitializationCommand.class);

    public InitializationCommand() {
        super("init", "initialize strowgr admin partners (consul, nsq, etc...)");
    }


    @Override
    public void configure(Subparser subparser) {
        super.configure(subparser);
        subparser.addArgument("-r", "--repository")
                .dest("repository")
                .type(Boolean.class)
                .required(false)
                .setDefault("false")
                .help("initialize repository (consul)");

    }

    @Override
    protected void run(Bootstrap bootstrap, Namespace namespace, StrowgrConfiguration strowgrConfiguration) throws Exception {
        ConsulRepository consulRepository = strowgrConfiguration.getConsulRepositoryFactory().build();

        // initialize vip of an haproxy cluster
        if (namespace.get("repository") != null && namespace.get("repository") == Boolean.TRUE) {
            LOGGER.info("initialize repository {}", strowgrConfiguration.getConsulRepositoryFactory());
            consulRepository.init();
        }
    }
}
