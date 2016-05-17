package com.vsct.dt.strowgr.admin.gui.mapping.json;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.vsct.dt.strowgr.admin.core.event.in.UpdatedEntryPoint;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static java.util.function.Function.identity;

/**
 * Created by william_montaz on 12/04/2016.
 */
public class UpdatedEntryPointMappingJson extends UpdatedEntryPoint {

    @JsonCreator
    public UpdatedEntryPointMappingJson(@JsonProperty("hapUser") String hapUser,
                                        @JsonProperty("context") Map<String, String> context,
                                        @JsonProperty("frontends") Set<UpdatedEntryPointFrontendMappingJson> frontends,
                                        @JsonProperty("backends") Set<UpdatedEntryPointBackendMappingJson> backends) {
        super(hapUser, context,
                frontends.stream().map(identity()).collect(Collectors.toSet()),
                backends.stream().map(identity()).collect(Collectors.toSet()));
    }
}
