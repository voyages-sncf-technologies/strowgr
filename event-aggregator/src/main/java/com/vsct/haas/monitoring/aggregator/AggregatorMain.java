package com.vsct.haas.monitoring.aggregator;

import com.datastax.driver.core.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.brainlag.nsq.NSQConsumer;
import com.github.brainlag.nsq.lookup.DefaultNSQLookup;
import com.github.brainlag.nsq.lookup.NSQLookup;
import com.vsct.haas.monitoring.aggregator.cassandra.CassandraEvent;
import com.vsct.haas.monitoring.aggregator.nsq.NsqEventHeader;
import com.vsct.haas.monitoring.aggregator.nsq.NsqLookupClient;
import com.vsct.haas.monitoring.aggregator.nsq.UnavailableNsqException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class AggregatorMain {

    private static final Logger LOGGER = LoggerFactory.getLogger(AggregatorMain.class);

    public static final String CHANNEL = "AGGREGATOR";

    private static final ObjectMapper mapper = new ObjectMapper();

    private static final Map<String, NSQConsumer> consumers = new HashMap<>();
    private static final String                   TABLE     = "entrypoint_by_day";
    private static final String                   KEYSPACE  = "haaas";
    private static Cluster           cluster;
    private static Session           session;
    private static PreparedStatement preparedStatement;

    public static void main(String[] args) throws UnavailableNsqException, InterruptedException {
        NsqLookupClient lookupClient = new NsqLookupClient("parisiancocktail", 54161);

        /* Reach Cassandra */
        cluster = Cluster.builder()
                .addContactPoint("localhost")
                .build();
        Metadata metadata = cluster.getMetadata();
        LOGGER.info("Connected to cluster: " + metadata.getClusterName());

        session = cluster.connect(KEYSPACE);
        LOGGER.info("Initiated Cassandra session");

        preparedStatement = session.prepare(
                "INSERT INTO " + TABLE + " (id, date, event_timestamp, correlation_id, event_name, haproxy_id, payload) VALUES (?,?,?,?,?,?,?);"
        );
        LOGGER.info("Prepared statement");

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

            LOGGER.info("Closing Cassandra connection");
            session.close();
            cluster.close();
        }));

        while (true) {
            Thread.sleep(10000);
        }
    }

    private static NSQConsumer createConsumer(NSQLookup lookup, String topic) {
        LOGGER.info("Creating NSQConsumer for topic -> " + topic);

        /* By convention, topic is using _ to sperate eventName and haproxyId */
        String[] subs = topic.split("_");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < subs.length - 1; i++) {
            sb.append(subs[i]);
            if (i < subs.length - 2) sb.append("_");
        }

        final String eventName = sb.toString();
        final String haproxyId = subs[subs.length - 1];

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

            String payloadString = null;
            try {
                payloadString = new String(message.getMessage());
                CassandraEvent cassandraEvent = new CassandraEvent(payload, haproxyId, eventName, payloadString);

                sendToCassandra(cassandraEvent);
            } catch (Throwable t) {
                LOGGER.error("Cannot publish message to cassandra. Reason: "+t.getMessage()+" MESSAGE -> "+payloadString);
            }
            message.finished();
        });
    }

    private static void sendToCassandra(CassandraEvent event) {
        if (LOGGER.isDebugEnabled()) LOGGER.debug("Record " + event.getEventName() + " for " + event.getId());

        BoundStatement bound = preparedStatement.bind(
                event.getId(),
                event.getDate(),
                event.getEventTimestamp(),
                event.getCorrelationId(),
                event.getEventName(),
                event.getHaproxyId(),
                event.getPayload()
        );
        session.execute(bound);
    }

}
