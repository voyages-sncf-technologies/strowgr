package com.vsct.dt.haas.admin.repository.consul.mapping.json;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.vsct.dt.haas.admin.core.configuration.EntryPoint;
import com.vsct.dt.haas.admin.core.configuration.EntryPointBackend;
import com.vsct.dt.haas.admin.core.configuration.EntryPointFrontend;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static java.util.function.Function.identity;

/**
 * Json mapping of {@code EntryPoint}.
 *
 * Created by william_montaz on 09/02/2016.
 */
public class EntryPointMappingJson extends EntryPoint {

    @JsonCreator
    public EntryPointMappingJson(@JsonProperty("haproxy") String haproxy,
                                 @JsonProperty("hapUser") String hapUser,
                                 @JsonProperty("frontends") Set<EntryPointFrontendMappingJson> frontends,
                                 @JsonProperty("backends") Set<EntryPointBackendMappingJson> backends,
                                 @JsonProperty("context") Map<String, String> context) {
        super(haproxy,
                hapUser,
                frontends.stream().map(identity()).collect(Collectors.toSet()),
                backends.stream().map(identity()).collect(Collectors.toSet()),
                context);
    }

}
