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

package com.vsct.dt.strowgr.admin.gui.configuration;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.vsct.dt.strowgr.admin.repository.consul.ConsulRepository;
import io.dropwizard.lifecycle.Managed;
import io.dropwizard.setup.Environment;
import org.hibernate.validator.constraints.NotEmpty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;

/**
 * Consul configuration.
 * <p>
 * Created by william_montaz on 15/02/2016.
 */
public class ConsulRepositoryFactory {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConsulRepositoryFactory.class);

    @NotEmpty
    private String host = "localhost";

    @Min(1)
    @Max(65535)
    private int port = 8500;

    @JsonProperty(value = "minGeneratedPort")
    private int minGeneratedPort = 32000;

    @JsonProperty("maxGeneratedPort")
    private int maxGeneratedPort = 64000;

    @JsonProperty(defaultValue = "localhost")
    public String getHost() {
        return host;
    }

    @JsonProperty
    public void setHost(String host) {
        this.host = host;
    }

    @JsonProperty
    public int getPort() {
        return port;
    }

    @JsonProperty
    public void setPort(int port) {
        this.port = port;
    }

    @JsonProperty
    public int getMinGeneratedPort() {
        return minGeneratedPort;
    }

    @JsonProperty
    public void setMinGeneratedPort(int minGeneratedPort) {
        this.minGeneratedPort = minGeneratedPort;
    }

    @JsonProperty
    public int getMaxGeneratedPort() {
        return maxGeneratedPort;
    }

    @JsonProperty
    public void setMaxGeneratedPort(int maxGeneratedPort) {
        this.maxGeneratedPort = maxGeneratedPort;
    }

    /**
     * Build a consul repository from configuration file.
     *
     * @return Built Consul repository from configuration
     */
    public ConsulRepository build() {
        return new ConsulRepository(getHost(), getPort(), getMinGeneratedPort(), getMaxGeneratedPort());
    }

    /**
     * Build a consul repository  and subscribe to dropwizard environment.
     *
     * @param environment dropwizard which will manage lifecycle of the
     * @return Built Consul repository from configuration
     */
    public ConsulRepository buildAndManageBy(Environment environment) {
        ConsulRepository repository = build();
        environment.lifecycle().manage(new Managed() {
            @Override
            public void start() throws Exception {
            }

            @Override
            public void stop() throws Exception {
                LOGGER.info("Shutting down consul repository client");
                repository.shutdown();
            }
        });
        return repository;
    }
}
