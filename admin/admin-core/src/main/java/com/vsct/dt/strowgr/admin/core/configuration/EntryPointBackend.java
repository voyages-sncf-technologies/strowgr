package com.vsct.dt.strowgr.admin.core.configuration;

import java.util.*;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.vsct.dt.strowgr.admin.Preconditions.*;

public class EntryPointBackend {

    private final String id;
    private final HashMap<String, EntryPointBackendServer> servers;
    private final HashMap<String, String> context;

    public EntryPointBackend(String id) {
        this(id, new HashSet<>(), new HashMap<>());
    }

    public EntryPointBackend(String id, Set<EntryPointBackendServer> servers, Map<String, String> context) {
        this.id = checkStringNotEmpty(id, "Backend should have an id");

        checkNotNull(servers);
        this.servers = new HashMap<>();
        for (EntryPointBackendServer s : servers) {
            this.servers.put(s.getId(), s);
        }

        this.context = new HashMap<>(checkNotNull(context));
    }

    /* PRIVATE so no check on null, no defensive copy, it should be handled by the calling code */
    private EntryPointBackend(String id, HashMap<String, EntryPointBackendServer> servers, HashMap<String, String> context) {
        this.id = id;
        this.servers = servers;
        this.context = context;
    }

    public String getId() {
        return id;
    }

    public Set<EntryPointBackendServer> getServers() {
        return new HashSet<>(servers.values());
    }

    public Map<String, String> getContext() {
        return new HashMap<>(context);
    }

    public Optional<EntryPointBackendServer> getServer(String name) {
        return Optional.ofNullable(servers.get(name));
    }

    public EntryPointBackend addOrReplaceServer(EntryPointBackendServer server) {
        checkNotNull(server);
        HashMap<String, EntryPointBackendServer> newServersMap = new HashMap<>(this.servers);
        newServersMap.put(server.getId(), server);
        return new EntryPointBackend(this.id, newServersMap, context);
    }

    public EntryPointBackend removeServer(String serverId) {
        HashMap<String, EntryPointBackendServer> newServersMap = new HashMap<>(this.servers);
        newServersMap.remove(serverId);
        return new EntryPointBackend(this.id, newServersMap, context);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        EntryPointBackend that = (EntryPointBackend) o;

        if (context != null ? !context.equals(that.context) : that.context != null) return false;
        if (id != null ? !id.equals(that.id) : that.id != null) return false;
        if (servers != null ? !servers.equals(that.servers) : that.servers != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = id != null ? id.hashCode() : 0;
        result = 31 * result + (servers != null ? servers.hashCode() : 0);
        result = 31 * result + (context != null ? context.hashCode() : 0);
        return result;
    }

}
