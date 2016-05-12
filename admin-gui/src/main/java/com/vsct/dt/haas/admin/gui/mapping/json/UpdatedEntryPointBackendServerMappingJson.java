package com.vsct.dt.haas.admin.gui.mapping.json;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.vsct.dt.haas.admin.core.event.in.UpdatedEntryPointBackendServer;

import java.util.Map;

/**
 * Created by william_montaz on 12/04/2016.
 */
public class UpdatedEntryPointBackendServerMappingJson extends UpdatedEntryPointBackendServer {

    @JsonCreator
    public UpdatedEntryPointBackendServerMappingJson(@JsonProperty("id") String id,
                                                     @JsonProperty("contextOverride") Map<String, String> contextOverride) {
        super(id, contextOverride);
    }
}
