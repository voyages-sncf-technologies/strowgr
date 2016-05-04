package com.vsct.dt.haas.admin.nsq.consumer;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.vsct.dt.haas.admin.nsq.Payload;

import java.util.HashMap;
import java.util.Map;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Created by william_montaz on 02/02/2016.
 */
public class RegisterServerPayload extends Payload {

    private final String application;
    private final String platform;
    private final String backend;
    private final String id;
    private final String hostname;
    private final String ip;
    private final String port;
    private final Map<String, String> context;

    @JsonCreator
    public RegisterServerPayload(@JsonProperty("correlationid") String correlationId,
                                 @JsonProperty("timestamp") Long timestamp,
                                 @JsonProperty("application") String application,
                                 @JsonProperty("platform") String platform,
                                 @JsonProperty("backend") String backend,
                                 @JsonProperty("id") String id,
                                 @JsonProperty("hostname") String hostname,
                                 @JsonProperty("ip") String ip,
                                 @JsonProperty("port") String port,
                                 @JsonProperty("context") Map<String, String> context) {
        super(correlationId, timestamp);
        this.context = checkNotNull(context, "context attribute is missing in RegisterServer event consume from NSQ");
        this.application = checkNotNull(application, "application attribute is missing in RegisterServer event consume from NSQ");
        this.platform = checkNotNull(platform, "platform attribute is missing in RegisterServer event consume from NSQ");
        this.backend = checkNotNull(backend, "backend attribute is missing in RegisterServer event consume from NSQ");
        this.id = checkNotNull(id);
        this.hostname = checkNotNull(hostname);
        this.ip = checkNotNull(ip);
        this.port = checkNotNull(port);
    }

    public String getApplication() {
        return application;
    }

    public String getPlatform() {
        return platform;
    }

    public String getBackend() {
        return backend;
    }

    public String getId() {
        return id;
    }

    public String getHostname() {
        return hostname;
    }

    public String getIp() {
        return ip;
    }

    public String getPort() {
        return port;
    }

    public Map<String, String> getContext() {
        return new HashMap<>(context);
    }

}
