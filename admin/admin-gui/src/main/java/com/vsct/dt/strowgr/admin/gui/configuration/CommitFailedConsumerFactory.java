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
import com.github.brainlag.nsq.lookup.NSQLookup;
import com.vsct.dt.strowgr.admin.core.event.in.CommitFailureEvent;
import com.vsct.dt.strowgr.admin.nsq.consumer.CommitFailedConsumer;
import io.dropwizard.lifecycle.Managed;
import io.dropwizard.setup.Environment;
import org.hibernate.validator.constraints.NotEmpty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Consumer;

/**
 * Created by william_montaz on 16/02/2016.
 */
public class CommitFailedConsumerFactory {

    private static final Logger LOGGER = LoggerFactory.getLogger(CommitFailedConsumerFactory.class);

    @NotEmpty
    private String topic;

    @JsonProperty("topic")
    public String getTopic() {
        return topic;
    }

    @JsonProperty("topic")
    public void setTopic(String topic) {
        this.topic = topic;
    }

    public CommitFailedConsumer build(NSQLookup lookup, String haproxy, Consumer<CommitFailureEvent> consumer, Environment environment) {
        CommitFailedConsumer commitFailedConsumer = new CommitFailedConsumer(getTopic(), lookup, haproxy, consumer);
        environment.lifecycle().manage(new Managed() {
            @Override
            public void start() throws Exception {
                LOGGER.info("Starting CommitFailedConsumer");
                commitFailedConsumer.start();
            }

            @Override
            public void stop() throws Exception {
                LOGGER.info("Stopping CommitFailedConsumer");
                commitFailedConsumer.stop();
            }
        });
        return commitFailedConsumer;
    }

}
