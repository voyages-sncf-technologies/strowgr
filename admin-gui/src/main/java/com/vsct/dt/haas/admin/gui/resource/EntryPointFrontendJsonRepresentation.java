package com.vsct.dt.haas.admin.gui.resource;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.vsct.dt.haas.admin.core.configuration.EntryPointFrontend;

import java.util.Map;

/**
 * Created by william_montaz on 09/02/2016.
 */
public class EntryPointFrontendJsonRepresentation extends EntryPointFrontend {

    @JsonCreator
    public EntryPointFrontendJsonRepresentation(@JsonProperty("id") String id,
                                                @JsonProperty("context") Map<String, String> context) {
        super(id, context);
    }

    public EntryPointFrontendJsonRepresentation(EntryPointFrontend f) {
        this(f.getId(), f.getContext());
    }
}
