package com.vsct.dt.haas.admin.gui.resource;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.vsct.dt.haas.admin.core.configuration.EntryPointConfiguration;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static java.util.function.Function.identity;

/**
 * Created by william_montaz on 09/02/2016.
 */
public class EntryPointConfigurationJsonRepresentation extends EntryPointConfiguration {

    @JsonCreator
    public EntryPointConfigurationJsonRepresentation(@JsonProperty("haproxy") String haproxy,
                                                     @JsonProperty("hapUser") String hapUser,
                                                     @JsonProperty("frontends") Set<EntryPointFrontendJsonRepresentation> frontends,
                                                     @JsonProperty("backends") Set<EntryPointBackendJsonRepresentation> backends,
                                                     @JsonProperty("context") Map<String, String> context) {
        super(haproxy,
                hapUser,
                frontends.stream().map(identity()).collect(Collectors.toSet()),
                backends.stream().map(identity()).collect(Collectors.toSet()),
                context);
    }

    public EntryPointConfigurationJsonRepresentation(EntryPointConfiguration c) {
        this(c.getHaproxy(), c.getHapUser(),
                c.getFrontends().stream().map(f -> new EntryPointFrontendJsonRepresentation(f)).collect(Collectors.toSet()),
                c.getBackends().stream().map(b -> new EntryPointBackendJsonRepresentation(b)).collect(Collectors.toSet()),
                c.getContext());
    }
}
