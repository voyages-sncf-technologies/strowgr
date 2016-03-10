package com.vsct.dt.haas.admin.core.event.in;

import com.vsct.dt.haas.admin.core.EntryPointKey;
import java.util.Map;
import java.util.Set;

/**
 * Created by william_montaz on 09/02/2016.
 */
public class UpdateEntryPointEvent extends EntryPointEvent {

    private final Map<String, String>              globalContext;
    private final Map<String, Map<String, String>> frontendContexts;
    private final Map<String, Map<String, String>> backendContexts;
    private final Map<String, Map<String, String>> serverContexts;
    private final Set<String>                      frontendsToRemove;
    private final Set<String>                      backendsToRemove;
    private final Set<String>                      serversToRemove;

    public UpdateEntryPointEvent(String correlationId, EntryPointKey key,
                                 Map<String, String> globalContext,
                                 Map<String, Map<String, String>> frontendContexts,
                                 Map<String, Map<String, String>> backendContexts,
                                 Map<String, Map<String, String>> serverContexts,
                                 Set<String> frontendsToRemove,
                                 Set<String> backendsToRemove,
                                 Set<String> serversToRemove) {
        super(correlationId, key);
        this.globalContext = globalContext;
        this.frontendContexts = frontendContexts;
        this.backendContexts = backendContexts;
        this.serverContexts = serverContexts;
        this.frontendsToRemove = frontendsToRemove;
        this.backendsToRemove = backendsToRemove;
        this.serversToRemove = serversToRemove;
    }

    public Map<String, String> getGlobalContext() {
        return globalContext;
    }

    public Map<String, Map<String, String>> getFrontendContexts() {
        return frontendContexts;
    }

    public Map<String, Map<String, String>> getBackendContexts() {
        return backendContexts;
    }

    public Map<String, Map<String, String>> getServerContexts() {
        return serverContexts;
    }

    public Set<String> getFrontendsToRemove() {
        return frontendsToRemove;
    }

    public Set<String> getBackendsToRemove() {
        return backendsToRemove;
    }

    public Set<String> getServersToRemove() {
        return serversToRemove;
    }
}
