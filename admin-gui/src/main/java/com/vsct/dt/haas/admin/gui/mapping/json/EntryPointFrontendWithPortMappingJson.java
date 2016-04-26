package com.vsct.dt.haas.admin.gui.mapping.json;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.vsct.dt.haas.admin.core.configuration.EntryPointFrontend;

import java.util.Map;

/**
 * Json mapping of {@code EntryPointFrontend}.
 *
 * Created by william_montaz on 09/02/2016.
 */
public class EntryPointFrontendWithPortMappingJson extends EntryPointFrontendMappingJson {

    private final Integer port;

    @JsonCreator
    public EntryPointFrontendWithPortMappingJson(@JsonProperty("id") String id,
                                                 @JsonProperty("port") Integer port,
                                                 @JsonProperty("context") Map<String, String> context) {
        super(id, context);
        this.port = port;
    }

    public Integer getPort() {
        return port;
    }
}
