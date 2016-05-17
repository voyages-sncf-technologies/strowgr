package com.vsct.strowgr.monitoring.aggregator;

import com.datastax.driver.core.*;
import com.github.brainlag.nsq.lookup.DefaultNSQLookup;
import com.github.brainlag.nsq.lookup.NSQLookup;
import com.vsct.strowgr.monitoring.aggregator.cassandra.ParsedPayloadWriter;
import com.vsct.strowgr.monitoring.aggregator.cassandra.ErrorRecordWriter;
import com.vsct.strowgr.monitoring.aggregator.nsq.Consumer;
import com.vsct.strowgr.monitoring.aggregator.nsq.UnavailableNsqException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;

public class AggregatorMain {

    private static final Logger LOGGER = LoggerFactory.getLogger(AggregatorMain.class);

    private static final HashMap<String, Consumer> consumers          = new HashMap<>();
    private static final String                    CASSANDRA_NODE     = "cassandra.node";
    private static final String                    CASSANDRA_KEYSPACE = "cassandra.keyspace";
    private static final String                    NSQ_LOOKUP_HOST    = "nsq.lookup.host";
    private static final String                    NSQ_LOOKUP_PORT    = "nsq.lookup.port";
    private static final String                    AGGREGATOR_CHANNEL = "nsq.channel";

    public static void main(String[] args) throws UnavailableNsqException, InterruptedException {
        //NsqLookupClient lookupClient = new NsqLookupClient("parisiancocktail", 54161);
        String cassandraNode = System.getenv(CASSANDRA_NODE);
        String keyspace = System.getenv(CASSANDRA_KEYSPACE);
        String nsqlookupHost = System.getenv(NSQ_LOOKUP_HOST);
        String nsqlookupPortString = System.getenv(NSQ_LOOKUP_PORT);
        String channel = System.getenv(AGGREGATOR_CHANNEL);

        if (cassandraNode == null | keyspace == null | nsqlookupHost == null | nsqlookupPortString == null | channel == null){
            System.out.println("Ensure you have set all mandatory environment variables :");
            System.out.println("\tcassandra.node");
            System.out.println("\tcassandra.keyspace");
            System.out.println("\tnsq.lookup.host");
            System.out.println("\tnsq.lookup.ip");
            System.out.println("\tnsq.channel");
            return;
        }

        int nsqlookupPort = Integer.valueOf(nsqlookupPortString);

        /* Reach Cassandra */
        Cluster cluster = Cluster.builder()
                .addContactPoint(cassandraNode)
                .build();
        Metadata metadata = cluster.getMetadata();
        LOGGER.info("Connected to cluster: " + metadata.getClusterName());

        Session session = cluster.connect(keyspace);
        LOGGER.info("Initiated Cassandra session");

        ParsedPayloadWriter writer = new ParsedPayloadWriter(session);
        ErrorRecordWriter errorWriter = new ErrorRecordWriter(session);
        MessageRecorder messageRecorder = new MessageRecorder(writer, errorWriter);

        NSQLookup lookup = new DefaultNSQLookup();
        lookup.addLookupAddress(nsqlookupHost, nsqlookupPort);
        consumers.put("commit_completed_default-name", new Consumer(lookup, "commit_completed_default-name", channel, messageRecorder));
        consumers.put("commit_slave_completed_default-name", new Consumer(lookup, "commit_slave_completed_default-name", channel, messageRecorder));
        consumers.put("commit_requested_default-name", new Consumer(lookup, "commit_requested_default-name", channel, messageRecorder));
        consumers.put("commit_failed_default-name", new Consumer(lookup, "commit_failed_default-name", channel, messageRecorder));
        consumers.put("register_server", new Consumer(lookup, "register_server", channel, messageRecorder));

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
