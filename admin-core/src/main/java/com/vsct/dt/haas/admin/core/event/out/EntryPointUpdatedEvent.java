package com.vsct.dt.haas.admin.core.event.out;

import com.vsct.dt.haas.admin.core.EntryPointKey;
import com.vsct.dt.haas.admin.core.event.in.EntryPointEvent;

/**
 * Created by william_montaz on 12/04/2016.
 */
public class EntryPointUpdatedEvent extends EntryPointEvent {
    public EntryPointUpdatedEvent(String correlationId, EntryPointKey key) {
        super(correlationId, key);
    }
}
