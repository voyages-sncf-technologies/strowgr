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

import java.util.*;

import static org.fest.assertions.Fail.fail;
import static org.mockito.Mockito.*;

public class EntryPointEventHandlerTest {

    EntryPointStateManager stateManager;
    EntryPointEventHandler handler;
    TemplateLocator        templateLocator;
    TemplateGenerator      templateGenerator;
    PortProvider           portProvider;

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

        EntryPoint config = EntryPoint
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

        EntryPoint config = EntryPoint
                .onHaproxy("haproxy")
                .withUser("hapuser")
                .definesFrontends(ImmutableSet.<EntryPointFrontend>of())
                .definesBackends(ImmutableSet.<EntryPointBackend>of())
                .withGlobalContext(ImmutableMap.<String, String>of())
                .build();

        EntryPoint current = EntryPoint
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

        EntryPoint config = EntryPoint
                .onHaproxy("haproxy")
                .withUser("hapuser")
                .definesFrontends(ImmutableSet.<EntryPointFrontend>of())
                .definesBackends(ImmutableSet.<EntryPointBackend>of())
                .withGlobalContext(ImmutableMap.<String, String>of())
                .build();

        EntryPoint committing = EntryPoint
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

    @Test(expected = IllegalStateException.class)
    public void try_commit_current_applies_for_not_existing_entrypoint() throws IncompleteConfigurationException {
        EntryPointKey key = new EntryPointKeyDefaultImpl("some_key");
        TryCommitCurrentConfigurationEvent event = new TryCommitCurrentConfigurationEvent(CorrelationId.newCorrelationId(), key);
        when(stateManager.tryCommitCurrent(key)).thenReturn(Optional.empty());

        handler.handleTryCommitCurrentConfigurationEvent(event);

        fail("should throw an IllegalStateException");
    }

    @Test
    public void update_entry_point_should_do_nothing_if_there_is_no_configuration_at_all() {
        EntryPointKey key = new EntryPointKeyDefaultImpl("key");
        UpdateEntryPointEvent event = new UpdateEntryPointEvent("correlation_id", key, new UpdatedEntryPoint("user", new HashMap<>(), new HashSet<>(), new HashSet<>()));

        when(stateManager.getCommittingConfiguration(key)).thenReturn(Optional.empty());
        when(stateManager.getPendingConfiguration(key)).thenReturn(Optional.empty());
        when(stateManager.getCurrentConfiguration(key)).thenReturn(Optional.empty());

        handler.handle(event);

        verify(stateManager, never()).prepare(any(), any());
    }

    @Test
    public void update_entry_point_should_change_configuration_based_on_pending_one() {
        EntryPointKey key = new EntryPointKeyDefaultImpl("key");

        /* Create an existing pending configuration */
        EntryPoint pendingConfiguration = getUpdateTestExistingEntryPoint();

        /* Create an update event that does a lot of things */
        UpdateEntryPointEvent event = getUpdateTestEvent(key);

        /* Create expected configuration */
        EntryPoint expectedConfiguration = getUpdateTestExpectedEntryPoint();

        when(stateManager.getCommittingConfiguration(key)).thenReturn(Optional.empty());
        when(stateManager.getPendingConfiguration(key)).thenReturn(Optional.of(pendingConfiguration));
        when(stateManager.getCurrentConfiguration(key)).thenReturn(Optional.empty());
        when(stateManager.prepare(key, expectedConfiguration)).thenReturn(Optional.of(expectedConfiguration));

        handler.handle(event);

        verify(stateManager).prepare(key, expectedConfiguration);
    }

    private EntryPoint getUpdateTestExpectedEntryPoint() {
        Map<String, String> newGlobalContext = new HashMap<>();
        newGlobalContext.put("key", "global");
        newGlobalContext.put("new", "new");

        Set<EntryPointFrontend> newFrontends = new HashSet<>();
        Map<String, String> newF2Context = new HashMap<>();
        newF2Context.put("key", "f2");
        EntryPointFrontend nf2 = new EntryPointFrontend("f2", newF2Context);
        newFrontends.add(nf2);
        newFrontends.add(new EntryPointFrontend("f3", new HashMap<String, String>()));

        Map<String, String> newS1Context = new HashMap<>();
        newS1Context.put("key", "existing");
        Map<String, String> contextOverrideS1 = new HashMap<>();
        contextOverrideS1.put("key", "s1");
        EntryPointBackendServer ns1 = new EntryPointBackendServer("s1", "host", "ip", "port", newS1Context, contextOverrideS1);
        Map<String, String> newS2Context = new HashMap<>();
        newS2Context.put("key", "existing");
        Map<String, String> contextOverrideS2 = new HashMap<>();
        contextOverrideS2.put("key", "s2");
        EntryPointBackendServer ns2 = new EntryPointBackendServer("s2", "host", "ip", "port", newS2Context, contextOverrideS2);
        EntryPointBackendServer ns4 = new EntryPointBackendServer("s4", "host", "ip", "port", new HashMap<>(), new HashMap<>());
        EntryPointBackendServer ns5 = new EntryPointBackendServer("s5", "host", "ip", "port", new HashMap<>(), new HashMap<>());

        Set<EntryPointBackend> newBackends = new HashSet<>();
        Map<String, String> newB1Context = new HashMap<>();
        newB1Context.put("key", "b1");
        EntryPointBackend nb1 = new EntryPointBackend("b1", Sets.newHashSet(ns1, ns2, ns4, ns5), newB1Context);
        newBackends.add(nb1);
        newBackends.add(new EntryPointBackend("b6", new HashSet<EntryPointBackendServer>(), new HashMap<String, String>()));

        return new EntryPoint("haproxy", "new_user", newFrontends, newBackends, newGlobalContext);
    }

    private UpdateEntryPointEvent getUpdateTestEvent(EntryPointKey key) {
    /* It redifines the global context */
        Map<String, String> globalContext = new HashMap<>();
        globalContext.put("key", "global");
        globalContext.put("new", "new");

        /* It removes one frontend, add one and changes the last one*/
        Map<String, String> f2Context = new HashMap<>();
        f2Context.put("key", "f2");
        UpdatedEntryPointFrontend uf2 = new UpdatedEntryPointFrontend("f2", f2Context);
        UpdatedEntryPointFrontend uf3 = new UpdatedEntryPointFrontend("f3", new HashMap<>());

        /* It removes backends, add one and changes another one. It changes context for 2 servers and adds context for a non existing server, which should do nothing */
        Map<String, String> b1Context = new HashMap<>();
        b1Context.put("key", "b1");

        Map<String, String> s1Context = new HashMap<>();
        s1Context.put("key", "s1");
        UpdatedEntryPointBackendServer us1 = new UpdatedEntryPointBackendServer("s1", s1Context);
        Map<String, String> s2Context = new HashMap<>();
        s2Context.put("key", "s2");
        UpdatedEntryPointBackendServer us2 = new UpdatedEntryPointBackendServer("s2", s2Context);
        UpdatedEntryPointBackendServer us6 = new UpdatedEntryPointBackendServer("s6", new HashMap<>());

        UpdatedEntryPointBackend ub1 = new UpdatedEntryPointBackend("b1", Sets.newHashSet(us1, us2, us6), b1Context);
        UpdatedEntryPointBackend ub6 = new UpdatedEntryPointBackend("b6", Sets.newHashSet(us1, us2), new HashMap<>());

        UpdatedEntryPoint uep = new UpdatedEntryPoint("new_user", globalContext, Sets.newHashSet(uf2, uf3), Sets.newHashSet(ub1, ub6));

        return new UpdateEntryPointEvent("correlation_id", key, uep);
    }

    private EntryPoint getUpdateTestExistingEntryPoint() {
    /* there is a global context */
        Map<String, String> existingGlobalContext = new HashMap<>();
        existingGlobalContext.put("key", "existing");

        /* There are 4 frontends, 2 with contexts, 2 that will be deleted */
        Set<EntryPointFrontend> frontends = new HashSet<>();
        Map<String, String> existingF1Context = new HashMap<>();
        existingF1Context.put("key", "existing");
        EntryPointFrontend f1 = new EntryPointFrontend("f1", existingF1Context);
        frontends.add(f1);

        Map<String, String> existingF2Context = new HashMap<>();
        existingF2Context.put("key", "existing");
        EntryPointFrontend f2 = new EntryPointFrontend("f2", existingF2Context);
        frontends.add(f2);

        frontends.add(new EntryPointFrontend("f4", new HashMap<String, String>()));
        frontends.add(new EntryPointFrontend("f5", new HashMap<String, String>()));

        /* There are 4 backends, 2 with contexts, 2 that will be deleted */
        /* We add all servers in the same backend TODO Fix servers and backends Jira HAAAS-32 */
        Map<String, String> existingS1Context = new HashMap<>();
        existingS1Context.put("key", "existing");
        EntryPointBackendServer s1 = new EntryPointBackendServer("s1", "host", "ip", "port", existingS1Context, new HashMap<>());
        Map<String, String> existingS2Context = new HashMap<>();
        existingS2Context.put("key", "existing");
        EntryPointBackendServer s2 = new EntryPointBackendServer("s2", "host", "ip", "port", existingS2Context, new HashMap<>());
        EntryPointBackendServer s4 = new EntryPointBackendServer("s4", "host", "ip", "port", new HashMap<>(), new HashMap<>());
        EntryPointBackendServer s5 = new EntryPointBackendServer("s5", "host", "ip", "port", new HashMap<>(), new HashMap<>());

        Set<EntryPointBackend> backends = new HashSet<>();
        Map<String, String> existingB1Context = new HashMap<>();
        existingB1Context.put("key", "existing");
        EntryPointBackend b1 = new EntryPointBackend("b1", Sets.newHashSet(s1, s2, s4, s5), existingB1Context);
        backends.add(b1);

        Map<String, String> existingB2Context = new HashMap<>();
        existingB2Context.put("key", "existing");
        EntryPointBackend b2 = new EntryPointBackend("b2", Sets.newHashSet(), existingB2Context);
        backends.add(b2);

        backends.add(new EntryPointBackend("b4", new HashSet<EntryPointBackendServer>(), new HashMap<String, String>()));
        backends.add(new EntryPointBackend("b5", new HashSet<EntryPointBackendServer>(), new HashMap<String, String>()));

        /* Put everything in a configuration */
        return new EntryPoint("haproxy", "user", frontends, backends, existingGlobalContext);
    }

    @Test
    public void otherwise_entry_point_update_should_create_pending_conf_based_on_committing_conf() {
        EntryPointKey key = new EntryPointKeyDefaultImpl("key");

        /* Create an existing pending configuration */
        EntryPoint committingConfiguration = getUpdateTestExistingEntryPoint();

        /* Create an update event that does a lot of things */
        UpdateEntryPointEvent event = getUpdateTestEvent(key);

        /* Create expected configuration */
        EntryPoint expectedConfiguration = getUpdateTestExpectedEntryPoint();

        when(stateManager.getCommittingConfiguration(key)).thenReturn(Optional.of(committingConfiguration));
        when(stateManager.getPendingConfiguration(key)).thenReturn(Optional.empty());
        when(stateManager.getCurrentConfiguration(key)).thenReturn(Optional.empty());
        when(stateManager.prepare(key, expectedConfiguration)).thenReturn(Optional.of(expectedConfiguration));

        handler.handle(event);

        verify(stateManager).prepare(key, expectedConfiguration);
    }

    @Test
    public void otherwise_entry_point_update_should_create_pending_conf_based_on_current_conf() {
        EntryPointKey key = new EntryPointKeyDefaultImpl("key");

        /* Create an existing pending configuration */
        EntryPoint currentConfiguration = getUpdateTestExistingEntryPoint();

        /* Create an update event that does a lot of things */
        UpdateEntryPointEvent event = getUpdateTestEvent(key);

        /* Create expected configuration */
        EntryPoint expectedConfiguration = getUpdateTestExpectedEntryPoint();

        when(stateManager.getCommittingConfiguration(key)).thenReturn(Optional.empty());
        when(stateManager.getPendingConfiguration(key)).thenReturn(Optional.empty());
        when(stateManager.getCurrentConfiguration(key)).thenReturn(Optional.of(currentConfiguration));
        when(stateManager.prepare(key, expectedConfiguration)).thenReturn(Optional.of(expectedConfiguration));

        handler.handle(event);

        verify(stateManager).prepare(key, expectedConfiguration);
    }

    @Test
    public void try_commit_current_applies_with_right_key() throws IncompleteConfigurationException {
        // Given
        EntryPointKey key = new EntryPointKeyDefaultImpl("some_key");
        TryCommitCurrentConfigurationEvent event = new TryCommitCurrentConfigurationEvent(CorrelationId.newCorrelationId(), key);
        EntryPoint entryPoint = EntryPoint
                .onHaproxy("haproxy")
                .withUser("hapuser")
                .definesFrontends(ImmutableSet.<EntryPointFrontend>of())
                .definesBackends(ImmutableSet.<EntryPointBackend>of())
                .withGlobalContext(ImmutableMap.<String, String>of())
                .build();
        when(stateManager.tryCommitCurrent(key)).thenReturn(Optional.of(entryPoint));
        when(portProvider.getPort(key.getID() + "-syslog")).thenReturn(Optional.of(666));

        // Test
        handler.handleTryCommitCurrentConfigurationEvent(event);

        // Check
        verify(stateManager).tryCommitCurrent(key);
    }

    @Test
    public void try_commit_pending_applies_with_right_key() throws IncompleteConfigurationException {
        // Given
        EntryPointKey key = new EntryPointKeyDefaultImpl("some_key");
        TryCommitPendingConfigurationEvent event = new TryCommitPendingConfigurationEvent(CorrelationId.newCorrelationId(), key);
        EntryPoint entryPoint = EntryPoint
                .onHaproxy("haproxy")
                .withUser("hapuser")
                .definesFrontends(ImmutableSet.<EntryPointFrontend>of())
                .definesBackends(ImmutableSet.<EntryPointBackend>of())
                .withGlobalContext(ImmutableMap.<String, String>of())
                .build();
        when(stateManager.tryCommitPending(key)).thenReturn(Optional.of(entryPoint));
        when(portProvider.getPort(key.getID() + "-syslog")).thenReturn(Optional.of(666));

        // Test
        handler.handleTryCommitPendingConfigurationEvent(event);

        // Check
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

        EntryPoint commmittingConfig = EntryPoint
                .onHaproxy("haproxy")
                .withUser("hapuser")
                .definesFrontends(ImmutableSet.<EntryPointFrontend>of())
                .definesBackends(ImmutableSet.<EntryPointBackend>of())
                .withGlobalContext(ImmutableMap.<String, String>of())
                .build();

        EntryPoint pendingConfig = EntryPoint
                .onHaproxy("haproxy")
                .withUser("hapuser")
                .definesFrontends(ImmutableSet.<EntryPointFrontend>of())
                .definesBackends(ImmutableSet.of(backend))
                .withGlobalContext(ImmutableMap.<String, String>of())
                .build();

        EntryPoint currentConfig = EntryPoint
                .onHaproxy("haproxy")
                .withUser("hapuser")
                .definesFrontends(ImmutableSet.<EntryPointFrontend>of())
                .definesBackends(ImmutableSet.<EntryPointBackend>of())
                .withGlobalContext(ImmutableMap.<String, String>of())
                .build();

        EntryPointBackendServer bs = new EntryPointBackendServer("ijklm", "hostname", "10.98.71.1", "9090", new HashMap<>(), new HashMap<>());
        EntryPointBackend expectedBackend = new EntryPointBackend("BACKEND", Sets.newHashSet(bs), new HashMap<>());
        EntryPoint expectedConfig = EntryPoint
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

        EntryPoint commmittingConfig = EntryPoint
                .onHaproxy("haproxy")
                .withUser("hapuser")
                .definesFrontends(ImmutableSet.<EntryPointFrontend>of())
                .definesBackends(ImmutableSet.of(backend))
                .withGlobalContext(ImmutableMap.<String, String>of())
                .build();

        EntryPoint currentConfig = EntryPoint
                .onHaproxy("haproxy")
                .withUser("hapuser")
                .definesFrontends(ImmutableSet.<EntryPointFrontend>of())
                .definesBackends(ImmutableSet.<EntryPointBackend>of())
                .withGlobalContext(ImmutableMap.<String, String>of())
                .build();

        EntryPointBackendServer bs = new EntryPointBackendServer("ijklm", "hostname", "10.98.71.1", "9090", new HashMap<>(), new HashMap<>());
        EntryPointBackend expectedBackend = new EntryPointBackend("BACKEND", Sets.newHashSet(bs), new HashMap<>());
        EntryPoint expectedConfig = EntryPoint
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

        EntryPoint currentConfig = EntryPoint
                .onHaproxy("haproxy")
                .withUser("hapuser")
                .definesFrontends(ImmutableSet.<EntryPointFrontend>of())
                .definesBackends(ImmutableSet.of(backend))
                .withGlobalContext(ImmutableMap.<String, String>of())
                .build();

        EntryPointBackendServer bs = new EntryPointBackendServer("ijklm", "hostname", "10.98.71.1", "9090", new HashMap<>(), new HashMap<>());
        EntryPointBackend expectedBackend = new EntryPointBackend("BACKEND", Sets.newHashSet(bs), new HashMap<>());
        EntryPoint expectedConfig = EntryPoint
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

        EntryPoint currentConfig = EntryPoint
                .onHaproxy("haproxy")
                .withUser("hapuser")
                .definesFrontends(ImmutableSet.<EntryPointFrontend>of())
                .definesBackends(ImmutableSet.<EntryPointBackend>of())
                .withGlobalContext(ImmutableMap.<String, String>of())
                .build();

        EntryPointBackendServer bs = new EntryPointBackendServer("ijklm", "hostname", "10.98.71.1", "9090", new HashMap<>(), new HashMap<>());
        EntryPointBackend expectedBackend = new EntryPointBackend("BACKEND", Sets.newHashSet(bs), new HashMap<>());
        EntryPoint expectedConfig = EntryPoint
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

        EntryPoint currentConfig = EntryPoint
                .onHaproxy("haproxy")
                .withUser("hapuser")
                .definesFrontends(ImmutableSet.<EntryPointFrontend>of())
                .definesBackends(ImmutableSet.of(backend))
                .withGlobalContext(ImmutableMap.<String, String>of())
                .build();

        EntryPointBackendServer bs = new EntryPointBackendServer("ijklm", "hostname", "10.98.71.1", "9090", new HashMap<>(), new HashMap<>());
        EntryPointBackend expectedBackend = new EntryPointBackend("BACKEND", Sets.newHashSet(bs), backendContext);
        EntryPoint expectedConfig = EntryPoint
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

        EntryPoint currentConfig = EntryPoint
                .onHaproxy("haproxy")
                .withUser("hapuser")
                .definesFrontends(ImmutableSet.<EntryPointFrontend>of())
                .definesBackends(ImmutableSet.of(backend))
                .withGlobalContext(ImmutableMap.<String, String>of())
                .build();

        EntryPointBackendServer bs = new EntryPointBackendServer("ijklm", "hostname2", "10.98.71.2", "9092", newServerContext, new HashMap<>());
        EntryPointBackend expectedBackend = new EntryPointBackend("BACKEND", Sets.newHashSet(bs), new HashMap<>());
        EntryPoint expectedConfig = EntryPoint
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

        EntryPoint currentConfig = EntryPoint
                .onHaproxy("haproxy")
                .withUser("hapuser")
                .definesFrontends(ImmutableSet.<EntryPointFrontend>of())
                .definesBackends(ImmutableSet.of(backend))
                .withGlobalContext(ImmutableMap.<String, String>of())
                .build();

        EntryPointBackendServer expectedServer = new EntryPointBackendServer("ijklm", "hostname2", "10.98.71.2", "9092", newServerContext, oldUserContext);
        EntryPointBackend expectedBackend = new EntryPointBackend("BACKEND", Sets.newHashSet(expectedServer), new HashMap<>());
        EntryPoint expectedConfig = EntryPoint
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

        EntryPoint currentConfig = EntryPoint
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

        EntryPoint expectedConfig = EntryPoint
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
