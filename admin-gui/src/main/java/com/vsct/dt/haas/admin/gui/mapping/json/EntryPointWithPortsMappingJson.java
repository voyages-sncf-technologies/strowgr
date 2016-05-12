package com.vsct.dt.haas.admin.gui.mapping.json;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.vsct.dt.haas.admin.core.EntryPointKey;
import com.vsct.dt.haas.admin.core.PortProvider;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static java.util.function.Function.identity;

/**
 * Json representation of {@code EntryPointConfiguration}.
 * <p>
 * Created by william_montaz on 09/02/2016.
 */
public class EntryPointWithPortsMappingJson extends EntryPointMappingJson {

    private final Integer syslogPort;

    @JsonCreator
    public EntryPointWithPortsMappingJson(@JsonProperty("haproxy") String haproxy,
                                          @JsonProperty("hapUser") String hapUser,
                                          @JsonProperty("syslogPort") Integer syslogPort,
                                          @JsonProperty("frontends") Set<EntryPointFrontendWithPortMappingJson> frontends,
                                          @JsonProperty("backends") Set<EntryPointBackendMappingJson> backends,
                                          @JsonProperty("context") Map<String, String> context) {
        super(haproxy,
                hapUser,
                frontends.stream().map(identity()).collect(Collectors.toSet()),
                backends.stream().map(identity()).collect(Collectors.toSet()),
                context);
        this.syslogPort = syslogPort;
    }

    public Map<String, Integer> generatePortMapping() {
        HashMap<String, Integer> mapping = new HashMap<>();
        mapping.put(syslogPortId(), this.syslogPort);
        this.getFrontends().forEach(f -> mapping.put(f.getId(),
                ((EntryPointFrontendWithPortMappingJson) f).getPort()
        ));
        return mapping;
    }

}
