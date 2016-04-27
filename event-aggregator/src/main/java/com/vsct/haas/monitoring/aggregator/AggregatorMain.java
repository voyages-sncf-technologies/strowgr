package com.vsct.haas.monitoring.aggregator;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.brainlag.nsq.NSQConsumer;
import com.github.brainlag.nsq.lookup.DefaultNSQLookup;
import com.github.brainlag.nsq.lookup.NSQLookup;
import com.vsct.haas.monitoring.aggregator.cassandra.CassandraEvent;
import com.vsct.haas.monitoring.aggregator.nsq.NsqLookupClient;
import com.vsct.haas.monitoring.aggregator.nsq.NsqEventHeader;
import com.vsct.haas.monitoring.aggregator.nsq.UnavailableNsqException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;

public class AggregatorMain {

    private static final Logger LOGGER = LoggerFactory.getLogger(AggregatorMain.class);

    public static final String CHANNEL = "AGGREGATOR";

    private static final ObjectMapper mapper = new ObjectMapper();

    private static final Map<String, NSQConsumer> consumers = new HashMap<>();

    public static void main(String[] args) throws UnavailableNsqException, InterruptedException {
        NsqLookupClient lookupClient = new NsqLookupClient("parisiancocktail", 54161);

        NSQLookup lookup = new DefaultNSQLookup();
        lookup.addLookupAddress("parisiancocktail", 54161);

        consumers.put("commit_completed_default-name", createConsumer(lookup, "commit_completed_default-name"));
        consumers.put("commit_slave_completed_default-name", createConsumer(lookup, "commit_slave_completed_default-name"));
        consumers.put("commit_requested_default-name", createConsumer(lookup, "commit_requested_default-name"));
        consumers.put("commit_completed_default-name", createConsumer(lookup, "commit_completed_default-name"));

        LOGGER.info("Starting NSQ Consumers");
        for (NSQConsumer consumer : consumers.values()) {
            consumer.start();
        }

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            LOGGER.info("Shutting down consumers");
            for (NSQConsumer consumer : consumers.values()) {
                consumer.shutdown();
            }
        }));

        while(true){
            Thread.sleep(10000);
        }
    }

    private static NSQConsumer createConsumer(NSQLookup lookup, String topic) {
        LOGGER.info("Creating NSQConsumer for topic -> "+topic);

        /* By convention, topic is using _ to sperate eventName and haproxyId */
        String[] subs = topic.split("_");
        StringBuilder sb = new StringBuilder();
        for(int i = 0; i< subs.length - 1; i++){
            sb.append(subs[i]);
            if(i < subs.length - 2) sb.append("_");
        }

        final String eventName = sb.toString();
        final String haproxyId = subs[subs.length-1];

        return new NSQConsumer(lookup, topic, CHANNEL, (message) -> {
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

            CassandraEvent cassandraEvent = new CassandraEvent(payload, haproxyId, eventName, new String(message.getMessage()));

            sendToCassandra(cassandraEvent);

            message.finished();
        });
    }

    private static void sendToCassandra(CassandraEvent event) {
        System.out.println("------ NEW MESSAGE --------");
        System.out.println(event.getEventName());
    }

}
