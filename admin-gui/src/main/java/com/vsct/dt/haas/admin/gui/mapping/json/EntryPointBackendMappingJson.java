package com.vsct.dt.haas.admin.gui.mapping.json;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.vsct.dt.haas.admin.core.configuration.EntryPointBackend;

import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Json mapping of {@code EntryPointBackend}.
 *
 * Created by william_montaz on 09/02/2016.
 */
public class EntryPointBackendMappingJson extends EntryPointBackend {
    public EntryPointBackendMappingJson(@JsonProperty("id") String id,
                                        @JsonProperty("servers") Set<EntryPointBackendServerMappingJson> servers,
                                        @JsonProperty("context") Map<String, String> context) {
        super(id, servers.stream().map(Function.identity()).collect(Collectors.toSet()), context);
    }

    public EntryPointBackendMappingJson(EntryPointBackend b) {
        this(b.getId(), b.getServers().stream().map(EntryPointBackendServerMappingJson::new).collect(Collectors.toSet()), b.getContext());
    }
}
