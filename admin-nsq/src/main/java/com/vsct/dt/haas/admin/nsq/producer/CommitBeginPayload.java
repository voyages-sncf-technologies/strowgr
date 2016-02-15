package com.vsct.dt.haas.admin.nsq.producer;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.vsct.dt.haas.admin.nsq.Payload;

public class CommitBeginPayload extends Payload {

    private final String application;
    private final String platform;
    private final String conf;

    @JsonCreator
    public CommitBeginPayload(@JsonProperty("correlationId") String correlationId,
                              @JsonProperty("application") String application,
                              @JsonProperty("platform") String platform,
                              @JsonProperty("conf") String conf) {
        super(correlationId);
        this.application = application;
        this.platform = platform;
        this.conf = conf;
    }

    public String getApplication() {
        return application;
    }

    public String getPlatform() {
        return platform;
    }

    public String getConf() {
        return conf;
    }
}
