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
package com.vsct.dt.strowgr.admin.nsq.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.brainlag.nsq.NSQMessage;
import com.google.common.collect.Sets;
import com.vsct.dt.strowgr.admin.core.configuration.IncomingEntryPointBackendServer;
import com.vsct.dt.strowgr.admin.core.event.in.RegisterServerEvent;
import com.vsct.dt.strowgr.admin.nsq.payload.RegisterServer;
import io.reactivex.functions.Function;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RegisterServerTransformer implements Function<NSQMessage, RegisterServerEvent> {

    private static final Logger LOGGER = LoggerFactory.getLogger(RegisterServerTransformer.class);

    private final ObjectMapper objectMapper;

    public RegisterServerTransformer(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public RegisterServerEvent apply(NSQMessage nsqMessage) throws Exception {
        RegisterServer payload = objectMapper.readValue(nsqMessage.getMessage(), RegisterServer.class);

        LOGGER.debug("received an new RegisterServerEvent with cid {}", payload.getHeader().getCorrelationId());

        return new RegisterServerEvent(payload.getHeader().getCorrelationId(),
                new EntryPointKeyVsctImpl(payload.getHeader().getApplication(), payload.getHeader().getPlatform()),
                payload.getServer().getBackendId(),
                Sets.newHashSet(new IncomingEntryPointBackendServer(
                        payload.getServer().getId(),
                        payload.getServer().getIp(),
                        payload.getServer().getPort(),
                        payload.getServer().getContext()
                )));
    }
}
