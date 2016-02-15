package com.vsct.dt.haas.admin.core.event.in;

import com.vsct.dt.haas.admin.core.EntryPointKey;

/**
 * Created by william_montaz on 09/02/2016.
 */
public class UpdateEntryPointEvent extends EntryPointEvent {

    public UpdateEntryPointEvent(String correlationId, EntryPointKey key) {
        super(correlationId, key);
    }

}
