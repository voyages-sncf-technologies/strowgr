package com.vsct.dt.strowgr.admin.gui.mapping.json;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.vsct.dt.strowgr.admin.core.event.in.UpdatedEntryPointFrontend;

import java.util.Map;

/**
 * Created by william_montaz on 12/04/2016.
 */
public class UpdatedEntryPointFrontendMappingJson extends UpdatedEntryPointFrontend {

    @JsonCreator
    public UpdatedEntryPointFrontendMappingJson(@JsonProperty("id") String id,
                                                @JsonProperty("context") Map<String, String> context) {
        super(id, context);
    }
}
