package com.vsct.dt.strowgr.admin.core.configuration;

import com.vsct.dt.strowgr.admin.core.event.in.UpdatedEntryPoint;
import com.vsct.dt.strowgr.admin.core.event.in.UpdatedEntryPointBackend;
import com.vsct.dt.strowgr.admin.core.event.in.UpdatedEntryPointBackendServer;
import com.vsct.dt.strowgr.admin.core.event.in.UpdatedEntryPointFrontend;
import org.fest.assertions.Assertions;
import org.junit.Test;

import java.util.HashMap;
import java.util.HashSet;

import static org.fest.assertions.Assertions.assertThat;

public class EntryPointTest {

    @Test
    public void should_merge_with_removed_entrypoint() {
        // given
        HashSet<EntryPointBackend> initBackends = new HashSet<>();
        {
            HashSet<EntryPointBackendServer> servers = new HashSet<>();
            servers.add(new EntryPointBackendServer("server1", "0.0.0.0", "80", new HashMap<>(), new HashMap<>()));
            servers.add(new EntryPointBackendServer("server2", "0.0.0.0", "80", new HashMap<>(), new HashMap<>()));
            servers.add(new EntryPointBackendServer("server3", "0.0.0.0", "80", new HashMap<>(), new HashMap<>()));
            initBackends.add(new EntryPointBackend("backend", servers, new HashMap<>()));
        }
        EntryPoint initialEP = EntryPoint.onHaproxy("haproxy", 1)
                .withUser("hapadm")
                .withVersion("0.0")
                .definesFrontends(new HashSet<>())
                .definesBackends(initBackends)
                .withGlobalContext(new HashMap<>())
                .build();

        HashSet<UpdatedEntryPointBackend> targetBackends = new HashSet<>();
        {
            HashSet<UpdatedEntryPointBackendServer> servers = new HashSet<>();
            servers.add(new UpdatedEntryPointBackendServer("server1", new HashMap<>()));
            servers.add(new UpdatedEntryPointBackendServer("server3", new HashMap<>()));
            targetBackends.add(new UpdatedEntryPointBackend("backend", servers, new HashMap<String, String>()));
        }

        UpdatedEntryPoint newEP = new UpdatedEntryPoint(1, "hapadm", new HashMap<String, String>(), new HashSet<UpdatedEntryPointFrontend>(), targetBackends, "0.0");
        // test
        EntryPoint updatedEP = initialEP.mergeWithUpdate(newEP);

        // check
        assertThat(updatedEP.getBackend("backend").get().getServers()).isNotNull();
        assertThat(updatedEP.getBackend("backend").get().getServers().size()).isEqualTo(2);
        assertThat(updatedEP.getBackend("backend").get().getServer("server2").isPresent()).isFalse();
    }
}