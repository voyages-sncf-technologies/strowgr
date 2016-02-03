package com.vsct.dt.haas.state;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import sun.security.x509.X509CertInfo;

import java.util.*;
import java.util.stream.Collectors;

/**
 * IMMUTABLE ENTRYPOINT
 */
public class EntryPoint {

    private final String haproxy;

    private final String application;
    private final String platform;

    private final String hapUser;
    private final String syslogPort;

    private final ImmutableMap<String, String> context;

    private final ImmutableSet<Frontend> frontends;
    private final ImmutableSet<Backend> backends;

    @JsonCreator
    public EntryPoint(@JsonProperty("haproxy") String haproxy,
                      @JsonProperty("application") String application,
                      @JsonProperty("platform") String platform,
                      @JsonProperty("hapUser") String hapUser,
                      @JsonProperty("syslogPort") String syslogPort,
                      @JsonProperty("frontends") ImmutableSet<Frontend> frontends,
                      @JsonProperty("backends") ImmutableSet<Backend> backends) {
        this.haproxy = haproxy;
        this.application = application;
        this.platform = platform;
        this.hapUser = hapUser;
        this.syslogPort = syslogPort;
        this.frontends = frontends;
        this.backends = backends;
        this.context = ImmutableMap.of();
    }

    public EntryPoint(String haproxy, String application, String platform, String hapUser, String syslogPort) {
        this.haproxy = haproxy;
        this.application = application;
        this.platform = platform;
        this.hapUser = hapUser;
        this.syslogPort = syslogPort;
        this.frontends = ImmutableSet.of();
        this.backends = ImmutableSet.of();
        this.context = ImmutableMap.of();
    }

    private EntryPoint(String haproxy, String application, String platform, String hapUser, String syslogPort, ImmutableSet<Frontend> frontends, ImmutableSet<Backend> backends, ImmutableMap<String, String> context) {
        this.haproxy = haproxy;
        this.application = application;
        this.platform = platform;
        this.hapUser = hapUser;
        this.syslogPort = syslogPort;
        this.frontends = frontends;
        this.backends = backends;
        this.context = context;
    }

    public EntryPoint addFrontend(Frontend frontend) {
        ImmutableSet<Frontend> frontends = ImmutableSet.<Frontend>builder().add(frontend).addAll(this.frontends).build();
        return new EntryPoint(this.haproxy, this.application, this.platform, this.hapUser, this.syslogPort, frontends, this.backends, this.context);
    }

    public EntryPoint addBackend(Backend backend) {
        ImmutableSet<Backend> backends = ImmutableSet.<Backend>builder().add(backend).addAll(this.backends).build();
        return new EntryPoint(this.haproxy, this.application, this.platform, this.hapUser, this.syslogPort, this.frontends, backends, this.context);
    }

    public Optional<Backend> getBackend(String name) {
        for(Backend backend : backends){
            if(backend.getName().equals(name)){
                return Optional.of(backend);
            }
        }
        return Optional.empty();
    }

    public String getApplication() {
        return application;
    }

    public String getPlatform() {
        return platform;
    }

    public String getHapUser() {
        return hapUser;
    }

    public String getSyslogPort() {
        return syslogPort;
    }

    public String getHaproxy() {
        return haproxy;
    }

    public ImmutableMap<String, String> getContext() {
        return context;
    }

    public ImmutableSet<Frontend> getFrontends() {
        return frontends;
    }

    public ImmutableSet<Backend> getBackends() {
        return backends;
    }

    public EntryPoint addServer(String backendName, Server server) {
        Backend backend = getBackend(backendName).map(b -> b.addServer(server)).get();
        return this.addBackend(backend);
    }

    public EntryPoint addServerContext(String backendName, String serverName, String key, String value) {
        Server server = getBackend(backendName).flatMap(b -> b.getServer(serverName))
                .map(s -> s.put(key, value)).get();
        return this.addServer(backendName, server);
    }

    public HashMap<String, Object> toMustacheScope() {
        HashMap<String, Object> context = new HashMap<String, Object>();

        context.put("application", this.application);
        context.put("platform", this.platform);
        context.put("hap_user", this.hapUser);
        context.put("syslog_port", this.syslogPort);

        context.putAll(context);

        Map<String, Object> frontend = frontends.stream().collect(Collectors.toMap(Frontend::getName, Frontend::toMustacheScope));
        context.put("frontend", frontend);

        Map<String, Object> backend = backends.stream().collect(Collectors.toMap(Backend::getName, Backend::toMustacheScope));
        context.put("backend", backend);

        return context;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        EntryPoint that = (EntryPoint) o;

        if (application != null ? !application.equals(that.application) : that.application != null) return false;
        if (backends != null ? !backends.equals(that.backends) : that.backends != null) return false;
        if (context != null ? !context.equals(that.context) : that.context != null) return false;
        if (frontends != null ? !frontends.equals(that.frontends) : that.frontends != null) return false;
        if (hapUser != null ? !hapUser.equals(that.hapUser) : that.hapUser != null) return false;
        if (haproxy != null ? !haproxy.equals(that.haproxy) : that.haproxy != null) return false;
        if (platform != null ? !platform.equals(that.platform) : that.platform != null) return false;
        if (syslogPort != null ? !syslogPort.equals(that.syslogPort) : that.syslogPort != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = haproxy != null ? haproxy.hashCode() : 0;
        result = 31 * result + (application != null ? application.hashCode() : 0);
        result = 31 * result + (platform != null ? platform.hashCode() : 0);
        result = 31 * result + (hapUser != null ? hapUser.hashCode() : 0);
        result = 31 * result + (syslogPort != null ? syslogPort.hashCode() : 0);
        result = 31 * result + (context != null ? context.hashCode() : 0);
        result = 31 * result + (frontends != null ? frontends.hashCode() : 0);
        result = 31 * result + (backends != null ? backends.hashCode() : 0);
        return result;
    }
}
