/*
 * Copyright (C) 2016 VSCT
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.vsct.dt.strowgr.admin.core.event.in;

import com.google.common.collect.ImmutableSet;
import com.vsct.dt.strowgr.admin.core.EntryPointKey;
import com.vsct.dt.strowgr.admin.core.configuration.IncomingEntryPointBackendServer;

import java.util.Set;

/**
 * Created by william_montaz on 05/02/2016.
 */
public class RegisterServerEvent extends EntryPointEvent {

    private final String backend;
    private final ImmutableSet<IncomingEntryPointBackendServer> servers;

    public RegisterServerEvent(String correlationId, EntryPointKey key, String backend, Set<IncomingEntryPointBackendServer> servers) {
        super(correlationId, key);
        this.backend = backend;
        this.servers = ImmutableSet.copyOf(servers);
    }

    public String getBackend() {
        return backend;
    }

    public Set<IncomingEntryPointBackendServer> getServers() {
        return servers;
    }

}
