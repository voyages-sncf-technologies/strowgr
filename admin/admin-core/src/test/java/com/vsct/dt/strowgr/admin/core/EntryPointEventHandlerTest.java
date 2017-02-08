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
package com.vsct.dt.strowgr.admin.core;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.eventbus.EventBus;
import com.vsct.dt.strowgr.admin.core.configuration.*;
import com.vsct.dt.strowgr.admin.core.event.CorrelationId;
import com.vsct.dt.strowgr.admin.core.event.in.*;
import com.vsct.dt.strowgr.admin.core.event.out.CommitRequestedEvent;
import com.vsct.dt.strowgr.admin.core.repository.HaproxyRepository;
import com.vsct.dt.strowgr.admin.core.repository.PortRepository;
import org.junit.Test;
import org.reactivestreams.Subscriber;

import java.util.*;

import static com.google.common.collect.Sets.newHashSet;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

public class EntryPointEventHandlerTest {

    private final EntryPointStateManager stateManager = mock(EntryPointStateManager.class);

    private final TemplateLocator templateLocator = mock(TemplateLocator.class);

    private final PortRepository portRepository = mock(PortRepository.class);

    private final HaproxyRepository haproxyRepository = mock(HaproxyRepository.class);

    private final TemplateGenerator templateGenerator = mock(TemplateGenerator.class);

    @SuppressWarnings("unchecked")
    private final Subscriber<CommitRequestedEvent> commitRequestedSubscriber = mock(Subscriber.class);

    private final EventBus outputBus = mock(EventBus.class);

    private final EntryPointEventHandler handler = new EntryPointEventHandler(stateManager, portRepository, haproxyRepository, templateLocator, templateGenerator, commitRequestedSubscriber);

    @Test
    public void try_commit_current_applies_with_right_key() throws IncompleteConfigurationException {
        // Given
        EntryPointKey key = new EntryPointKeyDefaultImpl("some_key");
        String correlationId = CorrelationId.newCorrelationId();
        TryCommitCurrentConfigurationEvent event = new TryCommitCurrentConfigurationEvent(correlationId, key);
        EntryPoint entryPoint = EntryPoint
                .onHaproxy("haproxy", 1)
                .withUser("hapuser")
                .withVersion("hapversion")
                .definesFrontends(ImmutableSet.of())
                .definesBackends(ImmutableSet.of())
                .withGlobalContext(ImmutableMap.of())
                .build();

        when(stateManager.lock(key)).thenReturn(true);
        when(stateManager.tryCommitCurrent(correlationId, key)).thenReturn(Optional.of(entryPoint));
        when(portRepository.getPort(key, EntryPoint.SYSLOG_PORT_ID)).thenReturn(Optional.of(666));
        when(templateLocator.readTemplate(entryPoint)).thenReturn(Optional.of("some template"));
        when(templateGenerator.generate(eq("some template"), eq(entryPoint), any())).thenReturn("some template");
        when(templateGenerator.generateSyslogFragment(eq(entryPoint), any())).thenReturn("some syslog conf");
        when(haproxyRepository.getHaproxyProperty("haproxy", "binding/1")).thenReturn(Optional.of("127.0.0.1"));
        when(haproxyRepository.isAutoreload("haproxy")).thenReturn(true);
        when(stateManager.isAutoreloaded(key)).thenReturn(true);

        // Test
        handler.handle(event);

        // Check
        verify(stateManager).tryCommitCurrent(correlationId, key);

        CommitRequestedEvent commitRequestedEvent = new CommitRequestedEvent(correlationId, new EntryPointKeyDefaultImpl("some_key"), entryPoint, "some template", "some syslog conf", "127.0.0.1");
        verify(commitRequestedSubscriber).onNext(commitRequestedEvent);
    }

    @Test
    public void try_commit_current_with_no_autoreload_haproxy() throws IncompleteConfigurationException {
        // Given
        EntryPointKey key = new EntryPointKeyDefaultImpl("some_key");
        String correlationId = CorrelationId.newCorrelationId();
        TryCommitCurrentConfigurationEvent event = new TryCommitCurrentConfigurationEvent(correlationId, key);
        EntryPoint entryPoint = EntryPoint
                .onHaproxy("haproxy", 1)
                .withUser("hapuser")
                .withVersion("hapversion")
                .definesFrontends(ImmutableSet.of())
                .definesBackends(ImmutableSet.of())
                .withGlobalContext(ImmutableMap.of())
                .build();

        when(stateManager.lock(key)).thenReturn(true);
        when(stateManager.tryCommitCurrent(correlationId, key)).thenReturn(Optional.of(entryPoint));
        when(portRepository.getPort(key, EntryPoint.SYSLOG_PORT_ID)).thenReturn(Optional.of(666));
        when(templateLocator.readTemplate(entryPoint)).thenReturn(Optional.of("some template"));
        when(templateGenerator.generate(eq("some template"), eq(entryPoint), any())).thenReturn("some template");
        when(templateGenerator.generateSyslogFragment(eq(entryPoint), any())).thenReturn("some syslog conf");
        when(haproxyRepository.getHaproxyProperty("haproxy", "binding/1")).thenReturn(Optional.of("127.0.0.1"));
        when(haproxyRepository.isAutoreload("haproxy")).thenReturn(false);


        // Test
        handler.handle(event);

        // Check
        verify(stateManager).tryCommitCurrent(correlationId, key);
        verify(stateManager).cancelCommit(key);
        verify(commitRequestedSubscriber, never()).onNext(any(CommitRequestedEvent.class));
    }

    @Test
    public void try_commit_pending_applies_with_right_key() throws IncompleteConfigurationException {
        // Given
        EntryPointKey key = new EntryPointKeyDefaultImpl("some_key");
        String correlationId = CorrelationId.newCorrelationId();
        TryCommitPendingConfigurationEvent event = new TryCommitPendingConfigurationEvent(correlationId, key);
        EntryPoint entryPoint = EntryPoint
                .onHaproxy("haproxy", 1)
                .withUser("hapuser")
                .withVersion("hapversion")
                .definesFrontends(ImmutableSet.of())
                .definesBackends(ImmutableSet.of())
                .withGlobalContext(ImmutableMap.of())
                .build();

        when(stateManager.lock(key)).thenReturn(true);
        when(stateManager.tryCommitPending(correlationId, key)).thenReturn(Optional.of(entryPoint));
        when(portRepository.getPort(key, EntryPoint.SYSLOG_PORT_ID)).thenReturn(Optional.of(666));
        when(templateLocator.readTemplate(entryPoint)).thenReturn(Optional.of("some template"));
        when(templateGenerator.generate(eq("some template"), eq(entryPoint), any())).thenReturn("some template");
        when(templateGenerator.generateSyslogFragment(eq(entryPoint), any())).thenReturn("some syslog conf");
        when(haproxyRepository.getHaproxyProperty("haproxy", "binding/1")).thenReturn(Optional.of("127.0.0.1"));
        when(stateManager.isAutoreloaded(key)).thenReturn(true);
        when(haproxyRepository.isAutoreload("haproxy")).thenReturn(true);

        // Test
        handler.handle(event);

        // Check
        verify(stateManager).tryCommitPending(correlationId, key);
        CommitRequestedEvent commitRequestedEvent = new CommitRequestedEvent(correlationId, new EntryPointKeyDefaultImpl("some_key"), entryPoint, "some template", "some syslog conf", "127.0.0.1");
        verify(commitRequestedSubscriber).onNext(commitRequestedEvent);
    }

    @Test
    public void try_commit_pending_with_no_autoreload_haproxy() throws IncompleteConfigurationException {
        // Given
        EntryPointKey key = new EntryPointKeyDefaultImpl("some_key");
        String correlationId = CorrelationId.newCorrelationId();
        TryCommitPendingConfigurationEvent event = new TryCommitPendingConfigurationEvent(correlationId, key);
        EntryPoint entryPoint = EntryPoint
                .onHaproxy("haproxy", 1)
                .withUser("hapuser")
                .withVersion("hapversion")
                .definesFrontends(ImmutableSet.of())
                .definesBackends(ImmutableSet.of())
                .withGlobalContext(ImmutableMap.of())
                .build();

        when(stateManager.lock(key)).thenReturn(true);
        when(stateManager.tryCommitPending(correlationId, key)).thenReturn(Optional.of(entryPoint));
        when(portRepository.getPort(key, EntryPoint.SYSLOG_PORT_ID)).thenReturn(Optional.of(666));
        when(templateLocator.readTemplate(entryPoint)).thenReturn(Optional.of("some template"));
        when(templateGenerator.generate(eq("some template"), eq(entryPoint), any())).thenReturn("some template");
        when(templateGenerator.generateSyslogFragment(eq(entryPoint), any())).thenReturn("some syslog conf");
        when(haproxyRepository.getHaproxyProperty("haproxy", "binding/1")).thenReturn(Optional.of("127.0.0.1"));
        when(haproxyRepository.isAutoreload("haproxy")).thenReturn(false);

        // Test
        handler.handle(event);

        // Check
        verify(stateManager).tryCommitPending(correlationId, key);
        verify(stateManager).cancelCommit(key);
        verify(commitRequestedSubscriber, never()).onNext(any(CommitRequestedEvent.class));
    }

    @Test
    public void commit_success_event_does_nothing_if_commit_correlationid_does_not_exists() {
        EntryPointKey key = new EntryPointKeyDefaultImpl("some_key");
        CommitSuccessEvent event = new CommitSuccessEvent(CorrelationId.newCorrelationId(), key);

        when(stateManager.lock(key)).thenReturn(true);
        when(stateManager.getCommitCorrelationId(key)).thenReturn(Optional.empty());
        when(stateManager.commit(key)).thenReturn(Optional.empty());
        handler.handle(event);

        verify(stateManager, never()).commit(any());
    }

    @Test
    public void commit_success_event_does_nothing_if_commit_correlationid_does_not_match() {
        EntryPointKey key = new EntryPointKeyDefaultImpl("some_key");
        CommitSuccessEvent event = new CommitSuccessEvent(CorrelationId.newCorrelationId(), key);

        when(stateManager.lock(key)).thenReturn(true);
        when(stateManager.getCommitCorrelationId(key)).thenReturn(Optional.of(CorrelationId.newCorrelationId()));
        when(stateManager.commit(key)).thenReturn(Optional.empty());
        handler.handle(event);

        verify(stateManager, never()).commit(any());
    }

    @Test
    public void commit_success_event_applies_commit_if_correlation_id_matches() {
        EntryPointKey key = new EntryPointKeyDefaultImpl("some_key");
        String correlationId = CorrelationId.newCorrelationId();
        CommitSuccessEvent event = new CommitSuccessEvent(correlationId, key);

        when(stateManager.lock(key)).thenReturn(true);
        when(stateManager.getCommitCorrelationId(key)).thenReturn(Optional.of(correlationId));
        when(stateManager.commit(key)).thenReturn(Optional.empty());
        handler.handle(event);

        verify(stateManager).commit(key);
    }

    @Test
    public void commit_failure_event_does_nothing_if_there_is_no_committing_configuration() {
        EntryPointKey key = new EntryPointKeyDefaultImpl("some_key");
        CommitFailureEvent commitFailureEvent = new CommitFailureEvent(CorrelationId.newCorrelationId(), key);

        when(stateManager.lock(key)).thenReturn(true);
        when(stateManager.getCommitCorrelationId(key)).thenReturn(Optional.empty());

        handler.handle(commitFailureEvent);

        verify(stateManager, never()).cancelCommit(any());
    }

    @Test
    public void commit_failure_event_does_nothing_if_the_correlation_id_does_not_match_the_event_that_trigerred_it() {
        EntryPointKey key = new EntryPointKeyDefaultImpl("some_key");
        CommitFailureEvent commitFailureEvent = new CommitFailureEvent(CorrelationId.newCorrelationId(), key);

        when(stateManager.lock(key)).thenReturn(true);
        when(stateManager.getCommitCorrelationId(key)).thenReturn(Optional.of(CorrelationId.newCorrelationId()));

        handler.handle(commitFailureEvent);

        verify(stateManager, never()).cancelCommit(any());
    }

    @Test
    public void commit_failure_event_removes_committing_configuration_if_correlation_id_matches() {
        EntryPointKey key = new EntryPointKeyDefaultImpl("some_key");
        String correlationId = CorrelationId.newCorrelationId();
        CommitFailureEvent commitFailureEvent = new CommitFailureEvent(correlationId, key);

        when(stateManager.lock(key)).thenReturn(true);
        when(stateManager.getCommitCorrelationId(key)).thenReturn(Optional.of(correlationId));

        handler.handle(commitFailureEvent);

        verify(stateManager).cancelCommit(key);
    }

    /* Server registration tests are made on one server but they apply to a set of server provided by the event */
    @Test
    public void server_registration_should_do_nothing_if_there_is_no_configuration_at_all() {
        EntryPointKey key = new EntryPointKeyDefaultImpl("some_key");
        IncomingEntryPointBackendServer server = new IncomingEntryPointBackendServer("ijklm", "10.98.71.1", "9090", new HashMap<>());
        RegisterServerEvent event = new RegisterServerEvent(CorrelationId.newCorrelationId(), key, "BACKEND", ImmutableSet.of(server));

        when(stateManager.lock(key)).thenReturn(true);
        when(stateManager.getCommittingConfiguration(key)).thenReturn(Optional.empty());
        when(stateManager.getPendingConfiguration(key)).thenReturn(Optional.empty());
        when(stateManager.getCurrentConfiguration(key)).thenReturn(Optional.empty());

        handler.handle(event);

        verify(stateManager, never()).prepare(any(), any());
    }

    @Test
    public void server_registration_should_change_configuration_based_on_pending_one() {
        EntryPointKey key = new EntryPointKeyDefaultImpl("some_key");
        IncomingEntryPointBackendServer server = new IncomingEntryPointBackendServer("ijklm", "10.98.71.1", "9090", new HashMap<>());
        RegisterServerEvent event = new RegisterServerEvent(CorrelationId.newCorrelationId(), key, "BACKEND", ImmutableSet.of(server));

        EntryPointBackend backend = new EntryPointBackend("BACKEND");

        EntryPoint commmittingConfig = EntryPoint
                .onHaproxy("haproxy", 0)
                .withUser("hapuser")
                .withVersion("hapversion")
                .definesFrontends(ImmutableSet.of())
                .definesBackends(ImmutableSet.of())
                .withGlobalContext(ImmutableMap.of())
                .build();

        EntryPoint pendingConfig = EntryPoint
                .onHaproxy("haproxy", 0)
                .withUser("hapuser")
                .withVersion("hapversion")
                .definesFrontends(ImmutableSet.of())
                .definesBackends(ImmutableSet.of(backend))
                .withGlobalContext(ImmutableMap.of())
                .build();

        EntryPoint currentConfig = EntryPoint
                .onHaproxy("haproxy", 0)
                .withUser("hapuser")
                .withVersion("hapversion")
                .definesFrontends(ImmutableSet.of())
                .definesBackends(ImmutableSet.of())
                .withGlobalContext(ImmutableMap.of())
                .build();

        EntryPointBackendServer bs = new EntryPointBackendServer("ijklm", "10.98.71.1", "9090", new HashMap<>(), new HashMap<>());
        EntryPointBackend expectedBackend = new EntryPointBackend("BACKEND", newHashSet(bs), new HashMap<>());
        EntryPoint expectedConfig = EntryPoint
                .onHaproxy("haproxy", 0)
                .withUser("hapuser")
                .withVersion("hapversion")
                .definesFrontends(ImmutableSet.of())
                .definesBackends(ImmutableSet.of(expectedBackend))
                .withGlobalContext(ImmutableMap.of())
                .build();


        when(stateManager.lock(key)).thenReturn(true);
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
        IncomingEntryPointBackendServer server = new IncomingEntryPointBackendServer("ijklm", "10.98.71.1", "9090", new HashMap<>());
        RegisterServerEvent event = new RegisterServerEvent(CorrelationId.newCorrelationId(), key, "BACKEND", ImmutableSet.of(server));

        EntryPointBackend backend = new EntryPointBackend("BACKEND");

        EntryPoint commmittingConfig = EntryPoint
                .onHaproxy("haproxy", 0)
                .withUser("hapuser")
                .withVersion("hapversion")
                .definesFrontends(ImmutableSet.of())
                .definesBackends(ImmutableSet.of(backend))
                .withGlobalContext(ImmutableMap.of())
                .build();

        EntryPoint currentConfig = EntryPoint
                .onHaproxy("haproxy", 0)
                .withUser("hapuser")
                .withVersion("hapversion")
                .definesFrontends(ImmutableSet.of())
                .definesBackends(ImmutableSet.of())
                .withGlobalContext(ImmutableMap.of())
                .build();

        EntryPointBackendServer bs = new EntryPointBackendServer("ijklm", "10.98.71.1", "9090", new HashMap<>(), new HashMap<>());
        EntryPointBackend expectedBackend = new EntryPointBackend("BACKEND", newHashSet(bs), new HashMap<>());
        EntryPoint expectedConfig = EntryPoint
                .onHaproxy("haproxy", 0)
                .withUser("hapuser")
                .withVersion("hapversion")
                .definesFrontends(ImmutableSet.of())
                .definesBackends(ImmutableSet.of(expectedBackend))
                .withGlobalContext(ImmutableMap.of())
                .build();


        when(stateManager.lock(key)).thenReturn(true);
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
        IncomingEntryPointBackendServer server = new IncomingEntryPointBackendServer("ijklm", "10.98.71.1", "9090", new HashMap<>());
        RegisterServerEvent event = new RegisterServerEvent(CorrelationId.newCorrelationId(), key, "BACKEND", ImmutableSet.of(server));

        EntryPointBackend backend = new EntryPointBackend("BACKEND");

        EntryPoint currentConfig = EntryPoint
                .onHaproxy("haproxy", 0)
                .withUser("hapuser")
                .withVersion("hapversion")
                .definesFrontends(ImmutableSet.of())
                .definesBackends(ImmutableSet.of(backend))
                .withGlobalContext(ImmutableMap.of())
                .build();

        EntryPointBackendServer bs = new EntryPointBackendServer("ijklm", "10.98.71.1", "9090", new HashMap<>(), new HashMap<>());
        EntryPointBackend expectedBackend = new EntryPointBackend("BACKEND", newHashSet(bs), new HashMap<>());
        EntryPoint expectedConfig = EntryPoint
                .onHaproxy("haproxy", 0)
                .withUser("hapuser")
                .withVersion("hapversion")
                .definesFrontends(ImmutableSet.of())
                .definesBackends(ImmutableSet.of(expectedBackend))
                .withGlobalContext(ImmutableMap.of())
                .build();

        when(stateManager.lock(key)).thenReturn(true);
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
        IncomingEntryPointBackendServer server = new IncomingEntryPointBackendServer("ijklm", "10.98.71.1", "9090", new HashMap<>());
        RegisterServerEvent event = new RegisterServerEvent(CorrelationId.newCorrelationId(), key, "BACKEND", ImmutableSet.of(server));

        EntryPoint currentConfig = EntryPoint
                .onHaproxy("haproxy", 0)
                .withUser("hapuser")
                .withVersion("hapversion")
                .definesFrontends(ImmutableSet.of())
                .definesBackends(ImmutableSet.of())
                .withGlobalContext(ImmutableMap.of())
                .build();

        EntryPointBackendServer bs = new EntryPointBackendServer("ijklm", "10.98.71.1", "9090", new HashMap<>(), new HashMap<>());
        EntryPointBackend expectedBackend = new EntryPointBackend("BACKEND", newHashSet(bs), new HashMap<>());
        EntryPoint expectedConfig = EntryPoint
                .onHaproxy("haproxy", 0)
                .withUser("hapuser")
                .withVersion("hapversion")
                .definesFrontends(ImmutableSet.of())
                .definesBackends(ImmutableSet.of(expectedBackend))
                .withGlobalContext(ImmutableMap.of())
                .build();

        when(stateManager.lock(key)).thenReturn(true);
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
        IncomingEntryPointBackendServer server = new IncomingEntryPointBackendServer("ijklm", "10.98.71.1", "9090", new HashMap<>());
        Map<String, String> backendContext = new HashMap<>();
        backendContext.put("key1", "value1");
        backendContext.put("key2", "value2");
        EntryPointBackend backend = new EntryPointBackend("BACKEND", new HashSet<>(), backendContext);

        RegisterServerEvent event = new RegisterServerEvent(CorrelationId.newCorrelationId(), key, "BACKEND", ImmutableSet.of(server));

        EntryPoint currentConfig = EntryPoint
                .onHaproxy("haproxy", 0)
                .withUser("hapuser")
                .withVersion("hapversion")
                .definesFrontends(ImmutableSet.of())
                .definesBackends(ImmutableSet.of(backend))
                .withGlobalContext(ImmutableMap.of())
                .build();

        EntryPointBackendServer bs = new EntryPointBackendServer("ijklm", "10.98.71.1", "9090", new HashMap<>(), new HashMap<>());
        EntryPointBackend expectedBackend = new EntryPointBackend("BACKEND", newHashSet(bs), backendContext);
        EntryPoint expectedConfig = EntryPoint
                .onHaproxy("haproxy", 0)
                .withUser("hapuser")
                .withVersion("hapversion")
                .definesFrontends(ImmutableSet.of())
                .definesBackends(ImmutableSet.of(expectedBackend))
                .withGlobalContext(ImmutableMap.of())
                .build();

        when(stateManager.lock(key)).thenReturn(true);
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
        EntryPointBackendServer oldServer = new EntryPointBackendServer("ijklm", "10.98.71.1", "9090", oldServerContext, new HashMap<>());

        Map<String, String> newServerContext = new HashMap<>();
        newServerContext.put("key1", "value1bis");
        newServerContext.put("key2", "value2bis");
        newServerContext.put("key3", "value3");
        IncomingEntryPointBackendServer newServer = new IncomingEntryPointBackendServer("ijklm", "10.98.71.2", "9092", newServerContext);

        EntryPointBackend backend = new EntryPointBackend("BACKEND", newHashSet(oldServer), new HashMap<>());

        RegisterServerEvent event = new RegisterServerEvent(CorrelationId.newCorrelationId(), key, "BACKEND", ImmutableSet.of(newServer));

        EntryPoint currentConfig = EntryPoint
                .onHaproxy("haproxy", 0)
                .withUser("hapuser")
                .withVersion("hapversion")
                .definesFrontends(ImmutableSet.of())
                .definesBackends(ImmutableSet.of(backend))
                .withGlobalContext(ImmutableMap.of())
                .build();

        EntryPointBackendServer bs = new EntryPointBackendServer("ijklm", "10.98.71.2", "9092", newServerContext, new HashMap<>());
        EntryPointBackend expectedBackend = new EntryPointBackend("BACKEND", newHashSet(bs), new HashMap<>());
        EntryPoint expectedConfig = EntryPoint
                .onHaproxy("haproxy", 0)
                .withUser("hapuser")
                .withVersion("hapversion")
                .definesFrontends(ImmutableSet.of())
                .definesBackends(ImmutableSet.of(expectedBackend))
                .withGlobalContext(ImmutableMap.of())
                .build();

        when(stateManager.lock(key)).thenReturn(true);
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

        EntryPointBackendServer oldServer = new EntryPointBackendServer("ijklm", "10.98.71.1", "9090", oldServerContext, oldUserContext);
        EntryPointBackend backend = new EntryPointBackend("BACKEND", newHashSet(oldServer), new HashMap<>());

        Map<String, String> newServerContext = new HashMap<>();
        newServerContext.put("key1", "value1bis");
        newServerContext.put("key2", "value2bis");
        IncomingEntryPointBackendServer newServerInEvent = new IncomingEntryPointBackendServer("ijklm", "10.98.71.2", "9092", newServerContext);

        RegisterServerEvent event = new RegisterServerEvent(CorrelationId.newCorrelationId(), key, "BACKEND", ImmutableSet.of(newServerInEvent));

        EntryPoint currentConfig = EntryPoint
                .onHaproxy("haproxy", 0)
                .withUser("hapuser")
                .withVersion("hapversion")
                .definesFrontends(ImmutableSet.of())
                .definesBackends(ImmutableSet.of(backend))
                .withGlobalContext(ImmutableMap.of())
                .build();

        EntryPointBackendServer expectedServer = new EntryPointBackendServer("ijklm", "10.98.71.2", "9092", newServerContext, oldUserContext);
        EntryPointBackend expectedBackend = new EntryPointBackend("BACKEND", newHashSet(expectedServer), new HashMap<>());
        EntryPoint expectedConfig = EntryPoint
                .onHaproxy("haproxy", 0)
                .withUser("hapuser")
                .withVersion("hapversion")
                .definesFrontends(ImmutableSet.of())
                .definesBackends(ImmutableSet.of(expectedBackend))
                .withGlobalContext(ImmutableMap.of())
                .build();

        when(stateManager.lock(key)).thenReturn(true);
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
        EntryPointBackendServer server = new EntryPointBackendServer("ijklm", "10.98.71.1", "9090", serverContext, serverUserContext);

        Map<String, String> backendContext = new HashMap<>();
        backendContext.put("key1", "value1");
        backendContext.put("key2", "value2");
        EntryPointBackend backend = new EntryPointBackend("BACKEND", newHashSet(server), backendContext);

        EntryPoint currentConfig = EntryPoint
                .onHaproxy("haproxy", 0)
                .withUser("hapuser")
                .withVersion("hapversion")
                .definesFrontends(ImmutableSet.of())
                .definesBackends(ImmutableSet.of(backend))
                .withGlobalContext(ImmutableMap.of())
                .build();

        IncomingEntryPointBackendServer newServerInEvent = new IncomingEntryPointBackendServer("ijklm", "10.98.71.2", "9092", new HashMap<>());
        RegisterServerEvent event = new RegisterServerEvent(CorrelationId.newCorrelationId(), key, "NEWBACKEND", ImmutableSet.of(newServerInEvent));

        EntryPointBackendServer expectedServer = new EntryPointBackendServer("ijklm", "10.98.71.2", "9092", new HashMap<>(), serverUserContext);
        EntryPointBackend expectedBackend1 = new EntryPointBackend("BACKEND", newHashSet(), backendContext);
        EntryPointBackend expectedBackend2 = new EntryPointBackend("NEWBACKEND", newHashSet(expectedServer), new HashMap<>());

        EntryPoint expectedConfig = EntryPoint
                .onHaproxy("haproxy", 0)
                .withUser("hapuser")
                .withVersion("hapversion")
                .definesFrontends(ImmutableSet.of())
                .definesBackends(ImmutableSet.of(expectedBackend1, expectedBackend2))
                .withGlobalContext(ImmutableMap.of())
                .build();

        when(stateManager.lock(key)).thenReturn(true);
        when(stateManager.getCommittingConfiguration(key)).thenReturn(Optional.empty());
        when(stateManager.getPendingConfiguration(key)).thenReturn(Optional.empty());
        when(stateManager.getCurrentConfiguration(key)).thenReturn(Optional.of(currentConfig));
        when(stateManager.prepare(key, expectedConfig)).thenReturn(Optional.of(expectedConfig));

        handler.handle(event);

        verify(stateManager).prepare(eq(key), eq(expectedConfig));
    }


}
