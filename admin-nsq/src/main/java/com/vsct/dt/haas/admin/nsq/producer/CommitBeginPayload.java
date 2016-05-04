package com.vsct.dt.haas.admin.nsq.producer;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.vsct.dt.haas.admin.nsq.Payload;

import static com.google.common.base.Preconditions.checkNotNull;

public class CommitBeginPayload extends Payload {

    private final String application;
    private final String platform;
    private final String conf;
    private final String syslogConf;

    @JsonCreator
    public CommitBeginPayload(@JsonProperty("correlationId") String correlationId,
                              @JsonProperty("application") String application,
                              @JsonProperty("platform") String platform,
                              @JsonProperty("conf") String conf,
                              @JsonProperty("syslogConf") String syslogConf) {
        super(correlationId);
        this.application = checkNotNull(application);
        this.platform = checkNotNull(platform);
        this.conf = checkNotNull(conf);
        this.syslogConf = checkNotNull(syslogConf);
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

    public String getSyslogConf() {
        return syslogConf;
    }
}
