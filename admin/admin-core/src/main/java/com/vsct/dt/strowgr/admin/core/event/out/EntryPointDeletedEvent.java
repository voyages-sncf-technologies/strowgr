package com.vsct.dt.strowgr.admin.core.event.out;

import com.vsct.dt.strowgr.admin.core.EntryPointKey;
import com.vsct.dt.strowgr.admin.core.configuration.IncomingEntryPointBackendServer;
import com.vsct.dt.strowgr.admin.core.event.in.EntryPointEvent;

import java.util.Set;

public class EntryPointDeletedEvent extends EntryPointEvent {

    public EntryPointDeletedEvent(String correlationId, EntryPointKey key, String backend, Set<IncomingEntryPointBackendServer> servers) {
        super(correlationId, key);
    }

}
