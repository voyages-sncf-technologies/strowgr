package com.vsct.dt.strowgr.admin.gui.mapping.json;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.vsct.dt.strowgr.admin.core.configuration.EntryPoint;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static java.util.function.Function.identity;

/**
 * Json representation of {@code EntryPointConfiguration}.
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

    public EntryPointMappingJson(EntryPoint c) {
        this(c.getHaproxy(), c.getHapUser(),
                c.getFrontends().stream().map(EntryPointFrontendMappingJson::new).collect(Collectors.toSet()),
                c.getBackends().stream().map(EntryPointBackendMappingJson::new).collect(Collectors.toSet()),
                c.getContext());
    }
}
