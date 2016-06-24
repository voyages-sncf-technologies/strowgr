/*
 *  Copyright (C) 2016 VSCT
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

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
