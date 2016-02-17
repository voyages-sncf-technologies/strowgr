package com.vsct.dt.haas.admin.nsq.consumer;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.vsct.dt.haas.admin.nsq.Payload;

/**
 * Created by william_montaz on 02/02/2016.
 */
public class CommitRequestedPayload extends Payload {

    private final String application;
    private final String platform;
    private final String confBase64;

    @JsonCreator
    public CommitRequestedPayload(@JsonProperty("correlationid") String correlationId,
                                  @JsonProperty("application") String application,
                                  @JsonProperty("platform") String platform,
                                  @JsonProperty("conf") String confBase64) {
        super(correlationId);
        this.application = application;
        this.platform = platform;
        this.confBase64 = confBase64;
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
