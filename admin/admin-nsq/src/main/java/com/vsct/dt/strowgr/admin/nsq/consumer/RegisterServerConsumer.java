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

package com.vsct.dt.strowgr.admin.nsq.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.brainlag.nsq.NSQConsumer;
import com.github.brainlag.nsq.lookup.NSQLookup;
import com.google.common.collect.Sets;
import com.vsct.dt.strowgr.admin.core.configuration.IncomingEntryPointBackendServer;
import com.vsct.dt.strowgr.admin.core.event.in.RegisterServerEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Arrays;
import java.util.function.Consumer;

public class RegisterServerConsumer {
    private static final Logger LOGGER = LoggerFactory.getLogger(RegisterServerConsumer.class);

    private static final String CHANNEL = "admin";
    private final NSQConsumer registerServerConsumer;
    private final ObjectMapper mapper = new ObjectMapper();

    public RegisterServerConsumer(String topic, NSQLookup lookup, Consumer<RegisterServerEvent> consumer) {

        registerServerConsumer = new NSQConsumer(lookup, topic, CHANNEL, (message) -> {

            RegisterServerPayload payload = null;
            try {
                payload = mapper.readValue(message.getMessage(), RegisterServerPayload.class);
                if (payload.getCorrelationId() == null) {
                    payload.setCorrelationId(Arrays.toString(message.getId()));
                }
                if (payload.getTimestamp() == null) {
                    payload.setTimestamp(message.getTimestamp().getTime());
                }
            } catch (IOException e) {
                LOGGER.error("can't deserialize the payload of message at " + message.getTimestamp() + ", id=" + Arrays.toString(message.getId()) + ": " + Arrays.toString(message.getMessage()), e);
                //Avoid republishing message and stop processing
                message.finished();
                return;
            }

            /* TODO Use some conflation to prevent dispatching all event */
            RegisterServerEvent event = new RegisterServerEvent(payload.getCorrelationId(),
                    new EntryPointKeyVsctImpl(payload.getApplication(), payload.getPlatform()),
                    payload.getBackend(),
                    Sets.newHashSet(new IncomingEntryPointBackendServer(payload.getId(), payload.getHostname(), payload.getIp(), payload.getPort(), payload.getContext())));

            consumer.accept(event);

            message.finished();
        });

    }

    public void start() {
        registerServerConsumer.start();
    }

    public void stop() {
        registerServerConsumer.shutdown();
    }

}
