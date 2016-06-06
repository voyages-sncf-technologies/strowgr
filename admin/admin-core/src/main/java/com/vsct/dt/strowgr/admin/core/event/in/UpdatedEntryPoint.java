package com.vsct.dt.strowgr.admin.core.event.in;

import java.util.*;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.vsct.dt.strowgr.admin.Preconditions.checkStringNotEmpty;

public class UpdatedEntryPoint {

    private final String hapUser;

    private final HashMap<String, String> context;

    private final HashMap<String, UpdatedEntryPointFrontend> frontends;
    private final HashMap<String, UpdatedEntryPointBackend>  backends;

    public UpdatedEntryPoint(String hapUser, Map<String, String> context, Set<UpdatedEntryPointFrontend> frontends, Set<UpdatedEntryPointBackend> backends) {
        this.hapUser = checkStringNotEmpty(hapUser, "hapUser must be provided");
        this.context = new HashMap<>(checkNotNull(context));

        this.frontends = new HashMap<>();
        for(UpdatedEntryPointFrontend f : checkNotNull(frontends)){
            this.frontends.put(f.getId(), f);
        }

        this.backends = new HashMap<>();
        for(UpdatedEntryPointBackend b : checkNotNull(backends)){
            this.backends.put(b.getId(), b);
        }
    }

    public String getHapUser() {
        return hapUser;
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
}
