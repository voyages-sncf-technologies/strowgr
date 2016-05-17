package com.vsct.dt.strowgr.admin.nsq;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Created by william_montaz on 02/02/2016.
 */
public abstract class Payload {

    private final long timestamp;

    private final String correlationId;

    public Payload(String correlationId) {
        this(correlationId, System.currentTimeMillis());
    }

    public Payload(String correlationId, long timestamp) {
        this.correlationId = checkNotNull(correlationId);
        this.timestamp = checkNotNull(timestamp);
    }

    public long getTimestamp() {
        return timestamp;
    }

    public String getCorrelationId() {
        return correlationId;
    }
}
