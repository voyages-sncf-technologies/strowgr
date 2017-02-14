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
package com.vsct.dt.strowgr.admin.gui.configuration;

import com.fasterxml.jackson.annotation.JsonProperty;
import fr.vsct.dt.nsq.NSQProducer;
import org.apache.log4j.Level;
import org.apache.log4j.LogManager;
import org.hibernate.validator.constraints.NotEmpty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;

/**
 * NSQProducerFactory for reading NSQProducer configuration from dropwizard yaml.
 * <p>
 * Created by william_montaz on 16/02/2016.
 */
public class NSQProducerFactory {

    private static final Logger LOGGER = LoggerFactory.getLogger(NSQProducerFactory.class);

    @NotEmpty
    private String host = "localhost";

    @JsonProperty
    @Min(1)
    @Max(65535)
    private int tcpPort = 4150;

    @JsonProperty
    @Min(1)
    @Max(65535)
    private int httpPort = 4151;

    @JsonProperty
    public String getHost() {
        return host;
    }

    @JsonProperty
    public void setHost(String host) {
        this.host = host;
    }


    public int getTcpPort() {
        return tcpPort;
    }

    public void setTcpPort(int tcpPort) {
        this.tcpPort = tcpPort;
    }

    public int getHttpPort() {
        return httpPort;
    }

    public void setHttpPort(int httpPort) {
        this.httpPort = httpPort;
    }

    public NSQProducer build() {
        NSQProducer nsqProducer = new NSQProducer();
        nsqProducer.addAddress(getHost(), getTcpPort());

        LogManager.getRootLogger().setLevel(Level.ERROR);
        LOGGER.info("read NSQ Producer configuration with host:{}, port: {}", getHost(), getTcpPort());
        return nsqProducer;
    }

}
