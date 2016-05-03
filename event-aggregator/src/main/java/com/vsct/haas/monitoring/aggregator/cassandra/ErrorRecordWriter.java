package com.vsct.haas.monitoring.aggregator.cassandra;

import com.datastax.driver.core.BoundStatement;
import com.datastax.driver.core.PreparedStatement;
import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Session;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class ErrorRecordWriter {
    private static final Logger LOGGER = LoggerFactory.getLogger(ErrorRecordWriter.class);
    private static final String TABLE  = "error_by_day";

    private final Session session;

    private final PreparedStatement writeRawPayloadPeparedStatement;

    public ErrorRecordWriter(Session session) {
        this.session = session;

        writeRawPayloadPeparedStatement = session.prepare(
                "INSERT INTO " + TABLE + " (date, timestamp, reason, payload) VALUES (?,?,?,?);"
        );
        LOGGER.debug("Prepared write error statement");
    }

    public void write(ErrorRecord payload) {
        if (LOGGER.isDebugEnabled()) LOGGER.debug("Record message processing error. Payload=" + payload.getPayload());

        BoundStatement bound = writeRawPayloadPeparedStatement.bind(
                payload.getDate(),
                payload.getTimestamp(),
                payload.getReason(),
                payload.getPayload()
        );

        Futures.addCallback(session.executeAsync(bound), new FutureCallback<ResultSet>() {
            @Override
            public void onSuccess(ResultSet rows) {

            }

            @Override
            public void onFailure(Throwable throwable) {
                LOGGER.error("Could not send message processing error to cassandra. Everything relies on the log.");
            }
        });

    }

}
