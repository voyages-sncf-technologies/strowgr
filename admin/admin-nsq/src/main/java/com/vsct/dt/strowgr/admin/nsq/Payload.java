package com.vsct.dt.strowgr.admin.nsq;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Generic Payload for NSQ message.
 * <p>
 * Created by william_montaz on 02/02/2016.
 */
public abstract class Payload {

    @JsonProperty("timestamp")
    private Long timestamp;

    @JsonProperty("correlationId")
    private String correlationId;

    protected Payload() {
    }

    public Payload(String correlationId) {
        this(correlationId, System.currentTimeMillis());
    }

    public Payload(String correlationId, Long timestamp) {
        this.correlationId = correlationId;
        this.timestamp = timestamp;
    }

    public Long getTimestamp() {
        return timestamp;
    }

    public String getCorrelationId() {
        return correlationId;
    }

    public void setCorrelationId(String correlationId) {
        this.correlationId = correlationId;
    }

    public void setTimestamp(Long timestamp) {
        this.timestamp = timestamp;
    }
}
