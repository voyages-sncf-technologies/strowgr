package com.vsct.strowgr.monitoring.aggregator.cassandra;

import com.datastax.driver.core.BoundStatement;
import com.datastax.driver.core.PreparedStatement;
import com.datastax.driver.core.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ParsedPayloadWriter {

    private static final Logger LOGGER = LoggerFactory.getLogger(ParsedPayloadWriter.class);
    private static final String TABLE  = "entrypoint_by_day";

    private final Session session;

    private final PreparedStatement writeEventPeparedStatement;

    public ParsedPayloadWriter(Session session) {
        this.session = session;

        writeEventPeparedStatement = session.prepare(
                "INSERT INTO " + TABLE + " (id, date, event_timestamp, correlation_id, event_name, haproxy_id, payload) VALUES (?,?,?,?,?,?,?);"
        );
        LOGGER.debug("Prepared write event statement");

    }

    public void write(ParsedPayload payload) {
        if (LOGGER.isDebugEnabled())
            LOGGER.debug("Record event " + payload.getEventName() + " for " + payload.getId());

        BoundStatement bound = writeEventPeparedStatement.bind(
                payload.getId(),
                payload.getDate(),
                payload.getEventTimestamp(),
                payload.getCorrelationId(),
                payload.getEventName(),
                payload.getHaproxyId(),
                payload.getPayload()
        );

        session.executeAsync(bound);
    }

}
