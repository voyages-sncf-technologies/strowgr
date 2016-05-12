package com.vsct.dt.haas.admin.core.event.in;

import com.vsct.dt.haas.admin.core.EntryPointKey;

/**
 * Created by william_montaz on 05/02/2016.
 */
public class CommitFailureEvent extends EntryPointEvent {
    public CommitFailureEvent(String correlationId, EntryPointKey key) {
        super(correlationId, key);
    }
}
