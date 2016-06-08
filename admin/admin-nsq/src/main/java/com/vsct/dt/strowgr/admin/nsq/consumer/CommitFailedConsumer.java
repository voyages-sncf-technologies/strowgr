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
import com.vsct.dt.strowgr.admin.core.event.in.CommitFailureEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Arrays;
import java.util.function.Consumer;

public class CommitFailedConsumer {
    private static final Logger LOGGER = LoggerFactory.getLogger(CommitFailedConsumer.class);

    private static final String CHANNEL = "admin";
    private final NSQConsumer nsqConsumer;
    private final ObjectMapper mapper = new ObjectMapper();

    public CommitFailedConsumer(String topic, NSQLookup lookup, String haproxy, Consumer<CommitFailureEvent> consumer) {
        nsqConsumer = new NSQConsumer(lookup, topic + haproxy, CHANNEL, (message) -> {

            CommitFailedPayload payload = null;
            try {
                payload = mapper.readValue(message.getMessage(), CommitFailedPayload.class);
            } catch (IOException e) {
                LOGGER.error("can't deserialize the payload:" + Arrays.toString(message.getMessage()), e);
                //Avoid republishing message and stop processing
                message.finished();
                return;
            }

            CommitFailureEvent event = new CommitFailureEvent(payload.getCorrelationId(), new EntryPointKeyVsctImpl(payload.getApplication(), payload.getPlatform()));
            consumer.accept(event);
            message.finished();
        });
    }

    public void start() {
        nsqConsumer.start();
    }

    public void stop() {
        nsqConsumer.shutdown();
    }

}
