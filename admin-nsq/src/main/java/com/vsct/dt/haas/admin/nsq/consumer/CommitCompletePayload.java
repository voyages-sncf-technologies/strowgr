package com.vsct.dt.haas.admin.nsq.consumer;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.vsct.dt.haas.admin.nsq.Payload;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Created by william_montaz on 02/02/2016.
 */
public class CommitCompletePayload extends Payload {

    private final String application;
    private final String platform;

    @JsonCreator
    public CommitCompletePayload(@JsonProperty("correlationid") String correlationId,
                                 @JsonProperty("timestamp") Long timestamp,
                                 @JsonProperty("application") String application,
                                 @JsonProperty("platform") String platform) {
        super(correlationId, timestamp);
        this.application = checkNotNull(application);
        this.platform = checkNotNull(platform);
    }

    public String getApplication() {
        return application;
    }

    public String getPlatform() {
        return platform;
    }
}
