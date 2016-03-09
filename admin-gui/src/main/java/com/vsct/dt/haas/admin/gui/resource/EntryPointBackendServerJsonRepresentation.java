package com.vsct.dt.haas.admin.gui.resource;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.vsct.dt.haas.admin.core.configuration.EntryPointBackendServer;

import java.util.Map;

/**
 * Created by william_montaz on 09/02/2016.
 */
public class EntryPointBackendServerJsonRepresentation extends EntryPointBackendServer {

    @JsonCreator
    public EntryPointBackendServerJsonRepresentation(@JsonProperty("id") String id,
                                                     @JsonProperty("hostname") String hostname,
                                                     @JsonProperty("ip") String ip,
                                                     @JsonProperty("port") String port,
                                                     @JsonProperty("context") Map<String, String> context,
                                                     @JsonProperty("contextOverride") Map<String, String> contextOverride) {
        super(id, hostname, ip, port, context, contextOverride);
    }

    public EntryPointBackendServerJsonRepresentation(EntryPointBackendServer s) {
        this(s.getId(), s.getHostname(), s.getIp(), s.getPort(), s.getContext(), s.getContextOverride());
    }
}
