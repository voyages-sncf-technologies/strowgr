package com.vsct.dt.strowgr.admin.template.generator;

import com.vsct.dt.strowgr.admin.core.configuration.EntryPointBackend;
import com.vsct.dt.strowgr.admin.core.configuration.EntryPointBackendServer;
import com.vsct.dt.strowgr.admin.core.configuration.EntryPoint;
import com.vsct.dt.strowgr.admin.core.configuration.EntryPointFrontend;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Transform an haproxy configuration into a mustache scope.
 * Look at test to see examples.
 * frontends, backends and servers are ordered by id when available as list.
 */
public class StrowgrMustacheScope extends HashMap<String, Object> {

    private final Map<String, Integer> portsMapping;

    public StrowgrMustacheScope(EntryPoint configuration, Map<String, Integer> portsMapping) {
        super();
        this.portsMapping = portsMapping;

        /* Put all context first which guaranties essential properties of a configuration are not overridden */
        this.putAll(configuration.getContext());
        this.put("hap_user", configuration.getHapUser());
        getPort(configuration.syslogPortId()).ifPresent(p -> this.put("syslog_port", p));

        Map<String, Object> frontend = configuration.getFrontends().stream().sorted((f1, f2) -> f1.getId().compareTo(f2.getId())).collect(Collectors.toMap(EntryPointFrontend::getId, this::toMustacheScope));
        this.put("frontend", frontend);
        this.put("frontends", frontend.values());

        Map<String, Object> backend = configuration.getBackends().stream().sorted((b1, b2) -> b1.getId().compareTo(b2.getId())).collect(Collectors.toMap(EntryPointBackend::getId, this::toMustacheScope));
        this.put("backend", backend);
        this.put("backends", backend.values());
    }

    public Map<String, Object> toMustacheScope(EntryPointFrontend frontend) {
        HashMap<String, Object> scope = new HashMap<>();
        /* Put all context first which guaranties essential properties of a frontend are not overridden */
        scope.putAll(frontend.getContext());
        scope.put("id", frontend.getId());
        getPort(frontend.portId()).ifPresent(p -> scope.put("port", p));

        return scope;
    }

    public Map<String, Object> toMustacheScope(EntryPointBackend backend) {
        HashMap<String, Object> scope = new HashMap<>();

        /* Put all context first which guaranties essential properties of a backend are not overridden */
        scope.putAll(backend.getContext());

        scope.put("id", backend.getId());

        Set<Object> servers = backend.getServers().stream().sorted((s1, s2) -> s1.getId().compareTo(s2.getId())).map(this::toMustacheScope).collect(Collectors.toCollection(LinkedHashSet::new));
        scope.put("servers", servers);

        return scope;
    }

    public Map<String, Object> toMustacheScope(EntryPointBackendServer server) {
        HashMap<String, Object> scope = new HashMap<>();

        /* Put all context first which guaranties essential properties of a server are not overridden */
        scope.putAll(server.getContext());
        /* Put all user provided context, ensuring overrides of the user provided values */
        scope.putAll(server.getContextOverride());

        scope.put("id", server.getId());
        scope.put("hostname", server.getHostname());
        scope.put("ip", server.getIp());
        scope.put("port", server.getPort());

        return scope;
    }

    private Optional<Integer> getPort(String id) {
        return Optional.ofNullable(portsMapping.get(id));
    }

}
