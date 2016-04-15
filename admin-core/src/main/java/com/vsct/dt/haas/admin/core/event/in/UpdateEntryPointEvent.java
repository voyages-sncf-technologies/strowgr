package com.vsct.dt.haas.admin.core.event.in;

import com.vsct.dt.haas.admin.core.EntryPointKey;
import java.util.Map;
import java.util.Set;

/**
 * Created by william_montaz on 09/02/2016.
 */
public class UpdateEntryPointEvent extends EntryPointEvent {

    private final UpdatedEntryPoint updatedEntryPoint;

    public UpdateEntryPointEvent(String correlationId, EntryPointKey key, UpdatedEntryPoint updatedEntryPoint) {
        super(correlationId, key);
        this.updatedEntryPoint = updatedEntryPoint;
    }

    public UpdatedEntryPoint getUpdatedEntryPoint() {
        return updatedEntryPoint;
    }
}
