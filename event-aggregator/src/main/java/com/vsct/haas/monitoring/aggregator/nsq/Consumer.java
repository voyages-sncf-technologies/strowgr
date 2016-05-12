package com.vsct.haas.monitoring.aggregator.nsq;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.brainlag.nsq.NSQConsumer;
import com.github.brainlag.nsq.callbacks.NSQMessageCallback;
import com.github.brainlag.nsq.lookup.NSQLookup;
import com.vsct.haas.monitoring.aggregator.MessageRecorder;
import com.vsct.haas.monitoring.aggregator.cassandra.ParsedPayload;
import com.vsct.haas.monitoring.aggregator.cassandra.ErrorRecord;
import com.vsct.haas.monitoring.aggregator.cassandra.ParsedPayloadWriter;
import com.vsct.haas.monitoring.aggregator.cassandra.ErrorRecordWriter;
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
