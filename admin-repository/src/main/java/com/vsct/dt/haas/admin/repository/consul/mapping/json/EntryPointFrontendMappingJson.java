package com.vsct.dt.haas.admin.repository.consul.mapping.json;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.vsct.dt.haas.admin.core.configuration.EntryPointFrontend;

import java.util.Map;

/**
 * Json mapping of {@code EntryPointFrontend}.
 *
 * Created by william_montaz on 09/02/2016.
 */
public class EntryPointFrontendMappingJson extends EntryPointFrontend {

    @JsonCreator
    public EntryPointFrontendMappingJson(@JsonProperty("id") String id,
                                         @JsonProperty("context") Map<String, String> context) {
        super(id, context);
    }

}
