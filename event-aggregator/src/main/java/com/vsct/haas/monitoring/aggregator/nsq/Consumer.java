package com.vsct.haas.monitoring.aggregator.nsq;

import com.datastax.driver.core.BoundStatement;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.brainlag.nsq.NSQConsumer;
import com.github.brainlag.nsq.callbacks.NSQMessageCallback;
import com.github.brainlag.nsq.lookup.NSQLookup;
import com.vsct.haas.monitoring.aggregator.cassandra.CassandraEvent;
import com.vsct.haas.monitoring.aggregator.cassandra.Sender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class Consumer {

    private static final Logger LOGGER = LoggerFactory.getLogger(Consumer.class);

    private static final ObjectMapper mapper  = new ObjectMapper();
    private static final  String       CHANNEL = "AGGREGATOR";
    private String eventName;
    private String haproxyId;

    private NSQConsumer consumer;
    private Sender sender;

    public Consumer(NSQLookup lookup, String topic, Sender sender) {
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

        this.sender = sender;
        this.consumer = new NSQConsumer(lookup, topic, CHANNEL, consumeMessage);
    }

    public void start() {
        consumer.start();
    }

    public void shutdown() {
        consumer.shutdown();
    }

    private NSQMessageCallback consumeMessage = (message) -> {
        NsqEventHeader payload = null;
        try {
            payload = mapper.readValue(message.getMessage(), NsqEventHeader.class);
        } catch (IOException e) {
            LOGGER.error("can't deserialize the payload of message at " + message.getTimestamp() + ", id=" + new String(message.getId()) + ": " + new String(message.getMessage()), e);
            //Avoid republishing message and stop processing
            //TODO send in a dead event table
            message.finished();
            return;
        }

        String payloadString = null;
        try {
            payloadString = new String(message.getMessage());
            CassandraEvent cassandraEvent = new CassandraEvent(payload, haproxyId, eventName, payloadString);
            sender.send(cassandraEvent);
        } catch (Throwable t) {
            LOGGER.error("Cannot publish message to cassandra. Reason: " + t.getMessage() + ". " + eventName + " -> " + payloadString);
        }

        message.finished();
    };
}
