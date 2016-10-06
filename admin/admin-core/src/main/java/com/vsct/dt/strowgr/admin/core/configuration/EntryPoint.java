/*
 *  Copyright (C) 2016 VSCT
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package com.vsct.dt.strowgr.admin.core.configuration;

import com.google.common.collect.Sets;
import com.vsct.dt.strowgr.admin.core.event.in.UpdatedEntryPoint;
import com.vsct.dt.strowgr.admin.core.event.in.UpdatedEntryPointBackend;
import com.vsct.dt.strowgr.admin.core.event.in.UpdatedEntryPointBackendServer;

import java.util.*;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.vsct.dt.strowgr.admin.Preconditions.checkStringNotEmpty;

public class EntryPoint {

    public static final String SYSLOG_PORT_ID = "syslog";

    private final String haproxy;
    private final int    bindingId;

    private final String hapUser;

    private final HashMap<String, String> context;

    private final HashMap<String, EntryPointFrontend> frontends;
    private final HashMap<String, EntryPointBackend>  backends;

    public EntryPoint(String haproxy, int bindingId, String hapUser,
                      Set<EntryPointFrontend> frontends, Set<EntryPointBackend> backends, Map<String, String> context) {
        this.haproxy = checkStringNotEmpty(haproxy, "EntryPointConfiguration should have an haproxy id");
        this.bindingId = bindingId;
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

    private EntryPoint(String haproxy, int bindingId, String hapUser,
                       HashMap<String, EntryPointFrontend> frontends, HashMap<String, EntryPointBackend> backends, HashMap<String, String> context) {
        this.haproxy = haproxy;
        this.bindingId = bindingId;
        this.hapUser = hapUser;
        this.frontends = frontends;
        this.backends = backends;
        this.context = context;
    }

    public static IHapUSer onHaproxy(String haproxy, int bindingId) {
        return new EntryPoint.Builder(haproxy, bindingId);
    }

    public EntryPoint addOrReplaceBackend(EntryPointBackend backend) {
        checkNotNull(backend);
        HashMap<String, EntryPointBackend> newBackends = new HashMap<>(backends);
        newBackends.put(backend.getId(), backend);
        return new EntryPoint(this.haproxy, this.bindingId, this.hapUser, this.frontends, newBackends, this.context);
    }

    public Optional<EntryPointBackend> getBackend(String id) {
        return Optional.ofNullable(backends.get(id));
    }

    public EntryPoint addServer(String backendId, IncomingEntryPointBackendServer server) {
        checkNotNull(server);
        Optional<EntryPointBackendServer> existingServer = findServer(server.getId());
        EntryPointBackendServer newServer = existingServer
                .map(es -> new EntryPointBackendServer(server.getId(), server.getIp(), server.getPort(), server.getContext(), es.getContextOverride()))
                .orElseGet(() -> new EntryPointBackendServer(server.getId(), server.getIp(), server.getPort(), server.getContext(), new HashMap<>()));

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

    public String getHapUser() {
        return hapUser;
    }

    public String getHaproxy() {
        return haproxy;
    }

    public int getBindingId() {
        return bindingId;
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
     * - Updated global context replaces existing one
     * - Updated syslog user replaces existing one
     * - Updated frontends replace existing ones, without consideration for ports
     * - Updated backends replace existing ones
     * - Updated servers just replace overrideConfiguration for existing ones
     *
     * @param updatedEntryPoint
     * @return A new EntryPoint, result of the merge of this entrypoint and the updatedconfiguration
     */
    public EntryPoint mergeWithUpdate(UpdatedEntryPoint updatedEntryPoint) {

        Set<EntryPointBackend> newBackends = new HashSet<>();
        for (UpdatedEntryPointBackend updatedBackend : updatedEntryPoint.getBackends()) {
            EntryPointBackend thisBackend = this.backends.get(updatedBackend.getId());
            if (thisBackend != null) {
                Set<EntryPointBackendServer> newServers = new HashSet<>();
                for (EntryPointBackendServer s : thisBackend.getServers()) {
                    Map<String, String> contextOverride = updatedBackend.getServer(s.getId()).map(UpdatedEntryPointBackendServer::getContextOverride).orElse(new HashMap<>());
                    newServers.add(new EntryPointBackendServer(s.getId(), s.getIp(), s.getPort(), s.getContext(), contextOverride));
                }
                newBackends.add(new EntryPointBackend(updatedBackend.getId(), newServers, updatedBackend.getContext()));
            }
            else {
                newBackends.add(new EntryPointBackend(updatedBackend.getId(), new HashSet<>(), updatedBackend.getContext()));
            }
        }

        return EntryPoint.onHaproxy(this.haproxy, updatedEntryPoint.getBindingId())
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
        return bindingId == that.bindingId &&
                java.util.Objects.equals(haproxy, that.haproxy) &&
                java.util.Objects.equals(hapUser, that.hapUser) &&
                java.util.Objects.equals(context, that.context) &&
                java.util.Objects.equals(frontends, that.frontends) &&
                java.util.Objects.equals(backends, that.backends);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(haproxy, bindingId, hapUser, context, frontends, backends);
    }

    public interface IHapUSer {
        IFrontends withUser(String user);
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

        private final int                     bindingId;
        private       Set<EntryPointBackend>  backends;
        private       Set<EntryPointFrontend> frontends;
        private       String                  haproxy;
        private       String                  user;
        private       String                  syslogPort;
        private       Map<String, String>     context;

        private Builder(String haproxy, int bindingId) {
            this.haproxy = haproxy;
            this.bindingId = bindingId;
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
            return new EntryPoint(haproxy, bindingId, user, frontends, backends, context);
        }

    }

}
