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
package com.vsct.strowgr.monitoring.aggregator.nsq;

import com.fasterxml.jackson.databind.ObjectMapper;
import fr.vsct.dt.nsq.NSQConsumer;
import fr.vsct.dt.nsq.callbacks.NSQMessageCallback;
import fr.vsct.dt.nsq.lookup.NSQLookup;
import com.vsct.strowgr.monitoring.aggregator.MessageRecorder;
import com.vsct.strowgr.monitoring.aggregator.cassandra.ErrorRecord;
import com.vsct.strowgr.monitoring.aggregator.cassandra.ParsedPayload;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class Consumer {

    private static final Logger LOGGER = LoggerFactory.getLogger(Consumer.class);

    private static final ObjectMapper mapper = new ObjectMapper();

    private String eventName;
    private String haproxyId;

    private NSQConsumer consumer;

    private MessageRecorder     messageRecorder;

    public Consumer(NSQLookup lookup, String topic, String channel, MessageRecorder messageRecorder) {
        LOGGER.info("Creating NSQConsumer for topic -> " + topic);

        /* By convention, topic is using _ to sperate eventName and haproxyId */
        String[] subs = topic.split("_");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < subs.length - 1; i++) {
            sb.append(subs[i]);
            if (i < subs.length - 2) sb.append("_");
        }

        this.eventName = sb.toString();
        this.haproxyId = subs[subs.length - 1];

        this.consumer = new NSQConsumer(lookup, topic, channel, consumeMessage);

        this.messageRecorder = messageRecorder;
    }

    public void start() {
        consumer.start();
    }

    public void shutdown() {
        consumer.shutdown();
    }

    private NSQMessageCallback consumeMessage = (message) -> {
        messageRecorder.record(() -> {
            String payload = new String(message.getMessage());
            NsqEventHeader header = null;
            try {
                header = mapper.readValue(payload, NsqEventHeader.class);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            return new ParsedPayload(header, haproxyId, eventName, payload);
        }, t -> {
            LOGGER.error("Cannot record message. Reason: " + t.getMessage() + ". " + eventName + " -> " + new String(message.getMessage()));
            return new ErrorRecord(message, t.getMessage());
        });

        message.finished();
    };
};
