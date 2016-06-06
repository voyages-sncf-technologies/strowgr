package com.vsct.dt.strowgr.admin.core.event.out;

import com.vsct.dt.strowgr.admin.core.EntryPointKey;
import com.vsct.dt.strowgr.admin.core.configuration.EntryPoint;
import com.vsct.dt.strowgr.admin.core.event.in.EntryPointEvent;

import java.util.Optional;

public class CommitRequestedEvent extends EntryPointEvent {
    private final EntryPoint configuration;
    private final String conf;
    private final String syslogConf;

    public CommitRequestedEvent(String correlationId, EntryPointKey key, EntryPoint configuration, String conf, String syslogConf) {
        super(correlationId, key);
        this.configuration = configuration;
        this.conf = conf;
        this.syslogConf = syslogConf;
    }

    public Optional<EntryPoint> getConfiguration() {
        return Optional.ofNullable(configuration);
    }

    public String getConf() {
        return conf;
    }

    public String getSyslogConf() {
        return syslogConf;
    }

    @Override
    public String toString() {
        return "CommitRequestedEvent{" +
                "correlationId=" + getCorrelationId() +
                "key=" + getKey() +
                "configuration=" + configuration +
                ", conf='" + conf + '\'' +
                ", syslogConf='" + syslogConf + '\'' +
                '}';
    }
}
