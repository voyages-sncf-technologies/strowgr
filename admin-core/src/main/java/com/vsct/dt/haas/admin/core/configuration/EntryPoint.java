package com.vsct.dt.haas.admin.core.configuration;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.vsct.dt.haas.admin.core.*;
import com.vsct.dt.haas.admin.core.event.in.UpdatedEntryPoint;
import com.vsct.dt.haas.admin.core.event.in.UpdatedEntryPointBackend;

import java.util.*;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.vsct.dt.haas.admin.Preconditions.*;

public class EntryPoint {

    public static final String SYSLOG_PORT_ID = "syslog";

    private final String haproxy;

    private final String hapUser;

    private final HashMap<String, String> context;

    private final HashMap<String, EntryPointFrontend> frontends;
    private final HashMap<String, EntryPointBackend> backends;

    public EntryPoint(String haproxy, String hapUser,
                      Set<EntryPointFrontend> frontends, Set<EntryPointBackend> backends, Map<String, String> context) {
        this.haproxy = checkStringNotEmpty(haproxy, "EntryPointConfiguration should have an haproxy id");
        this.hapUser = checkStringNotEmpty(hapUser, "EntryPointConfiguration should have a user for haproxy");

        checkNotNull(frontends);
        this.frontends = new HashMap<>();
        for (EntryPointFrontend f : frontends) {
            this.frontends.put(f.getId(), f);
        }

        checkNotNull(backends);
        this.backends = new HashMap<>();
        for (EntryPointBackend b : backends) {
            this.backends.put(b.getId(), b);
        }

        this.context = new HashMap<>(checkNotNull(context));
    }

    private EntryPoint(String haproxy, String hapUser,
                       HashMap<String, EntryPointFrontend> frontends, HashMap<String, EntryPointBackend> backends, HashMap<String, String> context) {
        this.haproxy = haproxy;
        this.hapUser = hapUser;
        this.frontends = frontends;
        this.backends = backends;
        this.context = context;
    }

    public static IHapUSer onHaproxy(String haproxy) {
        return new EntryPoint.Builder(haproxy);
    }

    public EntryPoint addOrReplaceBackend(EntryPointBackend backend) {
        checkNotNull(backend);
        HashMap<String, EntryPointBackend> newBackends = new HashMap<>(backends);
        newBackends.put(backend.getId(), backend);
        return new EntryPoint(this.haproxy, this.hapUser, this.frontends, newBackends, this.context);
    }

    public Optional<EntryPointBackend> getBackend(String id) {
        return Optional.ofNullable(backends.get(id));
    }

    public EntryPoint addServer(String backendId, IncomingEntryPointBackendServer server) {
        checkNotNull(server);
        Optional<EntryPointBackendServer> existingServer = findServer(server.getId());
        EntryPointBackendServer newServer = existingServer
                .map(es -> new EntryPointBackendServer(server.getId(), server.getHostname(), server.getIp(), server.getPort(), server.getContext(), es.getContextOverride()))
                .orElseGet(() -> new EntryPointBackendServer(server.getId(), server.getHostname(), server.getIp(), server.getPort(), server.getContext(), new HashMap<String, String>()));

        EntryPoint configuration = this.removeServer(server.getId());

        EntryPointBackend backend = getBackend(backendId)
                .map(b -> b.addOrReplaceServer(newServer))
                .orElse(new EntryPointBackend(backendId, Sets.newHashSet(newServer), new HashMap<>()));

        return configuration.addOrReplaceBackend(backend);
    }

    private EntryPoint removeServer(String serverId) {
        for (EntryPointBackend backend : backends.values()) {
            Optional<EntryPointBackendServer> server = backend.getServer(serverId);
            if (server.isPresent()) {
                EntryPointBackend newBackend = backend.removeServer(serverId);
                return this.addOrReplaceBackend(newBackend);
            }
        }
        return this;
    }

    private Optional<EntryPointBackendServer> findServer(String serverId) {
        for (EntryPointBackend backend : backends.values()) {
            Optional<EntryPointBackendServer> server = backend.getServer(serverId);
            if (server.isPresent()) {
                return server;
            }
        }
        return Optional.empty();
    }

    public EntryPoint registerServers(String backendId, Collection<IncomingEntryPointBackendServer> servers) {
        EntryPoint configuration = this;
        for (IncomingEntryPointBackendServer server : servers) {
            configuration = configuration.addServer(backendId, server);
        }
        return configuration;
    }

    public String generateHaproxyConfiguration(EntryPointKey entryPointKey, TemplateLocator templateLocator, TemplateGenerator templateGenerator, PortProvider portProvider) throws IncompleteConfigurationException {
        String template = templateLocator.readTemplate(this);
        Map<String, Integer> portsMapping = getOrCreatePortsMapping(entryPointKey, portProvider, this);
        return templateGenerator.generate(template, this, portsMapping);
    }

    private Map<String, Integer> getOrCreatePortsMapping(EntryPointKey key, PortProvider portProvider, EntryPoint entryPoint) {
        Map<String, Integer> portsMapping = new HashMap<>();
        String prefix = key.getID() + '-';

        int syslogPort = portProvider.getPort(prefix + entryPoint.syslogPortId()).orElseGet(() -> portProvider.newPort(prefix + entryPoint.syslogPortId()));
        portsMapping.put(entryPoint.syslogPortId(), syslogPort);

        for (EntryPointFrontend frontend : entryPoint.getFrontends()) {
            int frontendPort = portProvider.getPort(prefix + frontend.portId()).orElseGet(() -> portProvider.newPort(prefix + frontend.portId()));
            portsMapping.put(frontend.portId(), frontendPort);
        }

        return portsMapping;
    }

    public String getHapUser() {
        return hapUser;
    }

    public String getHaproxy() {
        return haproxy;
    }

    public Map<String, String> getContext() {
        return new HashMap<>(context);
    }

    public Set<EntryPointFrontend> getFrontends() {
        return new HashSet<>(frontends.values());
    }

    public Set<EntryPointBackend> getBackends() {
        return new HashSet<>(backends.values());
    }

    public String syslogPortId() {
        return SYSLOG_PORT_ID;
    }

    /**
     * Merging rules :
     *  - Updated global context replaces existing one
     *  - Updated syslog user replaces existing one
     *  - Updated frontends replace existing ones, without consideration for ports
     *  - Updated backends replace existing ones
     *  - Updated servers just replace overrideConfiguration for existing ones
     * @param updatedEntryPoint
     * @return A new EntryPoint, result of the merge of this entrypoint and the updatedconfiguration
     */
    public EntryPoint mergeWithUpdate(UpdatedEntryPoint updatedEntryPoint) {

        Set<EntryPointBackend> newBackends = new HashSet<>();
        for(UpdatedEntryPointBackend updatedBackend : updatedEntryPoint.getBackends()){
            EntryPointBackend thisBackend = this.backends.get(updatedBackend.getId());
            if(thisBackend != null){
                Set<EntryPointBackendServer> newServers = new HashSet<>();
                for(EntryPointBackendServer s : thisBackend.getServers()){
                    Map<String, String> contextOverride = updatedBackend.getServer(s.getId()).map(updatedServer -> updatedServer.getContextOverride()).orElse(new HashMap<>());
                    newServers.add(new EntryPointBackendServer(s.getId(), s.getHostname(), s.getIp(), s.getPort(), s.getContext(), contextOverride));
                }
                newBackends.add(new EntryPointBackend(updatedBackend.getId(), newServers, updatedBackend.getContext()));
            } else {
                newBackends.add(new EntryPointBackend(updatedBackend.getId(), new HashSet<EntryPointBackendServer>(), updatedBackend.getContext()));
            }
        }

        return EntryPoint.onHaproxy(this.haproxy)
                .withUser(updatedEntryPoint.getHapUser())
                .definesFrontends(updatedEntryPoint.getFrontends().stream().map(f -> new EntryPointFrontend(f.getId(), f.getContext())).collect(Collectors.toSet()))
                .definesBackends(newBackends)
                .withGlobalContext(updatedEntryPoint.getContext())
                .build();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        EntryPoint that = (EntryPoint) o;

        if (backends != null ? !backends.equals(that.backends) : that.backends != null) return false;
        if (context != null ? !context.equals(that.context) : that.context != null) return false;
        if (frontends != null ? !frontends.equals(that.frontends) : that.frontends != null) return false;
        if (hapUser != null ? !hapUser.equals(that.hapUser) : that.hapUser != null) return false;
        if (haproxy != null ? !haproxy.equals(that.haproxy) : that.haproxy != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = haproxy != null ? haproxy.hashCode() : 0;
        result = 31 * result + (hapUser != null ? hapUser.hashCode() : 0);
        result = 31 * result + (context != null ? context.hashCode() : 0);
        result = 31 * result + (frontends != null ? frontends.hashCode() : 0);
        result = 31 * result + (backends != null ? backends.hashCode() : 0);
        return result;
    }

    public interface IHapUSer {
        public IFrontends withUser(String user);
    }

    public interface IFrontends {
        IBackends definesFrontends(Set<EntryPointFrontend> frontends);
    }

    public interface IBackends {
        IContext definesBackends(Set<EntryPointBackend> backends);
    }

    public interface IContext {
        IBuild withGlobalContext(Map<String, String> context);
    }

    public interface IBuild {
        EntryPoint build();
    }

    public static class Builder implements IHapUSer, IFrontends, IBackends, IContext, IBuild {

        private Set<EntryPointBackend> backends;
        private Set<EntryPointFrontend> frontends;
        private String haproxy;
        private String user;
        private String syslogPort;
        private Map<String, String> context;

        private Builder(String haproxy) {
            this.haproxy = haproxy;
        }

        @Override
        public IContext definesBackends(Set<EntryPointBackend> backends) {
            this.backends = backends;
            return this;
        }

        @Override
        public IBackends definesFrontends(Set<EntryPointFrontend> frontends) {
            this.frontends = frontends;
            return this;
        }

        @Override
        public IFrontends withUser(String user) {
            this.user = user;
            return this;
        }

        @Override
        public IBuild withGlobalContext(Map<String, String> context) {
            this.context = context;
            return this;
        }

        @Override
        public EntryPoint build() {
            return new EntryPoint(haproxy, user, frontends, backends, context);
        }

    }

}
