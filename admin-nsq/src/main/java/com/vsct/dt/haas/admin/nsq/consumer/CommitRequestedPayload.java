package com.vsct.dt.haas.admin.nsq.consumer;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.vsct.dt.haas.admin.nsq.Payload;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Created by william_montaz on 02/02/2016.
 */
public class CommitRequestedPayload extends Payload {

    private final String application;
    private final String platform;
    private final String confBase64;

    @JsonCreator
    public CommitRequestedPayload(@JsonProperty("correlationId") String correlationId,
                                  @JsonProperty("timestamp") Long timestamp,
                                  @JsonProperty("application") String application,
                                  @JsonProperty("platform") String platform,
                                  @JsonProperty("conf") String confBase64) {
        super(correlationId, timestamp);
        this.application = checkNotNull(application);
        this.platform = checkNotNull(platform);
        this.confBase64 = checkNotNull(confBase64);
    }

    public String getApplication() {
        return application;
    }

    public String getPlatform() {
        return platform;
    }

    public String getConfBase64() {
        return confBase64;
    }
}
