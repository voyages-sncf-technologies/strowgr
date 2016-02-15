package com.vsct.dt.haas.admin.nsq;

/**
 * Created by william_montaz on 02/02/2016.
 */
public abstract class Payload {

    private final long timestamp;

    private final String correlationId;

    public Payload(String correlationId) {
        this.correlationId = correlationId;
        this.timestamp = System.currentTimeMillis();
    }

    public long getTimestamp() {
        return timestamp;
    }

    public String getCorrelationId() {
        return correlationId;
    }
}
