package com.vsct.dt.strowgr.admin.core.event.in;

import com.vsct.dt.strowgr.admin.core.EntryPointKey;

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
