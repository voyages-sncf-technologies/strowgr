package com.vsct.haas.monitoring.aggregator.nsq;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.brainlag.nsq.NSQConsumer;
import com.github.brainlag.nsq.callbacks.NSQMessageCallback;
import com.github.brainlag.nsq.lookup.NSQLookup;
import com.vsct.haas.monitoring.aggregator.cassandra.ParsedPayload;
import com.vsct.haas.monitoring.aggregator.cassandra.ErrorRecord;
import com.vsct.haas.monitoring.aggregator.cassandra.ParsedPayloadWriter;
import com.vsct.haas.monitoring.aggregator.cassandra.ErrorRecordWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Consumer {

    private static final Logger LOGGER = LoggerFactory.getLogger(Consumer.class);

    private static final ObjectMapper mapper  = new ObjectMapper();
    private String eventName;
    private String haproxyId;

    private NSQConsumer consumer;

    private ParsedPayloadWriter writer;
    private ErrorRecordWriter   errorWriter;

    public Consumer(NSQLookup lookup, String topic, String channel, ParsedPayloadWriter writer, ErrorRecordWriter errorWriter) {
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

        this.writer = writer;
        this.errorWriter = errorWriter;

        this.consumer = new NSQConsumer(lookup, topic, channel, consumeMessage);
    }

    public void start() {
        consumer.start();
    }

    public void shutdown() {
        consumer.shutdown();
    }

    private NSQMessageCallback consumeMessage = (message) -> {
        NsqEventHeader payload = null;
        String payloadString = null;
        try {
            payloadString = new String(message.getMessage());
            payload = mapper.readValue(message.getMessage(), NsqEventHeader.class);
            ParsedPayload parsedPayload = new ParsedPayload(payload, haproxyId, eventName, payloadString);
            writer.write(parsedPayload);
        } catch (Throwable t) {
            LOGGER.error("Cannot publish message to cassandra. Reason: " + t.getMessage() + ". " + eventName + " -> " + payloadString);
            errorWriter.write(new ErrorRecord(message, t.getMessage()));
        }

        message.finished();
    };
}
