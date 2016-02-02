package com.vsct.dt.haas.state;

import com.google.common.collect.ImmutableMap;

import java.util.HashMap;
import java.util.Map;

/**
 * IMMUTABLE SERVER
 * EQUALS WITH INSTANCE NAME
 */
public class Server {

    private final String instanceName;
    private final String name;
    private final String ip;
    private final String port;

    private final ImmutableMap<String, String> context;

    public Server(String instanceName, String name, String ip, String port) {
        this.instanceName = instanceName;
        this.name = name;
        this.port = port;
        this.ip = ip;
        this.context = ImmutableMap.of();
    }

    private Server(String instanceName, String name, String ip, String port, ImmutableMap<String, String> context) {
        this.instanceName = instanceName;
        this.name = name;
        this.port = port;
        this.ip = ip;
        this.context = context;
    }

    public String getName() {
        return name;
    }

    public Map<String, Object> toMustacheScope(){
        HashMap<String, Object> scope = new HashMap<>();

        scope.put("name", name);
        scope.put("ip", ip);
        scope.put("port", port);

        scope.putAll(context);

        return scope;
    }

    public Server put(String key, String value) {
        ImmutableMap<String, String> context = ImmutableMap.<String, String>builder().put(key, value).putAll(this.context).build();
        return new Server(this.instanceName, this.name, this.ip, this.port, context);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Server server = (Server) o;

        if (instanceName != null ? !instanceName.equals(server.instanceName) : server.instanceName != null)
            return false;

        return true;
    }

    @Override
    public int hashCode() {
        return instanceName != null ? instanceName.hashCode() : 0;
    }
}
