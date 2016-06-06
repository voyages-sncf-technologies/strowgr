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
import com.vsct.dt.strowgr.admin.nsq.producer.Producer;
import io.dropwizard.lifecycle.Managed;
import io.dropwizard.setup.Environment;
import org.hibernate.validator.constraints.NotEmpty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;

/**
 * Created by william_montaz on 16/02/2016.
 */
public class NSQProducerFactory {

    private static final Logger LOGGER = LoggerFactory.getLogger(NSQProducerFactory.class);

    @NotEmpty
    private String host;

    @Min(1)
    @Max(65535)
    private int port;

    @NotEmpty
    private String topic;

    @JsonProperty
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
    public String getTopic() {
        return topic;
    }

    @JsonProperty
    public void setTopic(String topic) {
        this.topic = topic;
    }

    public Producer build(Environment environment) {
        Producer producer = new Producer(getHost(), getPort(), getTopic());
        environment.lifecycle().manage(new Managed() {
            @Override
            public void start() throws Exception {
                LOGGER.info("Starting NSQProducer");
                producer.start();
            }

            @Override
            public void stop() throws Exception {
                LOGGER.info("Stopping NSQProducer");
                producer.stop();
            }
        });
        return producer;
    }

}
