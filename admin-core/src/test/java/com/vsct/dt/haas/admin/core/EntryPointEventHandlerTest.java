package com.vsct.dt.haas.admin.core;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.google.common.eventbus.EventBus;
import com.vsct.dt.haas.admin.core.configuration.*;
import com.vsct.dt.haas.admin.core.event.CorrelationId;
import com.vsct.dt.haas.admin.core.event.in.*;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;

import static org.junit.Assert.fail;
import static org.mockito.Mockito.*;

/**
 * Created by william_montaz on 05/02/2016.
 */
public class EntryPointEventHandlerTest {

    EntryPointStateManager stateManager;
    EntryPointEventHandler handler;
    TemplateLocator templateLocator;
    TemplateGenerator templateGenerator;
    PortProvider portProvider;

    @Before
    public void setUp() {
        stateManager = mock(EntryPointStateManager.class);

        templateLocator = mock(TemplateLocator.class);
        templateGenerator = mock(TemplateGenerator.class);
        portProvider = mock(PortProvider.class);
        handler = new EntryPointEventHandler(stateManager, portProvider, templateLocator, templateGenerator, new EventBus());
    }

    @Test
    public void add_entry_point_should_prepare_configuration_if_no_current_or_committing_exists() {
        EntryPointKey key = new EntryPointKeyDefaultImpl("some_key");

        EntryPointConfiguration config = EntryPointConfiguration
                .onHaproxy("haproxy")
                .withUser("hapuser")
                .definesFrontends(ImmutableSet.<EntryPointFrontend>of())
                .definesBackends(ImmutableSet.<EntryPointBackend>of())
                .withGlobalContext(ImmutableMap.<String, String>of())
                .build();

        AddEntryPointEvent event = new AddEntryPointEvent(CorrelationId.newCorrelationId(), key, config);

        when(stateManager.getCommittingConfiguration(key)).thenReturn(Optional.empty());
        when(stateManager.getCurrentConfiguration(key)).thenReturn(Optional.empty());
        when(stateManager.prepare(key, config)).thenReturn(Optional.of(config));

        handler.handle(event);

        verify(stateManager).prepare(key, config);
    }

    @Test
    public void add_entry_point_should_do_nothing_if_current_exists() {
        EntryPointKey key = new EntryPointKeyDefaultImpl("some_key");

        EntryPointConfiguration config = EntryPointConfiguration
                .onHaproxy("haproxy")
                .withUser("hapuser")
                .definesFrontends(ImmutableSet.<EntryPointFrontend>of())
                .definesBackends(ImmutableSet.<EntryPointBackend>of())
                .withGlobalContext(ImmutableMap.<String, String>of())
                .build();

        EntryPointConfiguration current = EntryPointConfiguration
                .onHaproxy("haproxy")
                .withUser("hapuser")
                .definesFrontends(ImmutableSet.<EntryPointFrontend>of())
                .definesBackends(ImmutableSet.<EntryPointBackend>of())
                .withGlobalContext(ImmutableMap.<String, String>of())
                .build();

        AddEntryPointEvent event = new AddEntryPointEvent(CorrelationId.newCorrelationId(), key, config);

        when(stateManager.getCommittingConfiguration(key)).thenReturn(Optional.empty());
        when(stateManager.getCurrentConfiguration(key)).thenReturn(Optional.of(current));

        handler.handle(event);

        verify(stateManager, never()).prepare(any(), any());
    }

    @Test
    public void add_entry_point_should_do_nothing_if_committing_exists() {
        EntryPointKey key = new EntryPointKeyDefaultImpl("some_key");

        EntryPointConfiguration config = EntryPointConfiguration
                .onHaproxy("haproxy")
                .withUser("hapuser")
                .definesFrontends(ImmutableSet.<EntryPointFrontend>of())
                .definesBackends(ImmutableSet.<EntryPointBackend>of())
                .withGlobalContext(ImmutableMap.<String, String>of())
                .build();

        EntryPointConfiguration committing = EntryPointConfiguration
                .onHaproxy("haproxy")
                .withUser("hapuser")
                .definesFrontends(ImmutableSet.<EntryPointFrontend>of())
                .definesBackends(ImmutableSet.<EntryPointBackend>of())
                .withGlobalContext(ImmutableMap.<String, String>of())
                .build();

        AddEntryPointEvent event = new AddEntryPointEvent(CorrelationId.newCorrelationId(), key, config);

        when(stateManager.getCommittingConfiguration(key)).thenReturn(Optional.of(committing));
        when(stateManager.getCurrentConfiguration(key)).thenReturn(Optional.empty());

        handler.handle(event);

        verify(stateManager, never()).prepare(any(), any());
    }

    @Test
    public void try_commit_current_applies_with_right_key() {
        EntryPointKey key = new EntryPointKeyDefaultImpl("some_key");
        TryCommitCurrentConfigurationEvent event = new TryCommitCurrentConfigurationEvent(CorrelationId.newCorrelationId(), key);
        when(stateManager.tryCommitCurrent(key)).thenReturn(Optional.empty());

        handler.handleTryCommitCurrentConfigurationEvent(event);

        verify(stateManager).tryCommitCurrent(key);
    }

    @Test
    public void try_commit_pending_applies_with_right_key() {
        EntryPointKey key = new EntryPointKeyDefaultImpl("some_key");
        TryCommitPendingConfigurationEvent event = new TryCommitPendingConfigurationEvent(CorrelationId.newCorrelationId(), key);
        when(stateManager.tryCommitPending(key)).thenReturn(Optional.empty());

        handler.handleTryCommitPendingConfigurationEvent(event);

        verify(stateManager).tryCommitPending(key);
    }

    @Test
    public void commit_success_event_applies_with_right_key() {
        EntryPointKey key = new EntryPointKeyDefaultImpl("some_key");
        CommitSuccessEvent event = new CommitSuccessEvent(CorrelationId.newCorrelationId(), key);
        when(stateManager.commit(key)).thenReturn(Optional.empty());

        handler.handleCommitSuccessEvent(event);

        verify(stateManager).commit(key);
    }

    @Test
    public void commit_failure_event() {
        /* Not Implemented Yet */
    }

    /* Server registration tests are made on one server but they apply to a set of server provided by the event */
    @Test
    public void server_registration_should_do_nothing_if_there_is_no_configuration_at_all() {
        EntryPointKey key = new EntryPointKeyDefaultImpl("some_key");
        IncomingEntryPointBackendServer server = new IncomingEntryPointBackendServer("ijklm", "hostname", "10.98.71.1", "9090", new HashMap<>());
        RegisterServerEvent event = new RegisterServerEvent(CorrelationId.newCorrelationId(), key, "BACKEND", ImmutableSet.of(server));

        when(stateManager.getCommittingConfiguration(key)).thenReturn(Optional.empty());
        when(stateManager.getPendingConfiguration(key)).thenReturn(Optional.empty());
        when(stateManager.getCurrentConfiguration(key)).thenReturn(Optional.empty());

        handler.handle(event);

        verify(stateManager, never()).prepare(any(), any());
    }

    @Test
    public void server_registration_should_change_configuration_based_on_pending_one() {
        EntryPointKey key = new EntryPointKeyDefaultImpl("some_key");
        IncomingEntryPointBackendServer server = new IncomingEntryPointBackendServer("ijklm", "hostname", "10.98.71.1", "9090", new HashMap<>());
        RegisterServerEvent event = new RegisterServerEvent(CorrelationId.newCorrelationId(), key, "BACKEND", ImmutableSet.of(server));

        EntryPointBackend backend = new EntryPointBackend("BACKEND");

        EntryPointConfiguration commmittingConfig = EntryPointConfiguration
                .onHaproxy("haproxy")
                .withUser("hapuser")
                .definesFrontends(ImmutableSet.<EntryPointFrontend>of())
                .definesBackends(ImmutableSet.<EntryPointBackend>of())
                .withGlobalContext(ImmutableMap.<String, String>of())
                .build();

        EntryPointConfiguration pendingConfig = EntryPointConfiguration
                .onHaproxy("haproxy")
                .withUser("hapuser")
                .definesFrontends(ImmutableSet.<EntryPointFrontend>of())
                .definesBackends(ImmutableSet.of(backend))
                .withGlobalContext(ImmutableMap.<String, String>of())
                .build();

        EntryPointConfiguration currentConfig = EntryPointConfiguration
                .onHaproxy("haproxy")
                .withUser("hapuser")
                .definesFrontends(ImmutableSet.<EntryPointFrontend>of())
                .definesBackends(ImmutableSet.<EntryPointBackend>of())
                .withGlobalContext(ImmutableMap.<String, String>of())
                .build();

        EntryPointBackend expectedBackend = new EntryPointBackend("BACKEND", Sets.newHashSet(new EntryPointBackendServer(server.getId(), server.getHostname(), server.getIp(), server.getPort(), server.getContext(), new HashMap<String, String>())), new HashMap<>());
        EntryPointConfiguration expectedConfig = EntryPointConfiguration
                .onHaproxy("haproxy")
                .withUser("hapuser")
                .definesFrontends(ImmutableSet.<EntryPointFrontend>of())
                .definesBackends(ImmutableSet.of(expectedBackend))
                .withGlobalContext(ImmutableMap.<String, String>of())
                .build();

        when(stateManager.getCommittingConfiguration(key)).thenReturn(Optional.of(commmittingConfig));
        when(stateManager.getPendingConfiguration(key)).thenReturn(Optional.of(pendingConfig));
        when(stateManager.getCurrentConfiguration(key)).thenReturn(Optional.of(currentConfig));
        when(stateManager.prepare(key, expectedConfig)).thenReturn(Optional.of(expectedConfig));

        handler.handle(event);

        verify(stateManager).prepare(eq(key), eq(expectedConfig));
    }

    @Test
    public void otherwise_server_registration_should_create_pending_configuration_based_on_committing_one() {
        EntryPointKey key = new EntryPointKeyDefaultImpl("some_key");
        IncomingEntryPointBackendServer server = new IncomingEntryPointBackendServer("ijklm", "hostname", "10.98.71.1", "9090", new HashMap<>());
        RegisterServerEvent event = new RegisterServerEvent(CorrelationId.newCorrelationId(), key, "BACKEND", ImmutableSet.of(server));

        EntryPointBackend backend = new EntryPointBackend("BACKEND");

        EntryPointConfiguration commmittingConfig = EntryPointConfiguration
                .onHaproxy("haproxy")
                .withUser("hapuser")
                .definesFrontends(ImmutableSet.<EntryPointFrontend>of())
                .definesBackends(ImmutableSet.of(backend))
                .withGlobalContext(ImmutableMap.<String, String>of())
                .build();

        EntryPointConfiguration currentConfig = EntryPointConfiguration
                .onHaproxy("haproxy")
                .withUser("hapuser")
                .definesFrontends(ImmutableSet.<EntryPointFrontend>of())
                .definesBackends(ImmutableSet.<EntryPointBackend>of())
                .withGlobalContext(ImmutableMap.<String, String>of())
                .build();

        EntryPointBackend expectedBackend = new EntryPointBackend("BACKEND", Sets.newHashSet(new EntryPointBackendServer(server.getId(), server.getHostname(), server.getIp(), server.getPort(), server.getContext(), new HashMap<String, String>())), new HashMap<>());
        EntryPointConfiguration expectedConfig = EntryPointConfiguration
                .onHaproxy("haproxy")
                .withUser("hapuser")
                .definesFrontends(ImmutableSet.<EntryPointFrontend>of())
                .definesBackends(ImmutableSet.of(expectedBackend))
                .withGlobalContext(ImmutableMap.<String, String>of())
                .build();

        when(stateManager.getCommittingConfiguration(key)).thenReturn(Optional.of(commmittingConfig));
        when(stateManager.getPendingConfiguration(key)).thenReturn(Optional.empty());
        when(stateManager.getCurrentConfiguration(key)).thenReturn(Optional.of(currentConfig));
        when(stateManager.prepare(key, expectedConfig)).thenReturn(Optional.of(expectedConfig));

        handler.handle(event);

        verify(stateManager).prepare(eq(key), eq(expectedConfig));
    }

    @Test
    public void otherwise_server_registration_should_create_pending_configuration_based_on_current_one() {
        EntryPointKey key = new EntryPointKeyDefaultImpl("some_key");
        IncomingEntryPointBackendServer server = new IncomingEntryPointBackendServer("ijklm", "hostname", "10.98.71.1", "9090", new HashMap<>());
        RegisterServerEvent event = new RegisterServerEvent(CorrelationId.newCorrelationId(), key, "BACKEND", ImmutableSet.of(server));

        EntryPointBackend backend = new EntryPointBackend("BACKEND");

        EntryPointConfiguration currentConfig = EntryPointConfiguration
                .onHaproxy("haproxy")
                .withUser("hapuser")
                .definesFrontends(ImmutableSet.<EntryPointFrontend>of())
                .definesBackends(ImmutableSet.of(backend))
                .withGlobalContext(ImmutableMap.<String, String>of())
                .build();

        EntryPointBackend expectedBackend = new EntryPointBackend("BACKEND", Sets.newHashSet(new EntryPointBackendServer(server.getId(), server.getHostname(), server.getIp(), server.getPort(), server.getContext(), new HashMap<String, String>())), new HashMap<>());
        EntryPointConfiguration expectedConfig = EntryPointConfiguration
                .onHaproxy("haproxy")
                .withUser("hapuser")
                .definesFrontends(ImmutableSet.<EntryPointFrontend>of())
                .definesBackends(ImmutableSet.of(expectedBackend))
                .withGlobalContext(ImmutableMap.<String, String>of())
                .build();

        when(stateManager.getCommittingConfiguration(key)).thenReturn(Optional.empty());
        when(stateManager.getPendingConfiguration(key)).thenReturn(Optional.empty());
        when(stateManager.getCurrentConfiguration(key)).thenReturn(Optional.of(currentConfig));
        when(stateManager.prepare(key, expectedConfig)).thenReturn(Optional.of(expectedConfig));

        handler.handle(event);


        verify(stateManager).prepare(eq(key), eq(expectedConfig));
    }

    @Test
    public void server_registration_should_create_a_new_backend_with_no_context_if_the_backend_does_not_exists() {
        EntryPointKey key = new EntryPointKeyDefaultImpl("some_key");
        IncomingEntryPointBackendServer server = new IncomingEntryPointBackendServer("ijklm", "hostname", "10.98.71.1", "9090", new HashMap<>());
        RegisterServerEvent event = new RegisterServerEvent(CorrelationId.newCorrelationId(), key, "BACKEND", ImmutableSet.of(server));

        EntryPointConfiguration currentConfig = EntryPointConfiguration
                .onHaproxy("haproxy")
                .withUser("hapuser")
                .definesFrontends(ImmutableSet.<EntryPointFrontend>of())
                .definesBackends(ImmutableSet.<EntryPointBackend>of())
                .withGlobalContext(ImmutableMap.<String, String>of())
                .build();

        EntryPointBackend expectedBackend = new EntryPointBackend("BACKEND", Sets.newHashSet(new EntryPointBackendServer(server.getId(), server.getHostname(), server.getIp(), server.getPort(), server.getContext(), new HashMap<String, String>())), new HashMap<>());
        EntryPointConfiguration expectedConfig = EntryPointConfiguration
                .onHaproxy("haproxy")
                .withUser("hapuser")
                .definesFrontends(ImmutableSet.<EntryPointFrontend>of())
                .definesBackends(ImmutableSet.of(expectedBackend))
                .withGlobalContext(ImmutableMap.<String, String>of())
                .build();

        when(stateManager.getCommittingConfiguration(key)).thenReturn(Optional.empty());
        when(stateManager.getPendingConfiguration(key)).thenReturn(Optional.empty());
        when(stateManager.getCurrentConfiguration(key)).thenReturn(Optional.of(currentConfig));
        when(stateManager.prepare(key, expectedConfig)).thenReturn(Optional.of(expectedConfig));

        handler.handle(event);

        verify(stateManager).prepare(eq(key), eq(expectedConfig));
    }

    @Test
    public void server_registration_should_not_erase_backend_context_if_backend_already_exists() {

        EntryPointKey key = new EntryPointKeyDefaultImpl("some_key");
        IncomingEntryPointBackendServer server = new IncomingEntryPointBackendServer("ijklm", "hostname", "10.98.71.1", "9090", new HashMap<>());
        Map<String, String> backendContext = new HashMap<>();
        backendContext.put("key1", "value1");
        backendContext.put("key2", "value2");
        EntryPointBackend backend = new EntryPointBackend("BACKEND", new HashSet<>(), backendContext);

        RegisterServerEvent event = new RegisterServerEvent(CorrelationId.newCorrelationId(), key, "BACKEND", ImmutableSet.of(server));

        EntryPointConfiguration currentConfig = EntryPointConfiguration
                .onHaproxy("haproxy")
                .withUser("hapuser")
                .definesFrontends(ImmutableSet.<EntryPointFrontend>of())
                .definesBackends(ImmutableSet.of(backend))
                .withGlobalContext(ImmutableMap.<String, String>of())
                .build();

        EntryPointBackend expectedBackend = new EntryPointBackend("BACKEND", Sets.newHashSet(new EntryPointBackendServer(server.getId(), server.getHostname(), server.getIp(), server.getPort(), server.getContext(), new HashMap<String, String>())), backendContext);
        EntryPointConfiguration expectedConfig = EntryPointConfiguration
                .onHaproxy("haproxy")
                .withUser("hapuser")
                .definesFrontends(ImmutableSet.<EntryPointFrontend>of())
                .definesBackends(ImmutableSet.of(expectedBackend))
                .withGlobalContext(ImmutableMap.<String, String>of())
                .build();

        when(stateManager.getCommittingConfiguration(key)).thenReturn(Optional.empty());
        when(stateManager.getPendingConfiguration(key)).thenReturn(Optional.empty());
        when(stateManager.getCurrentConfiguration(key)).thenReturn(Optional.of(currentConfig));
        when(stateManager.prepare(key, expectedConfig)).thenReturn(Optional.of(expectedConfig));

        handler.handle(event);

        verify(stateManager).prepare(eq(key), eq(expectedConfig));
    }

    @Test
    public void server_registration_should_erase_hostname_ip_port_and_context_attributed_to_the_same_server_id() {
        EntryPointKey key = new EntryPointKeyDefaultImpl("some_key");
        Map<String, String> oldServerContext = new HashMap<>();
        oldServerContext.put("key1", "value1");
        oldServerContext.put("key2", "value2");
        EntryPointBackendServer oldServer = new EntryPointBackendServer("ijklm", "hostname", "10.98.71.1", "9090", oldServerContext, new HashMap<>());

        Map<String, String> newServerContext = new HashMap<>();
        newServerContext.put("key1", "value1bis");
        newServerContext.put("key2", "value2bis");
        newServerContext.put("key3", "value3");
        IncomingEntryPointBackendServer newServer = new IncomingEntryPointBackendServer("ijklm", "hostname2", "10.98.71.2", "9092", newServerContext);

        EntryPointBackend backend = new EntryPointBackend("BACKEND", Sets.newHashSet(oldServer), new HashMap<>());

        RegisterServerEvent event = new RegisterServerEvent(CorrelationId.newCorrelationId(), key, "BACKEND", ImmutableSet.of(newServer));

        EntryPointConfiguration currentConfig = EntryPointConfiguration
                .onHaproxy("haproxy")
                .withUser("hapuser")
                .definesFrontends(ImmutableSet.<EntryPointFrontend>of())
                .definesBackends(ImmutableSet.of(backend))
                .withGlobalContext(ImmutableMap.<String, String>of())
                .build();

        EntryPointBackend expectedBackend = new EntryPointBackend("BACKEND", Sets.newHashSet(new EntryPointBackendServer(newServer.getId(), newServer.getHostname(), newServer.getIp(), newServer.getPort(), newServer.getContext(), new HashMap<String, String>())), new HashMap<>());
        EntryPointConfiguration expectedConfig = EntryPointConfiguration
                .onHaproxy("haproxy")
                .withUser("hapuser")
                .definesFrontends(ImmutableSet.<EntryPointFrontend>of())
                .definesBackends(ImmutableSet.of(expectedBackend))
                .withGlobalContext(ImmutableMap.<String, String>of())
                .build();

        when(stateManager.getCommittingConfiguration(key)).thenReturn(Optional.empty());
        when(stateManager.getPendingConfiguration(key)).thenReturn(Optional.empty());
        when(stateManager.getCurrentConfiguration(key)).thenReturn(Optional.of(currentConfig));
        when(stateManager.prepare(key, expectedConfig)).thenReturn(Optional.of(expectedConfig));

        handler.handle(event);

        verify(stateManager).prepare(eq(key), eq(expectedConfig));
    }

    @Test
    public void server_registration_should_not_erase_user_provided_context_attributed_to_the_same_server_id() {
        EntryPointKey key = new EntryPointKeyDefaultImpl("some_key");
        Map<String, String> oldServerContext = new HashMap<>();
        oldServerContext.put("key1", "value1");
        oldServerContext.put("key2", "value2");
        Map<String, String> oldUserContext = new HashMap<>();
        oldUserContext.put("key1", "value_user");
        oldUserContext.put("key4", "value_user");

        EntryPointBackendServer oldServer = new EntryPointBackendServer("ijklm", "hostname", "10.98.71.1", "9090", oldServerContext, oldUserContext);
        EntryPointBackend backend = new EntryPointBackend("BACKEND", Sets.newHashSet(oldServer), new HashMap<>());

        Map<String, String> newServerContext = new HashMap<>();
        newServerContext.put("key1", "value1bis");
        newServerContext.put("key2", "value2bis");
        IncomingEntryPointBackendServer newServerInEvent = new IncomingEntryPointBackendServer("ijklm", "hostname2", "10.98.71.2", "9092", newServerContext);

        RegisterServerEvent event = new RegisterServerEvent(CorrelationId.newCorrelationId(), key, "BACKEND", ImmutableSet.of(newServerInEvent));

        EntryPointConfiguration currentConfig = EntryPointConfiguration
                .onHaproxy("haproxy")
                .withUser("hapuser")
                .definesFrontends(ImmutableSet.<EntryPointFrontend>of())
                .definesBackends(ImmutableSet.of(backend))
                .withGlobalContext(ImmutableMap.<String, String>of())
                .build();

        EntryPointBackendServer expectedServer = new EntryPointBackendServer("ijklm", "hostname2", "10.98.71.2", "9092", newServerContext, oldUserContext);
        EntryPointBackend expectedBackend = new EntryPointBackend("BACKEND", Sets.newHashSet(expectedServer), new HashMap<>());
        EntryPointConfiguration expectedConfig = EntryPointConfiguration
                .onHaproxy("haproxy")
                .withUser("hapuser")
                .definesFrontends(ImmutableSet.<EntryPointFrontend>of())
                .definesBackends(ImmutableSet.of(expectedBackend))
                .withGlobalContext(ImmutableMap.<String, String>of())
                .build();

        when(stateManager.getCommittingConfiguration(key)).thenReturn(Optional.empty());
        when(stateManager.getPendingConfiguration(key)).thenReturn(Optional.empty());
        when(stateManager.getCurrentConfiguration(key)).thenReturn(Optional.of(currentConfig));
        when(stateManager.prepare(key, expectedConfig)).thenReturn(Optional.of(expectedConfig));

        handler.handle(event);

        verify(stateManager).prepare(eq(key), eq(expectedConfig));
    }

    @Test
    public void server_registration_should_remove_the_backend_from_the_backends_where_it_was_previously_registered_and_preserve_its_original_user_provided_context() {
        EntryPointKey key = new EntryPointKeyDefaultImpl("some_key");

        Map<String, String> serverContext = new HashMap<>();
        serverContext.put("key1", "value1");
        serverContext.put("key2", "value2");
        Map<String, String> serverUserContext = new HashMap<>();
        serverUserContext.put("key1", "value3");
        serverUserContext.put("key2", "value4");
        EntryPointBackendServer server = new EntryPointBackendServer("ijklm", "hostname", "10.98.71.1", "9090", serverContext, serverUserContext);

        Map<String, String> backendContext = new HashMap<>();
        backendContext.put("key1", "value1");
        backendContext.put("key2", "value2");
        EntryPointBackend backend = new EntryPointBackend("BACKEND", Sets.newHashSet(server), backendContext);

        EntryPointConfiguration currentConfig = EntryPointConfiguration
                .onHaproxy("haproxy")
                .withUser("hapuser")
                .definesFrontends(ImmutableSet.<EntryPointFrontend>of())
                .definesBackends(ImmutableSet.of(backend))
                .withGlobalContext(ImmutableMap.<String, String>of())
                .build();

        IncomingEntryPointBackendServer newServerInEvent = new IncomingEntryPointBackendServer("ijklm", "hostname2", "10.98.71.2", "9092", new HashMap<>());
        RegisterServerEvent event = new RegisterServerEvent(CorrelationId.newCorrelationId(), key, "NEWBACKEND", ImmutableSet.of(newServerInEvent));

        EntryPointBackendServer expectedServer = new EntryPointBackendServer("ijklm", "hostname2", "10.98.71.2", "9092", new HashMap<>(), serverUserContext);
        EntryPointBackend expectedBackend1 = new EntryPointBackend("BACKEND", Sets.newHashSet(), backendContext);
        EntryPointBackend expectedBackend2 = new EntryPointBackend("NEWBACKEND", Sets.newHashSet(expectedServer), new HashMap<>());

        EntryPointConfiguration expectedConfig = EntryPointConfiguration
                .onHaproxy("haproxy")
                .withUser("hapuser")
                .definesFrontends(ImmutableSet.<EntryPointFrontend>of())
                .definesBackends(ImmutableSet.of(expectedBackend1, expectedBackend2))
                .withGlobalContext(ImmutableMap.<String, String>of())
                .build();

        when(stateManager.getCommittingConfiguration(key)).thenReturn(Optional.empty());
        when(stateManager.getPendingConfiguration(key)).thenReturn(Optional.empty());
        when(stateManager.getCurrentConfiguration(key)).thenReturn(Optional.of(currentConfig));
        when(stateManager.prepare(key, expectedConfig)).thenReturn(Optional.of(expectedConfig));

        handler.handle(event);

        verify(stateManager).prepare(eq(key), eq(expectedConfig));
    }


}
