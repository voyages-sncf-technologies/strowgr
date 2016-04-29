package com.vsct.dt.haas.admin.nsq.consumer;

import com.vsct.dt.haas.admin.nsq.Payload;

public class CommitFailedPayload extends Payload {
    public CommitFailedPayload(String correlationId) {
        super(correlationId);
    }
}
