package com.vsct.dt.haas.admin.core.event.in;

import com.vsct.dt.haas.admin.core.EntryPointKey;

/**
 * Event triggered when current configuration is commiting.
 *
 * Created by william_montaz on 05/02/2016.
 */
public class TryCommitCurrentConfigurationEvent extends EntryPointEvent {

    public TryCommitCurrentConfigurationEvent(String correlationId, EntryPointKey key) {
        super(correlationId, key);
    }

}
