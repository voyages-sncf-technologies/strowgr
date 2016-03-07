package com.vsct.dt.haas.admin.template.generator;

import com.vsct.dt.haas.admin.core.configuration.EntryPointBackend;
import com.vsct.dt.haas.admin.core.configuration.EntryPointBackendServer;
import com.vsct.dt.haas.admin.core.configuration.EntryPointConfiguration;
import com.vsct.dt.haas.admin.core.configuration.EntryPointFrontend;

import java.security.Provider;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/* Transform an haproxy configuration into a mustache scope
* Look at test to see examples
* frontends, backends and servers are ordered by id when available as list */
public class HaasMustacheScope extends HashMap<String, Object> {

    private final Map<String, Integer> portsMapping;

    public HaasMustacheScope(EntryPointConfiguration configuration, Map<String, Integer> portsMapping) {
        super();
        this.portsMapping = portsMapping;

        /* Put all context first which guaranties essential properties of a configuration are not overridden */
        this.putAll(configuration.getContext());
        this.put("hap_user", configuration.getHapUser());
        this.put("syslog_port", getPort(configuration.syslogPortId()));

        Map<String, Object> frontend = configuration.getFrontends().stream().sorted((f1, f2) -> f1.getId().compareTo(f2.getId())).collect(Collectors.toMap(EntryPointFrontend::getId, this::toMustacheScope));
        this.put("frontend", frontend);

        Map<String, Object> backend = configuration.getBackends().stream().sorted((b1, b2) -> b1.getId().compareTo(b2.getId())).collect(Collectors.toMap(EntryPointBackend::getId, this::toMustacheScope));
        this.put("backend", backend);
    }

    public Map<String, Object> toMustacheScope(EntryPointFrontend frontend) {
        HashMap<String, Object> scope = new HashMap<>();
        /* Put all context first which guaranties essential properties of a frontend are not overridden */
        scope.putAll(frontend.getContext());
        scope.put("id", frontend.getId());
        scope.put("port", getPort(frontend.portId()));

        return scope;
    }

    public Map<String, Object> toMustacheScope(EntryPointBackend backend) {
        HashMap<String, Object> scope = new HashMap<>();

        /* Put all context first which guaranties essential properties of a backend are not overridden */
        scope.putAll(backend.getContext());

        scope.put("id", backend.getId());

        Set<Object> servers = backend.getServers().stream().sorted((s1, s2) -> s1.getId().compareTo(s2.getId())).map(this::toMustacheScope).collect(Collectors.toSet());
        scope.put("servers", servers);

        return scope;
    }

    public Map<String, Object> toMustacheScope(EntryPointBackendServer server) {
        HashMap<String, Object> scope = new HashMap<>();

        /* Put all context first which guaranties essential properties of a server are not overridden */
        scope.putAll(server.getContext());
        /* Put all user provided context, ensuring overrides of the user provided values */
        scope.putAll(server.getUserProvidedContext());

        scope.put("id", server.getId());
        scope.put("hostname", server.getHostname());
        scope.put("ip", server.getIp());
        scope.put("port", server.getPort());

        return scope;
    }

    private int getPort(String id) {
        Integer port = portsMapping.get(id);
        if(port == null){
            throw new RuntimeException("Impossible to find a port for "+id+" It should have been provided by the caller in the portsMapping hashmap");
        }
        return port;
    }

}
