package com.vsct.dt.strowgr.admin.core.event.in;

import com.vsct.dt.strowgr.admin.core.EntryPointKey;
import com.vsct.dt.strowgr.admin.core.configuration.EntryPoint;

import java.util.Optional;

/**
 * Event of a new added entrypoint.
 * <p/>
 * Created by william_montaz on 02/02/2016.
 */
public class AddEntryPointEvent extends EntryPointEvent {

    private final EntryPoint configuration;

    public AddEntryPointEvent(String correlationId, EntryPointKey key, EntryPoint configuration) {
        super(correlationId, key);
        this.configuration = configuration;
    }

    public Optional<EntryPoint> getConfiguration() {
        return Optional.ofNullable(configuration);
    }


    @Override
    public String toString() {
        return "AddEntryPointEvent{" +
                "correlationId=" + getCorrelationId() +
                "key=" + getKey() +
                "configuration=" + configuration +
                '}';
    }

}
