package com.vsct.dt.haas.admin.core.event.out;

import com.vsct.dt.haas.admin.core.EntryPointKey;
import com.vsct.dt.haas.admin.core.configuration.EntryPoint;
import com.vsct.dt.haas.admin.core.event.in.EntryPointEvent;

import java.util.Optional;

public class EntryPointAddedEvent extends EntryPointEvent {
    private final EntryPoint configuration;

    public EntryPointAddedEvent(String correlationId, EntryPointKey key, EntryPoint configuration) {
        super(correlationId, key);
        this.configuration = configuration;
    }

    public Optional<EntryPoint> getConfiguration() {
        return Optional.ofNullable(configuration);
    }
}
