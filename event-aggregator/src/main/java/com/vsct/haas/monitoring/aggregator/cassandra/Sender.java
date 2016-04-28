package com.vsct.haas.monitoring.aggregator.cassandra;

import com.datastax.driver.core.BoundStatement;
import com.datastax.driver.core.PreparedStatement;
import com.datastax.driver.core.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Sender {

    private static final Logger LOGGER = LoggerFactory.getLogger(Sender.class);
    private static final String                   TABLE     = "entrypoint_by_day";

    private final Session session;

    private final PreparedStatement preparedStatement;

    public Sender(Session session) {
        this.session = session;

        preparedStatement = session.prepare(
                "INSERT INTO " + TABLE + " (id, date, event_timestamp, correlation_id, event_name, haproxy_id, payload) VALUES (?,?,?,?,?,?,?);"
        );
        LOGGER.info("Prepared statement");
    }

    public void send(CassandraEvent event) {
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
