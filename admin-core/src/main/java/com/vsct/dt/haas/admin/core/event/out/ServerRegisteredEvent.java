package com.vsct.dt.haas.admin.core.event.out;

import com.google.common.collect.ImmutableSet;
import com.vsct.dt.haas.admin.core.EntryPointKey;
import com.vsct.dt.haas.admin.core.configuration.EntryPointBackendServer;
import com.vsct.dt.haas.admin.core.event.in.EntryPointEvent;

import java.util.Set;

public class ServerRegisteredEvent extends EntryPointEvent {
    private final String backend;
    private final ImmutableSet<EntryPointBackendServer> servers;

    public ServerRegisteredEvent(String correlationId, EntryPointKey key, String backend, Set<EntryPointBackendServer> servers) {
        super(correlationId, key);
        this.backend = backend;
        this.servers = ImmutableSet.copyOf(servers);
    }

    public String getBackend() {
        return backend;
    }

    public Set<EntryPointBackendServer> getServers() {
        return servers;
    }
}
