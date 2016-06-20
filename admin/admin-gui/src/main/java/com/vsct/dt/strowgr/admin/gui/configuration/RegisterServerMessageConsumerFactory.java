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
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.brainlag.nsq.NSQConsumer;
import com.github.brainlag.nsq.lookup.NSQLookup;
import com.google.common.collect.Sets;
import com.vsct.dt.strowgr.admin.core.configuration.IncomingEntryPointBackendServer;
import com.vsct.dt.strowgr.admin.core.event.in.RegisterServerEvent;
import com.vsct.dt.strowgr.admin.nsq.consumer.EntryPointKeyVsctImpl;
import com.vsct.dt.strowgr.admin.nsq.payload.RegisterServer;
import com.vsct.dt.strowgr.admin.nsq.payload.fragment.Header;
import org.hibernate.validator.constraints.NotEmpty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Arrays;
import java.util.function.Consumer;

/**
 * Configuration factory from Dropwizard for RegisterServerMessageConsumer NSQ.
 */
public class RegisterServerMessageConsumerFactory {

    private static final Logger LOGGER = LoggerFactory.getLogger(RegisterServerMessageConsumerFactory.class);

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

    private final ObjectMapper mapper = new ObjectMapper();

    public NSQConsumer build(NSQLookup lookup, Consumer<RegisterServerEvent> consumer) {
        return new NSQConsumer(lookup, topic, "admin", (message) -> {

            RegisterServer registerServer = null;
            Header header;
            try {
                registerServer = mapper.readValue(message.getMessage(), RegisterServer.class);
                header = registerServer.getHeader();
                if (header.getCorrelationId() == null) {
                    header.setCorrelationId(Arrays.toString(message.getId()));
                }
                if (header.getTimestamp() == null) {
                    header.setTimestamp(message.getTimestamp().getTime());
                }
                // TODO Use some conflation to prevent dispatching all event
                RegisterServerEvent event = new RegisterServerEvent(header.getCorrelationId(),
                        new EntryPointKeyVsctImpl(header.getApplication(), header.getPlatform()),
                        registerServer.getServer().getBackendId(),
                        Sets.newHashSet(new IncomingEntryPointBackendServer(registerServer.getServer().getId(), registerServer.getServer().getIp(), registerServer.getServer().getPort(), registerServer.getServer().getContext())));
                consumer.accept(event);

            } catch (IOException e) {
                LOGGER.error("can't deserialize the registerServer of message at " + message.getTimestamp() + ", id=" + Arrays.toString(message.getId()) + ": " + Arrays.toString(message.getMessage()), e);
                //Avoid republishing message and stop processing
                message.finished();
                return;
            }

            message.finished();
        });
    }
}
