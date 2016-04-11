package com.vsct.dt.haas.admin.core.event.in;


import com.vsct.dt.haas.admin.core.EntryPointKey;
import com.vsct.dt.haas.admin.core.configuration.EntryPoint;

import java.util.Optional;

/**
 * Generic entrypoint event.
 *
 * Created by william_montaz on 05/02/2016.
 */
public class EntryPointEvent {

    private final String correlationId;
    private final EntryPointKey key;

    public EntryPointEvent(String correlationId, EntryPointKey key) {
        this.correlationId = correlationId;
        this.key = key;
    }

    public EntryPointKey getKey() {
        return key;
    }

    public String getCorrelationId() {
        return correlationId;
    }

    public Optional<EntryPoint> getConfiguration(){
        return Optional.empty();
    }

}
