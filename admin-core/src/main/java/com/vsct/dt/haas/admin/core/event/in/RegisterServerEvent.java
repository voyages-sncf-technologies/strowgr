package com.vsct.dt.haas.admin.core.event.in;

import com.google.common.collect.ImmutableSet;
import com.vsct.dt.haas.admin.core.EntryPointKey;
import com.vsct.dt.haas.admin.core.configuration.EntryPointBackendServer;
import com.vsct.dt.haas.admin.core.configuration.IncomingEntryPointBackendServer;

import java.util.Set;

/**
 * Created by william_montaz on 05/02/2016.
 */
public class RegisterServerEvent extends EntryPointEvent {

    private final String                                        backend;
    private final ImmutableSet<IncomingEntryPointBackendServer> servers;

    public RegisterServerEvent(String correlationId, EntryPointKey key, String backend, Set<IncomingEntryPointBackendServer> servers) {
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
