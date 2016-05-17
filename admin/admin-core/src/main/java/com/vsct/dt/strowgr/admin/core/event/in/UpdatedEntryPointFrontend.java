package com.vsct.dt.strowgr.admin.core.event.in;

import java.util.HashMap;
import java.util.Map;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.vsct.dt.strowgr.admin.Preconditions.checkStringNotEmpty;

/**
 * Created by william_montaz on 12/04/2016.
 */
public class UpdatedEntryPointFrontend {

    private final String                  id;
    private final HashMap<String, String> context;

    public UpdatedEntryPointFrontend(String id, Map<String, String> context) {
        this.id = checkStringNotEmpty(id, "Updated Frontend should have an id");
        this.context = new HashMap<>(checkNotNull(context));
    }

    public String getId() {
        return id;
    }

    public HashMap<String, String> getContext() {
        return new HashMap<>(context);
    }
}
