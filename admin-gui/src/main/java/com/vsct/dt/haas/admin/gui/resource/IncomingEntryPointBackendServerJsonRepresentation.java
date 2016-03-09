package com.vsct.dt.haas.admin.gui.resource;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.vsct.dt.haas.admin.core.configuration.EntryPointBackendServer;
import com.vsct.dt.haas.admin.core.configuration.IncomingEntryPointBackendServer;

import java.util.Map;

/**
 * Created by william_montaz on 09/02/2016.
 */
public class IncomingEntryPointBackendServerJsonRepresentation extends IncomingEntryPointBackendServer {

    @JsonCreator
    public IncomingEntryPointBackendServerJsonRepresentation(@JsonProperty("id") String id,
                                                             @JsonProperty("hostname") String hostname,
                                                             @JsonProperty("ip") String ip,
                                                             @JsonProperty("port") String port,
                                                             @JsonProperty("context") Map<String, String> context) {
        super(id, hostname, ip, port, context);
    }

}
