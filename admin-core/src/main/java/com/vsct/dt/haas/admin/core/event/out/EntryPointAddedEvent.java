package com.vsct.dt.haas.admin.core.event.out;

import com.vsct.dt.haas.admin.core.EntryPointKey;
import com.vsct.dt.haas.admin.core.configuration.EntryPointConfiguration;
import com.vsct.dt.haas.admin.core.event.in.EntryPointEvent;

public class EntryPointAddedEvent extends EntryPointEvent {
    private final EntryPointConfiguration configuration;

    public EntryPointAddedEvent(String correlationId, EntryPointKey key, EntryPointConfiguration configuration) {
        super(correlationId, key);
        this.configuration = configuration;
    }

    public EntryPointConfiguration getConfiguration() {
        return configuration;
    }
}
