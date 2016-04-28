package com.vsct.haas.monitoring.aggregator;

import com.datastax.driver.core.*;
import com.github.brainlag.nsq.lookup.DefaultNSQLookup;
import com.github.brainlag.nsq.lookup.NSQLookup;
import com.vsct.haas.monitoring.aggregator.cassandra.ParsedPayloadWriter;
import com.vsct.haas.monitoring.aggregator.cassandra.ErrorRecordWriter;
import com.vsct.haas.monitoring.aggregator.nsq.Consumer;
import com.vsct.haas.monitoring.aggregator.nsq.UnavailableNsqException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

public class AggregatorMain {

    private static final Logger LOGGER = LoggerFactory.getLogger(AggregatorMain.class);

    private static final Map<String, Consumer> consumers = new HashMap<>();
    private static final String                   KEYSPACE  = "haaas";
    private static Cluster           cluster;
    private static Session           session;

    public static void main(String[] args) throws UnavailableNsqException, InterruptedException {
        //NsqLookupClient lookupClient = new NsqLookupClient("parisiancocktail", 54161);

        /* Reach Cassandra */
        cluster = Cluster.builder()
                .addContactPoint("localhost")
                .build();
        Metadata metadata = cluster.getMetadata();
        LOGGER.info("Connected to cluster: " + metadata.getClusterName());

        session = cluster.connect(KEYSPACE);
        LOGGER.info("Initiated Cassandra session");

        ParsedPayloadWriter writer = new ParsedPayloadWriter(session);
        ErrorRecordWriter errorWriter = new ErrorRecordWriter(session);

        NSQLookup lookup = new DefaultNSQLookup();
        lookup.addLookupAddress("parisiancocktail", 54161);
        consumers.put("commit_completed_default-name", new Consumer(lookup, "commit_completed_default-name", writer, errorWriter));
        consumers.put("commit_slave_completed_default-name", new Consumer(lookup, "commit_slave_completed_default-name", writer, errorWriter));
        consumers.put("commit_requested_default-name", new Consumer(lookup, "commit_requested_default-name", writer, errorWriter));
        consumers.put("commit_completed_default-name", new Consumer(lookup, "commit_completed_default-name", writer, errorWriter));

        LOGGER.info("Starting NSQ Consumers");
        for (Consumer consumer : consumers.values()) {
            consumer.start();
        }

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            LOGGER.info("Shutting down consumers");
            for (Consumer consumer : consumers.values()) {
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

}
