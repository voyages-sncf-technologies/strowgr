package com.vsct.dt.haas.admin.template.generator;

import com.vsct.dt.haas.admin.core.configuration.EntryPointBackend;
import com.vsct.dt.haas.admin.core.configuration.EntryPointBackendServer;
import com.vsct.dt.haas.admin.core.configuration.EntryPointConfiguration;
import com.vsct.dt.haas.admin.core.configuration.EntryPointFrontend;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class HaasMustacheScope extends HashMap<String, Object> {

    public HaasMustacheScope(EntryPointConfiguration configuration) {
        super();
        /* Put all context first which guaranties essential properties of a configuration are not overridden */
        this.putAll(configuration.getContext());
        this.put("hap_user", configuration.getHapUser());
        this.put("syslog_port", configuration.getSyslogPort());

        Map<String, Object> frontend = configuration.getFrontends().stream().collect(Collectors.toMap(EntryPointFrontend::getId, this::toMustacheScope));
        this.put("frontend", frontend);

        Map<String, Object> backend = configuration.getBackends().stream().collect(Collectors.toMap(EntryPointBackend::getId, this::toMustacheScope));
        this.put("backend", backend);
    }

    public Map<String, Object> toMustacheScope(EntryPointFrontend frontend) {
        HashMap<String, Object> scope = new HashMap<>();
        /* Put all context first which guaranties essential properties of a frontend are not overridden */
        scope.putAll(frontend.getContext());
        scope.put("id", frontend.getId());
        scope.put("port", frontend.getPort());

        return scope;
    }

    public Map<String, Object> toMustacheScope(EntryPointBackend backend) {
        HashMap<String, Object> scope = new HashMap<>();

        /* Put all context first which guaranties essential properties of a backend are not overridden */
        scope.putAll(backend.getContext());

        scope.put("id", backend.getId());

        Set<Object> servers = backend.getServers().stream().map(this::toMustacheScope).collect(Collectors.toSet());
        scope.put("servers", servers);

        return scope;
    }

    public Map<String, Object> toMustacheScope(EntryPointBackendServer server) {
        HashMap<String, Object> scope = new HashMap<>();

        /* Put all context first which guaranties essential properties of a server are not overridden */
        scope.putAll(server.getContext());

        scope.put("id", server.getId());
        scope.put("hostname", server.getHostname());
        scope.put("ip", server.getIp());
        scope.put("port", server.getPort());

        return scope;
    }

}
