package com.vsct.dt.haas.admin.nsq.consumer;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.vsct.dt.haas.admin.nsq.Payload;

public class CommitFailedPayload extends Payload {
    public CommitFailedPayload(@JsonProperty("correlationid") String correlationId,
                               @JsonProperty("timestamp") Long timestamp) {
        super(correlationId, timestamp);
    }
}
