package com.vsct.dt.haas.admin.core.event.out;

import com.vsct.dt.haas.admin.core.EntryPointKey;
import com.vsct.dt.haas.admin.core.configuration.EntryPointConfiguration;
import com.vsct.dt.haas.admin.core.event.in.EntryPointEvent;

public class CommitBeginEvent extends EntryPointEvent {
    private final EntryPointConfiguration configuration;
    private final String conf;
    private final String syslogConf;

    public CommitBeginEvent(String correlationId, EntryPointKey key, EntryPointConfiguration configuration, String conf, String syslogConf) {
        super(correlationId, key);
        this.configuration = configuration;
        this.conf = conf;
        this.syslogConf = syslogConf;
    }

    public EntryPointConfiguration getConfiguration() {
        return configuration;
    }

    public String getConf() {
        return conf;
    }

    public String getSyslogConf() {
        return syslogConf;
    }
}
