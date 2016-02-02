package com.vsct.dt.haas.state;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

import java.util.*;
import java.util.stream.Collectors;

/**
 * IMMUTABLE BACKEND
 * EQUALS WITH NAME
 */
public class Backend {

    private final String name;
    private final ImmutableSet<Server> servers;
    private final ImmutableMap<String, String> context;

    public Backend(String name) {
        this.name = name;
        this.servers = ImmutableSet.of();
        this.context = ImmutableMap.of();
    }

    private Backend(String name, ImmutableSet<Server> servers, ImmutableMap<String, String> context){
        this.name = name;
        this.servers = servers;
        this.context = context;
    }

    public String getName() {
        return name;
    }

    public Backend addServer(Server server) {
        ImmutableSet<Server> servers = ImmutableSet.<Server>builder().add(server).addAll(this.servers).build();
        return new Backend(this.name, servers, context);
    }

    public Map<String, Object> toMustacheScope(){
        HashMap<String, Object> scope = new HashMap<>();

        scope.put("name", name);

        scope.putAll(context);

        Set<Object> servers = this.servers.stream().map(Server::toMustacheScope).collect(Collectors.toSet());
        scope.put("servers", servers);

        return scope;
    }

    public Optional<Server> getServer(String name) {
        for(Server s : servers){
            if(s.getName().equals(name)){
                return Optional.of(s);
            }
        }
        return Optional.empty();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Backend backend = (Backend) o;

        if (name != null ? !name.equals(backend.name) : backend.name != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return name != null ? name.hashCode() : 0;
    }
}
