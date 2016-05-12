package com.vsct.dt.haas.admin.core.event.in;

import java.util.*;

import static com.vsct.dt.haas.admin.Preconditions.checkStringNotEmpty;

/**
 * Created by william_montaz on 12/04/2016.
 */
public class UpdatedEntryPointBackendServer {

    private final String id;
    private final HashMap<String, String> contextOverride;

    public UpdatedEntryPointBackendServer(String id, Map<String, String> contextOverride) {
        this.id = checkStringNotEmpty(id, "Updated Server should have an id");
        this.contextOverride = new HashMap<>(contextOverride);
    }

    public String getId() {
        return id;
    }

    public HashMap<String, String> getContextOverride() {
        return new HashMap<>(contextOverride);
    }
}
