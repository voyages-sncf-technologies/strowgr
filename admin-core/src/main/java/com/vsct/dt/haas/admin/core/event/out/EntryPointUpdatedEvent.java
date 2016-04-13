package com.vsct.dt.haas.admin.core.event.out;

import com.vsct.dt.haas.admin.core.EntryPointKey;
import com.vsct.dt.haas.admin.core.configuration.EntryPoint;
import com.vsct.dt.haas.admin.core.event.in.EntryPointEvent;

import java.util.Optional;

/**
 * Created by william_montaz on 12/04/2016.
 */
public class EntryPointUpdatedEvent extends EntryPointEvent {
    private final EntryPoint configuration;

    public EntryPointUpdatedEvent(String correlationId, EntryPointKey key, EntryPoint configuration) {
        super(correlationId, key);
        this.configuration = configuration;
    }

    public Optional<EntryPoint> getConfiguration() {
        return Optional.ofNullable(configuration);
    }
}
