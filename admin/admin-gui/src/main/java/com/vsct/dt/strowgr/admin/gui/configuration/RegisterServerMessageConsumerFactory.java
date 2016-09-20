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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.brainlag.nsq.NSQConsumer;
import com.github.brainlag.nsq.lookup.NSQLookup;
import com.google.common.collect.Sets;
import com.vsct.dt.strowgr.admin.core.configuration.IncomingEntryPointBackendServer;
import com.vsct.dt.strowgr.admin.core.event.in.RegisterServerEvent;
import com.vsct.dt.strowgr.admin.nsq.consumer.EntryPointKeyVsctImpl;
import com.vsct.dt.strowgr.admin.nsq.payload.RegisterServer;
import com.vsct.dt.strowgr.admin.nsq.payload.fragment.Header;
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

    private static final String TOPIC = "register_server";

    private final NSQLookup                     nsqLookup;
    private final ObjectMapper                  objectMapper;
    private final Consumer<RegisterServerEvent> consumer;

    public RegisterServerMessageConsumerFactory(NSQLookup nsqLookup, ObjectMapper objectMapper, Consumer<RegisterServerEvent> consumer) {
        this.nsqLookup = nsqLookup;
        this.objectMapper = objectMapper;
        this.consumer = consumer;
    }

    public NSQConsumer build() {
        return new NSQConsumer(nsqLookup, TOPIC, "admin", (message) -> {

            RegisterServer registerServer = null;
            Header header;
            try {
                registerServer = objectMapper.readValue(message.getMessage(), RegisterServer.class);
                header = registerServer.getHeader();
                if (header.getCorrelationId() == null) {
                    header.setCorrelationId(new String(message.getId()));
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
                //Avoid republishing message and stopLookup processing
                message.finished();
                return;
            }

            message.finished();
        });
    }
}
