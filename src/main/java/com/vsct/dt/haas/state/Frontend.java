package com.vsct.dt.haas.state;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.ImmutableMap;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * IMMUTABLE FRONTEND
 * EQUALS WITH NAME
 */
public class Frontend {

    private final String name;
    private final String port;

    private final ImmutableMap<String, String> context;

    @JsonCreator
    public Frontend(@JsonProperty("name") String name, @JsonProperty("port") String port) {
        this.name = name;
        this.port = port;
        this.context = ImmutableMap.of();
    }

    public String getName() {
        return name;
    }

    public String getPort() {
        return port;
    }

    public ImmutableMap<String, String> getContext() {
        return context;
    }

    public Map<String, Object> toMustacheScope(){
        HashMap<String, Object> scope = new HashMap<>();

        scope.put("name", name);
        scope.put("port", port);

        scope.putAll(context);

        return scope;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Frontend frontend = (Frontend) o;

        if (name != null ? !name.equals(frontend.name) : frontend.name != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return name != null ? name.hashCode() : 0;
    }
}
