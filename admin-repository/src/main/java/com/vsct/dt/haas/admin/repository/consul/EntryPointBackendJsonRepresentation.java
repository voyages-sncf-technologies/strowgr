package com.vsct.dt.haas.admin.repository.consul;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.vsct.dt.haas.admin.core.configuration.EntryPointBackend;

import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Created by william_montaz on 09/02/2016.
 */
public class EntryPointBackendJsonRepresentation extends EntryPointBackend {
    public EntryPointBackendJsonRepresentation(@JsonProperty("id") String id,
                                               @JsonProperty("servers") Set<EntryPointBackendServerJsonRepresentation> servers,
                                               @JsonProperty("context") Map<String, String> context) {
        super(id, servers.stream().map(Function.identity()).collect(Collectors.toSet()), context);
    }
}
