package com.vsct.dt.haas.admin.core.event.out;

import com.vsct.dt.haas.admin.core.EntryPointKey;
import com.vsct.dt.haas.admin.core.configuration.EntryPointConfiguration;
import com.vsct.dt.haas.admin.core.event.in.EntryPointEvent;

public class CommitBeginEvent extends EntryPointEvent {
    private final EntryPointConfiguration configuration;
    private final String conf;

    public CommitBeginEvent(String correlationId, EntryPointKey key, EntryPointConfiguration configuration, String conf) {
        super(correlationId, key);
        this.configuration = configuration;
        this.conf = conf;
    }

    public EntryPointConfiguration getConfiguration() {
        return configuration;
    }

    public String getConf() {
        return conf;
    }
}
