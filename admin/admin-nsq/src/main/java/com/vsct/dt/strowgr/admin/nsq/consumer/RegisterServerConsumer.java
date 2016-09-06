/*
 * Copyright (C) 2016 VSCT
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package com.vsct.dt.strowgr.admin.nsq.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.brainlag.nsq.NSQConsumer;
import com.github.brainlag.nsq.lookup.NSQLookup;
import com.google.common.collect.Sets;
import com.vsct.dt.strowgr.admin.core.configuration.IncomingEntryPointBackendServer;
import com.vsct.dt.strowgr.admin.core.event.in.RegisterServerEvent;
import com.vsct.dt.strowgr.admin.nsq.payload.RegisterServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rx.Observable;
import rx.exceptions.Exceptions;

import java.io.IOException;
import java.util.Arrays;
import java.util.function.Consumer;

public class RegisterServerConsumer {
    private static final Logger LOGGER = LoggerFactory.getLogger(RegisterServerConsumer.class);

    private static final String CHANNEL = "admin";
    private static final String TOPIC = "register_server";

    private final ObservableNSQConsumer nsqConsumer;
    private final Observable<RegisterServerEvent> observable;

    public RegisterServerConsumer(NSQLookup lookup, ObjectMapper mapper) {
        nsqConsumer = new ObservableNSQConsumer(lookup, TOPIC, CHANNEL);
        observable = nsqConsumer.observe().map(nsqMessage -> {
            try {
                RegisterServer payload = mapper.readValue(nsqMessage.getMessage(), RegisterServer.class);
                if (payload.getHeader().getCorrelationId() == null) {
                    payload.getHeader().setCorrelationId(Arrays.toString(nsqMessage.getId()));
                }
                if (payload.getHeader().getTimestamp() == null) {
                    payload.getHeader().setTimestamp(nsqMessage.getTimestamp().getTime());
                }

                return new RegisterServerEvent(payload.getHeader().getCorrelationId(),
                        new EntryPointKeyVsctImpl(payload.getHeader().getApplication(), payload.getHeader().getPlatform()),
                        payload.getServer().getBackendId(),
                        Sets.newHashSet(new IncomingEntryPointBackendServer(
                                payload.getServer().getId(),
                                payload.getServer().getIp(),
                                payload.getServer().getPort(),
                                payload.getServer().getContext()
                        )));
            } catch (IOException e) {
                LOGGER.error("can't deserialize the payload of message at " + nsqMessage.getTimestamp() + ", id=" + Arrays.toString(nsqMessage.getId()) + ": " + Arrays.toString(nsqMessage.getMessage()), e);
                throw Exceptions.propagate(e);
            } finally {
                nsqMessage.finished();
            }
        });
    }

    public Observable<RegisterServerEvent> observe(){
        return observable;
    }

    public void shutdown() {
        nsqConsumer.shutdown();
    }

}
