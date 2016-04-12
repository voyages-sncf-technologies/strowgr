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
                .withSyslogPort("666")
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
                .withSyslogPort("666")
                .withGlobalContext(ImmutableMap.<String, String>of())
                .build();

        EntryPoint current = EntryPoint
                .onHaproxy("haproxy")
                .withUser("hapuser")
                .definesFrontends(ImmutableSet.<EntryPointFrontend>of())
                .definesBackends(ImmutableSet.<EntryPointBackend>of())
                .withSyslogPort("666")
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
                .withSyslogPort("666")
                .withGlobalContext(ImmutableMap.<String, String>of())
                .build();

        EntryPoint committing = EntryPoint
                .onHaproxy("haproxy")
                .withUser("hapuser")
                .definesFrontends(ImmutableSet.<EntryPointFrontend>of())
                .definesBackends(ImmutableSet.<EntryPointBackend>of())
                .withSyslogPort("666")
                .withGlobalContext(ImmutableMap.<String, String>of())
                .build();

        AddEntryPointEvent event = new AddEntryPointEvent(CorrelationId.newCorrelationId(), key, config);

        when(stateManager.getCommittingConfiguration(key)).thenReturn(Optional.of(committing));
        when(stateManager.getCurrentConfiguration(key)).thenReturn(Optional.empty());

        handler.handle(event);

        verify(stateManager, never()).prepare(any(), any());
    }

    @Test(expected = IllegalStateException.class)
    public void try_commit_current_applies_for_not_existing_entrypoint() {
        EntryPointKey key = new EntryPointKeyDefaultImpl("some_key");
        TryCommitCurrentConfigurationEvent event = new TryCommitCurrentConfigurationEvent(CorrelationId.newCorrelationId(), key);
        when(stateManager.tryCommitCurrent(key)).thenReturn(Optional.empty());

        handler.handleTryCommitCurrentConfigurationEvent(event);

        fail("should throw an IllegalStateException");
    }

    @Test
    public void update_entry_point_should_do_nothing_if_there_is_no_configuration_at_all() {
        EntryPointKey key = new EntryPointKeyDefaultImpl("key");
        UpdateEntryPointEvent event = new UpdateEntryPointEvent("correlation_id", key,
                new HashMap<>(), new HashMap<>(), new HashMap<>(), new HashMap<>(), new HashSet<>(), new HashSet<>(), new HashSet<>());

        handler.handle(event);

        verify(stateManager, never()).prepare(any(), any());
    }

    @Test
    public void update_entry_point_should_change_configuration_based_on_pending_one() {
        EntryPointKey key = new EntryPointKeyDefaultImpl("key");

        /* Create an existing pending configuration */
        Map<String, String> existingGlobalContext = new HashMap<>();
        existingGlobalContext.put("key", "existing");
        existingGlobalContext.put("not_touched", "not_touched");

        /* There are 4 frontends, 2 with contexts, 2 that will be deleted */
        Set<EntryPointFrontend> frontends = new HashSet<>();
        Map<String, String> existingF1Context = new HashMap<>();
        existingF1Context.put("key", "existing");
        existingF1Context.put("not_touched", "not_touched");
        EntryPointFrontend f1 = new EntryPointFrontend("f1", existingF1Context);
        frontends.add(f1);

        Map<String, String> existingF2Context = new HashMap<>();
        existingF2Context.put("key", "existing");
        existingF2Context.put("not_touched", "not_touched");
        EntryPointFrontend f2 = new EntryPointFrontend("f2", existingF2Context);
        frontends.add(f2);

        frontends.add(new EntryPointFrontend("f4", new HashMap<String, String>()));
        frontends.add(new EntryPointFrontend("f5", new HashMap<String, String>()));

        /* There are 4 backends, 2 with contexts, 2 that will be deleted */
        /* We add all servers in the same backend TODO Fix servers and backends Jira HAAAS-32 */
        Map<String, String> existingS1Context = new HashMap<>();
        existingS1Context.put("key", "existing");
        existingS1Context.put("not_touched", "not_touched");
        EntryPointBackendServer s1 = new EntryPointBackendServer("s1", "host", "ip", "port", existingS1Context, new HashMap<>());
        Map<String, String> existingS2Context = new HashMap<>();
        existingS2Context.put("key", "existing");
        existingS2Context.put("not_touched", "not_touched");
        EntryPointBackendServer s2 = new EntryPointBackendServer("s2", "host", "ip", "port", existingS2Context, new HashMap<>());
        EntryPointBackendServer s4 = new EntryPointBackendServer("s4", "host", "ip", "port", new HashMap<>(), new HashMap<>());
        EntryPointBackendServer s5 = new EntryPointBackendServer("s5", "host", "ip", "port", new HashMap<>(), new HashMap<>());

        Set<EntryPointBackend> backends = new HashSet<>();
        Map<String, String> existingB1Context = new HashMap<>();
        existingB1Context.put("key", "existing");
        existingB1Context.put("not_touched", "not_touched");
        EntryPointBackend b1 = new EntryPointBackend("f1", Sets.newHashSet(s1, s2, s4, s5), existingB1Context);
        backends.add(b1);

        Map<String, String> existingB2Context = new HashMap<>();
        existingB2Context.put("key", "existing");
        existingB2Context.put("not_touched", "not_touched");
        EntryPointBackend b2 = new EntryPointBackend("f2", Sets.newHashSet(), existingB2Context);
        backends.add(b2);

        backends.add(new EntryPointBackend("b4", new HashSet<EntryPointBackendServer>(), new HashMap<String, String>()));
        backends.add(new EntryPointBackend("b5", new HashSet<EntryPointBackendServer>(), new HashMap<String, String>()));

        /* Put everything in a configuration */
        EntryPoint existingConfiguration = new EntryPoint("haproxy", "user", frontends, backends, existingGlobalContext);

        /* Create an update event that does a lot of things */
        Map<String, String> globalContext = new HashMap<>();
        globalContext.put("key", "global");
        globalContext.put("new", "new");

        /* Add 2 frontend contexts + one not existing (should do nothing) */
        Map<String, String> f1Context = new HashMap<>();
        f1Context.put("key", "f1");
        Map<String, String> f2Context = new HashMap<>();
        f2Context.put("key", "f2");
        Map<String, Map<String, String>> frontendContexts = new HashMap<>();
        frontendContexts.put("f1", f1Context);
        frontendContexts.put("f2", f2Context);
        frontendContexts.put("f3", new HashMap<>());

        /* Add 2 backend contexts + one not existing (should do nothing) */
        Map<String, String> b1Context = new HashMap<>();
        b1Context.put("key", "b1");
        Map<String, String> b2Context = new HashMap<>();
        b2Context.put("key", "b2");
        Map<String, Map<String, String>> backendContexts = new HashMap<>();
        backendContexts.put("b1", b1Context);
        backendContexts.put("b2", b2Context);
        backendContexts.put("b3", new HashMap<>());

        /* Add 2 server contexts + one not existing (should do nothing) */
        Map<String, String> s1Context = new HashMap<>();
        s1Context.put("key", "s1");
        Map<String, String> s2Context = new HashMap<>();
        s2Context.put("key", "s2");
        Map<String, Map<String, String>> serverContexts = new HashMap<>();
        serverContexts.put("s1", s1Context);
        serverContexts.put("s2", s2Context);

        /* Remove 2 frontends + one not existing (should do nothing) */
        Set<String> frontendsToRemove = Sets.newHashSet("f4", "f5", "f6");
        /* Remove 2 frontends + one not existing (should do nothing) */
        Set<String> backendsToRemove = Sets.newHashSet("b4", "b5", "b6");
        /* Remove 2 servers + one not existing (should do nothing) */
        Set<String> serverToRemove = Sets.newHashSet("s4", "s5", "s6");

        UpdateEntryPointEvent event = new UpdateEntryPointEvent("correlation_id", key,
                globalContext, frontendContexts, backendContexts, serverContexts, frontendsToRemove, backendsToRemove, serverToRemove);


        handler.handle(event);

        /* Create expected configuration */
        Set<EntryPointFrontend> expectedFrontends = new HashSet<>();
        Set<EntryPointBackend> expectedBackends = new HashSet<>();

        Map<String, String> expectedContext =  new HashMap<>();
        expectedContext.put("key", "global");
        expectedContext.put("not_touched", "not_touched");
        expectedContext.put("new", "new");
        EntryPoint expectedConfiguration = new EntryPoint("haproxy", "user", expectedFrontends, expectedBackends, expectedContext);

        //verify(stateManager).prepare(key, expectedConfiguration);
    }

    @Test
    public void otherwise_entry_point_update_should_create_pending_conf_based_on_committing_conf() {

    }

    @Test
    public void otherwise_entry_point_update_should_create_pending_conf_based_on_current_conf() {

    }

    @Test
    public void try_commit_current_applies_with_right_key() {
        // Given
        EntryPointKey key = new EntryPointKeyDefaultImpl("some_key");
        TryCommitCurrentConfigurationEvent event = new TryCommitCurrentConfigurationEvent(CorrelationId.newCorrelationId(), key);
        EntryPoint entryPoint = EntryPoint
                .onHaproxy("haproxy")
                .withUser("hapuser")
                .definesFrontends(ImmutableSet.<EntryPointFrontend>of())
                .definesBackends(ImmutableSet.<EntryPointBackend>of())
                .withSyslogPort("666")
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
    public void try_commit_pending_applies_with_right_key() {
        // Given
        EntryPointKey key = new EntryPointKeyDefaultImpl("some_key");
        TryCommitPendingConfigurationEvent event = new TryCommitPendingConfigurationEvent(CorrelationId.newCorrelationId(), key);
        EntryPoint entryPoint = EntryPoint
                .onHaproxy("haproxy")
                .withUser("hapuser")
                .definesFrontends(ImmutableSet.<EntryPointFrontend>of())
                .definesBackends(ImmutableSet.<EntryPointBackend>of())
                .withSyslogPort("666")
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
                .withSyslogPort("666")
                .withGlobalContext(ImmutableMap.<String, String>of())
                .build();

        EntryPoint pendingConfig = EntryPoint
                .onHaproxy("haproxy")
                .withUser("hapuser")
                .definesFrontends(ImmutableSet.<EntryPointFrontend>of())
                .definesBackends(ImmutableSet.of(backend))
                .withSyslogPort("666")
                .withGlobalContext(ImmutableMap.<String, String>of())
                .build();

        EntryPoint currentConfig = EntryPoint
                .onHaproxy("haproxy")
                .withUser("hapuser")
                .definesFrontends(ImmutableSet.<EntryPointFrontend>of())
                .definesBackends(ImmutableSet.<EntryPointBackend>of())
                .withSyslogPort("666")
                .withGlobalContext(ImmutableMap.<String, String>of())
                .build();

        EntryPointBackendServer bs = new EntryPointBackendServer("ijklm", "hostname", "10.98.71.1", "9090", new HashMap<>(), new HashMap<>());
        EntryPointBackend expectedBackend = new EntryPointBackend("BACKEND", Sets.newHashSet(bs), new HashMap<>());
        EntryPoint expectedConfig = EntryPoint
                .onHaproxy("haproxy")
                .withUser("hapuser")
                .definesFrontends(ImmutableSet.<EntryPointFrontend>of())
                .definesBackends(ImmutableSet.of(expectedBackend))
                .withSyslogPort("666")
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
                .withSyslogPort("666")
                .withGlobalContext(ImmutableMap.<String, String>of())
                .build();

        EntryPoint currentConfig = EntryPoint
                .onHaproxy("haproxy")
                .withUser("hapuser")
                .definesFrontends(ImmutableSet.<EntryPointFrontend>of())
                .definesBackends(ImmutableSet.<EntryPointBackend>of())
                .withSyslogPort("666")
                .withGlobalContext(ImmutableMap.<String, String>of())
                .build();

        EntryPointBackendServer bs = new EntryPointBackendServer("ijklm", "hostname", "10.98.71.1", "9090", new HashMap<>(), new HashMap<>());
        EntryPointBackend expectedBackend = new EntryPointBackend("BACKEND", Sets.newHashSet(bs), new HashMap<>());
        EntryPoint expectedConfig = EntryPoint
                .onHaproxy("haproxy")
                .withUser("hapuser")
                .definesFrontends(ImmutableSet.<EntryPointFrontend>of())
                .definesBackends(ImmutableSet.of(expectedBackend))
                .withSyslogPort("666")
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
                .withSyslogPort("666")
                .withGlobalContext(ImmutableMap.<String, String>of())
                .build();

        EntryPointBackendServer bs = new EntryPointBackendServer("ijklm", "hostname", "10.98.71.1", "9090", new HashMap<>(), new HashMap<>());
        EntryPointBackend expectedBackend = new EntryPointBackend("BACKEND", Sets.newHashSet(bs), new HashMap<>());
        EntryPoint expectedConfig = EntryPoint
                .onHaproxy("haproxy")
                .withUser("hapuser")
                .definesFrontends(ImmutableSet.<EntryPointFrontend>of())
                .definesBackends(ImmutableSet.of(expectedBackend))
                .withSyslogPort("666")
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
                .withSyslogPort("666")
                .withGlobalContext(ImmutableMap.<String, String>of())
                .build();

        EntryPointBackendServer bs = new EntryPointBackendServer("ijklm", "hostname", "10.98.71.1", "9090", new HashMap<>(), new HashMap<>());
        EntryPointBackend expectedBackend = new EntryPointBackend("BACKEND", Sets.newHashSet(bs), new HashMap<>());
        EntryPoint expectedConfig = EntryPoint
                .onHaproxy("haproxy")
                .withUser("hapuser")
                .definesFrontends(ImmutableSet.<EntryPointFrontend>of())
                .definesBackends(ImmutableSet.of(expectedBackend))
                .withSyslogPort("666")
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
                .withSyslogPort("666")
                .withGlobalContext(ImmutableMap.<String, String>of())
                .build();

        EntryPointBackendServer bs = new EntryPointBackendServer("ijklm", "hostname", "10.98.71.1", "9090", new HashMap<>(), new HashMap<>());
        EntryPointBackend expectedBackend = new EntryPointBackend("BACKEND", Sets.newHashSet(bs), backendContext);
        EntryPoint expectedConfig = EntryPoint
                .onHaproxy("haproxy")
                .withUser("hapuser")
                .definesFrontends(ImmutableSet.<EntryPointFrontend>of())
                .definesBackends(ImmutableSet.of(expectedBackend))
                .withSyslogPort("666")
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
                .withSyslogPort("666")
                .withGlobalContext(ImmutableMap.<String, String>of())
                .build();

        EntryPointBackendServer bs = new EntryPointBackendServer("ijklm", "hostname2", "10.98.71.2", "9092", newServerContext, new HashMap<>());
        EntryPointBackend expectedBackend = new EntryPointBackend("BACKEND", Sets.newHashSet(bs), new HashMap<>());
        EntryPoint expectedConfig = EntryPoint
                .onHaproxy("haproxy")
                .withUser("hapuser")
                .definesFrontends(ImmutableSet.<EntryPointFrontend>of())
                .definesBackends(ImmutableSet.of(expectedBackend))
                .withSyslogPort("666")
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
                .withSyslogPort("666")
                .withGlobalContext(ImmutableMap.<String, String>of())
                .build();

        EntryPointBackendServer expectedServer = new EntryPointBackendServer("ijklm", "hostname2", "10.98.71.2", "9092", newServerContext, oldUserContext);
        EntryPointBackend expectedBackend = new EntryPointBackend("BACKEND", Sets.newHashSet(expectedServer), new HashMap<>());
        EntryPoint expectedConfig = EntryPoint
                .onHaproxy("haproxy")
                .withUser("hapuser")
                .definesFrontends(ImmutableSet.<EntryPointFrontend>of())
                .definesBackends(ImmutableSet.of(expectedBackend))
                .withSyslogPort("666")
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
                .withSyslogPort("666")
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
                .withSyslogPort("666")
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
