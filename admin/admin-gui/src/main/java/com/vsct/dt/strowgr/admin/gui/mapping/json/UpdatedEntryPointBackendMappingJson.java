package com.vsct.dt.strowgr.admin.gui.mapping.json;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.vsct.dt.strowgr.admin.core.event.in.UpdatedEntryPointBackend;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static java.util.function.Function.identity;

/**
 * Created by william_montaz on 12/04/2016.
 */
public class UpdatedEntryPointBackendMappingJson extends UpdatedEntryPointBackend {

    @JsonCreator
    public UpdatedEntryPointBackendMappingJson(@JsonProperty("id") String id,
                                               @JsonProperty("servers") Set<UpdatedEntryPointBackendServerMappingJson> servers,
                                               @JsonProperty("context") Map<String, String> context) {
        super(id, servers.stream().map(identity()).collect(Collectors.toSet()), context);
    }
}
