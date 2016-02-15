package com.vsct.dt.haas.admin.core.event.in;

import com.vsct.dt.haas.admin.core.EntryPointKey;
import com.vsct.dt.haas.admin.core.configuration.EntryPointConfiguration;

/**
 * Created by william_montaz on 02/02/2016.
 */
public class AddEntryPointEvent extends EntryPointEvent {

    private final EntryPointConfiguration configuration;

    public AddEntryPointEvent(String correlationId, EntryPointKey key, EntryPointConfiguration configuration) {
        super(correlationId, key);
        this.configuration = configuration;
    }

    public EntryPointConfiguration getConfiguration() {
        return configuration;
    }

}
