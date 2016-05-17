package com.vsct.dt.strowgr.admin.nsq.consumer;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.vsct.dt.strowgr.admin.nsq.Payload;

public class CommitFailedPayload extends Payload {

    private final String application;
    private final String platform;


    public CommitFailedPayload(@JsonProperty("correlationId") String correlationId,
                               @JsonProperty("timestamp") Long timestamp,
                               @JsonProperty("application") String application,
                               @JsonProperty("platform") String platform) {
        super(correlationId, timestamp);
        this.application = application;
        this.platform = platform;
    }

    public String getApplication() {
        return application;
    }

    public String getPlatform() {
        return platform;
    }
}
