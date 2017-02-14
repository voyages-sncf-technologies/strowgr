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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.vsct.dt.strowgr.admin.Preconditions.checkStringNotEmpty;

public class UpdatedEntryPoint {

    private final String hapUser;
    private final String hapVersion;
    private final int bindingId;

    private final HashMap<String, String> context;

    private final HashMap<String, UpdatedEntryPointFrontend> frontends;
    private final HashMap<String, UpdatedEntryPointBackend> backends;

    public UpdatedEntryPoint(int bindingId, String hapUser, Map<String, String> context, Set<UpdatedEntryPointFrontend> frontends, Set<UpdatedEntryPointBackend> backends, String hapVersion) {
        this.bindingId = bindingId;
        this.hapUser = checkStringNotEmpty(hapUser, "hapUser must be provided");
        this.context = new HashMap<>(checkNotNull(context));
        this.hapVersion = hapVersion;

        this.frontends = new HashMap<>();
        for (UpdatedEntryPointFrontend f : checkNotNull(frontends)) {
            this.frontends.put(f.getId(), f);
        }

        this.backends = new HashMap<>();
        for (UpdatedEntryPointBackend b : checkNotNull(backends)) {
            this.backends.put(b.getId(), b);
        }
    }

    public String getHapUser() {
        return hapUser;
    }

    public String getHapVersion() {
        return hapVersion;
    }

    public HashMap<String, String> getContext() {
        return new HashMap<>(context);
    }

    public Set<UpdatedEntryPointFrontend> getFrontends() {
        return new HashSet<>(frontends.values());
    }

    public Set<UpdatedEntryPointBackend> getBackends() {
        return new HashSet<>(backends.values());
    }

    public int getBindingId() {
        return bindingId;
    }
}
