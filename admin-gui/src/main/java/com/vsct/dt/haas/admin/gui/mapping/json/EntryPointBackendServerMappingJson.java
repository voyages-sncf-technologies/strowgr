package com.vsct.dt.haas.admin.gui.mapping.json;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.vsct.dt.haas.admin.core.configuration.EntryPointBackendServer;

import java.util.Map;

/**
 * Json mapping of {@code EntryPointBackendServer}.
 *
 * Created by william_montaz on 09/02/2016.
 */
public class EntryPointBackendServerMappingJson extends EntryPointBackendServer {

    @JsonCreator
    public EntryPointBackendServerMappingJson(@JsonProperty("id") String id,
                                              @JsonProperty("hostname") String hostname,
                                              @JsonProperty("ip") String ip,
                                              @JsonProperty("port") String port,
                                              @JsonProperty("context") Map<String, String> context,
                                              @JsonProperty("contextOverride") Map<String, String> contextOverride) {
        super(id, hostname, ip, port, context, contextOverride);
    }

    public EntryPointBackendServerMappingJson(EntryPointBackendServer s) {
        this(s.getId(), s.getHostname(), s.getIp(), s.getPort(), s.getContext(), s.getContextOverride());
    }
}
