package com.vsct.dt.haas.admin.core.configuration;

import com.google.common.collect.ImmutableMap;

import java.util.HashMap;
import java.util.Map;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.vsct.dt.haas.admin.Preconditions.checkStringNotEmpty;

public class EntryPointBackendServer {

    private final String id;
    private final String hostname;
    private final String ip;
    private final String port;

    private final HashMap<String, String> context;
    private final HashMap<String, String> userProvidedContext;

    public EntryPointBackendServer(String id, String hostname, String ip, String port) {
        this(id, hostname, ip, port, new HashMap<>(), new HashMap<>());
    }

    public EntryPointBackendServer(String id, String hostname, String ip, String port, Map<String, String> context) {
        this(id, hostname, ip, port, context, new HashMap<>());
    }

    public EntryPointBackendServer(String id, String hostname, String ip, String port, Map<String, String> context, Map<String, String> userProvidedContext) {
        this.id = checkStringNotEmpty(id, "Backend should have an id");
        this.hostname = checkStringNotEmpty(hostname, "Backend should have a hostname");
        this.ip = checkStringNotEmpty(ip, "Backend should have an ip");
        this.port = checkStringNotEmpty(port, "Backend should have a port");
        this.context = new HashMap<>(checkNotNull(context));
        this.userProvidedContext = new HashMap<>(checkNotNull(userProvidedContext));
    }

    public String getHostname() {
        return hostname;
    }

    public String getId() {
        return id;
    }

    public String getIp() {
        return ip;
    }

    public String getPort() {
        return port;
    }

    public Map<String, String> getContext() {
        return new HashMap<>(context);
    }

    public HashMap<String, String> getUserProvidedContext() {
        return userProvidedContext;
    }

    public EntryPointBackendServer put(String key, String value) {
        ImmutableMap<String, String> context = ImmutableMap.<String, String>builder().put(key, value).putAll(this.context).build();
        return new EntryPointBackendServer(this.id, this.hostname, this.ip, this.port, context);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        EntryPointBackendServer that = (EntryPointBackendServer) o;

        if (id != null ? !id.equals(that.id) : that.id != null) return false;
        if (hostname != null ? !hostname.equals(that.hostname) : that.hostname != null) return false;
        if (ip != null ? !ip.equals(that.ip) : that.ip != null) return false;
        if (port != null ? !port.equals(that.port) : that.port != null) return false;
        if (context != null ? !context.equals(that.context) : that.context != null) return false;
        if (userProvidedContext != null ? !userProvidedContext.equals(that.userProvidedContext) : that.userProvidedContext != null)
            return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = id != null ? id.hashCode() : 0;
        result = 31 * result + (hostname != null ? hostname.hashCode() : 0);
        result = 31 * result + (ip != null ? ip.hashCode() : 0);
        result = 31 * result + (port != null ? port.hashCode() : 0);
        result = 31 * result + (context != null ? context.hashCode() : 0);
        result = 31 * result + (userProvidedContext != null ? userProvidedContext.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "EntryPointBackendServer{" +
                "id='" + id + '\'' +
                ", hostname='" + hostname + '\'' +
                ", ip='" + ip + '\'' +
                ", port='" + port + '\'' +
                ", context=" + context +
                '}';
    }
}
