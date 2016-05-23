package com.vsct.dt.strowgr.admin.core.event.out;

import com.google.common.collect.ImmutableSet;
import com.vsct.dt.strowgr.admin.core.EntryPointKey;
import com.vsct.dt.strowgr.admin.core.configuration.IncomingEntryPointBackendServer;
import com.vsct.dt.strowgr.admin.core.event.in.EntryPointEvent;

import java.util.Set;

public class ServerRegisteredEvent extends EntryPointEvent {
    private final String                                        backend;
    private final ImmutableSet<IncomingEntryPointBackendServer> servers;

    public ServerRegisteredEvent(String correlationId, EntryPointKey key, String backend, Set<IncomingEntryPointBackendServer> servers) {
        super(correlationId, key);
        this.backend = backend;
        this.servers = ImmutableSet.copyOf(servers);
    }

    public String getBackend() {
        return backend;
    }

    public Set<IncomingEntryPointBackendServer> getServers() {
        return servers;
    }
}
