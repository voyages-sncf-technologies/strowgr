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

package com.vsct.dt.strowgr.admin.core.event.in;

import java.util.*;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.vsct.dt.strowgr.admin.Preconditions.checkStringNotEmpty;

/**
 * Created by william_montaz on 12/04/2016.
 */
public class UpdatedEntryPointBackend {

    private final String id;
    private final HashMap<String, UpdatedEntryPointBackendServer> servers;
    private final HashMap<String, String> context;

    public UpdatedEntryPointBackend(String id, Set<UpdatedEntryPointBackendServer> servers, Map<String, String> context) {
        this.id = checkStringNotEmpty(id, "Updated Backend should have an id");

        this.servers = new HashMap<>();
        for (UpdatedEntryPointBackendServer s : checkNotNull(servers)) {
            this.servers.put(s.getId(), s);
        }

        this.context = new HashMap<>(checkNotNull(context));
    }

    public String getId() {
        return id;
    }

    public Set<UpdatedEntryPointBackendServer> getServers() {
        return new HashSet<>(servers.values());
    }

    public Optional<UpdatedEntryPointBackendServer> getServer(String id) {
        return Optional.ofNullable(servers.get(id));
    }

    public HashMap<String, String> getContext() {
        return new HashMap<>(context);
    }
}
