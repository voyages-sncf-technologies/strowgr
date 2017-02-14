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
package com.vsct.dt.strowgr.admin.gui.resource.api;

import com.vsct.dt.strowgr.admin.core.EntryPointKey;
import com.vsct.dt.strowgr.admin.core.configuration.EntryPoint;
import com.vsct.dt.strowgr.admin.core.entrypoint.*;
import com.vsct.dt.strowgr.admin.core.event.in.*;
import com.vsct.dt.strowgr.admin.core.event.out.DeleteEntryPointEvent;
import com.vsct.dt.strowgr.admin.core.repository.EntryPointRepository;
import com.vsct.dt.strowgr.admin.gui.mapping.json.EntryPointMappingJson;
import com.vsct.dt.strowgr.admin.gui.mapping.json.UpdatedEntryPointMappingJson;
import com.vsct.dt.strowgr.admin.gui.resource.IncomingEntryPointBackendServerJsonRepresentation;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.reactivestreams.Subscriber;

import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Optional;

import static javax.ws.rs.core.Response.Status.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

public class EntryPointResourcesTest {

    private final EntryPointRepository entryPointRepository = mock(EntryPointRepository.class);

    @SuppressWarnings("unchecked")
    private final Subscriber<AutoReloadConfigEvent> autoReloadConfigSubscriber = mock(Subscriber.class);

    @SuppressWarnings("unchecked")
    private final Subscriber<AddEntryPointEvent> addEntryPointSubscriber = mock(Subscriber.class);

    @SuppressWarnings("unchecked")
    private final Subscriber<UpdateEntryPointEvent> updatedEntryPointSubscriber = mock(Subscriber.class);

    @SuppressWarnings("unchecked")
    private Subscriber<DeleteEntryPointEvent> deleteEntryPointSubscriber = mock(Subscriber.class);

    @SuppressWarnings("unchecked")
    private Subscriber<TryCommitPendingConfigurationEvent> tryCommitPendingConfigurationSubscriber = mock(Subscriber.class);

    @SuppressWarnings("unchecked")
    private Subscriber<TryCommitCurrentConfigurationEvent> tryCommitCurrentConfigurationSubscriber = mock(Subscriber.class);

    @SuppressWarnings("unchecked")
    private Subscriber<RegisterServerEvent> registerServerSubscriber = mock(Subscriber.class);

    @SuppressWarnings("unchecked")
    private Subscriber<CommitSuccessEvent> commitSuccessSubscriber = mock(Subscriber.class);

    @SuppressWarnings("unchecked")
    private Subscriber<CommitFailureEvent> commitFailureSubscriber = mock(Subscriber.class);

    @SuppressWarnings("unchecked")
    private ArgumentCaptor<AutoReloadConfigEvent> autoReloadEventCaptor = (ArgumentCaptor) ArgumentCaptor.forClass(AutoReloadConfigEvent.class);

    @SuppressWarnings("unchecked")
    private ArgumentCaptor<AddEntryPointEvent> addEntryPointEventCaptor = (ArgumentCaptor) ArgumentCaptor.forClass(AutoReloadConfigEvent.class);

    @SuppressWarnings("unchecked")
    private ArgumentCaptor<UpdateEntryPointEvent> updateEntryPointEventCaptor = (ArgumentCaptor) ArgumentCaptor.forClass(AutoReloadConfigEvent.class);

    private final EntryPointResources entryPointResources = new EntryPointResources(entryPointRepository,
            autoReloadConfigSubscriber, addEntryPointSubscriber, updatedEntryPointSubscriber, deleteEntryPointSubscriber,
            tryCommitPendingConfigurationSubscriber, tryCommitCurrentConfigurationSubscriber, registerServerSubscriber,
            commitSuccessSubscriber, commitFailureSubscriber);

    @Test
    public void swap_auto_reload_should_return_partial_response_on_handler_success() throws Exception {
        // given
        AsyncResponse asyncResponse = mock(AsyncResponse.class);
        String entryPointKey = "entryPointKey";
        ArgumentCaptor<Response> responseCaptor = ArgumentCaptor.forClass(Response.class);

        // when
        entryPointResources.swapAutoReload(asyncResponse, entryPointKey);
        verify(autoReloadConfigSubscriber).onNext(autoReloadEventCaptor.capture());
        autoReloadEventCaptor.getValue().onSuccess(mock(AutoReloadConfigResponse.class));

        // verify
        assertThat(autoReloadEventCaptor.getValue().getKey().getID()).isEqualTo(entryPointKey);
        verify(asyncResponse).resume(responseCaptor.capture());
        assertThat(responseCaptor.getValue()).isNotNull();
        assertThat(responseCaptor.getValue().getStatus()).isEqualTo(Status.PARTIAL_CONTENT.getStatusCode());
    }

    @Test
    public void swap_auto_reload_should_call_resume_with_error_on_handler_error() throws Exception {
        // given
        AsyncResponse asyncResponse = mock(AsyncResponse.class);
        String entryPointKey = "entryPointKey";
        RuntimeException exception = new RuntimeException();

        // when
        entryPointResources.swapAutoReload(asyncResponse, entryPointKey);
        verify(autoReloadConfigSubscriber).onNext(autoReloadEventCaptor.capture());
        autoReloadEventCaptor.getValue().onError(exception);

        // then
        assertThat(autoReloadEventCaptor.getValue().getKey().getID()).isEqualTo(entryPointKey);
        verify(asyncResponse).resume(exception);
    }

    @Test
    public void add_entry_point_should_return_created_response_on_handler_success() throws Exception {
        // given
        EntryPointMappingJson entryPoint = mock(EntryPointMappingJson.class);
        AsyncResponse asyncResponse = mock(AsyncResponse.class);
        String entryPointKey = "entryPointKey";
        ArgumentCaptor<Response> responseCaptor = ArgumentCaptor.forClass(Response.class);
        AddEntryPointResponse responseBody = mock(AddEntryPointResponse.class);
        when(responseBody.getConfiguration()).thenReturn(entryPoint);

        // when
        entryPointResources.addEntryPoint(asyncResponse, entryPointKey, entryPoint);
        verify(addEntryPointSubscriber).onNext(addEntryPointEventCaptor.capture());
        addEntryPointEventCaptor.getValue().onSuccess(responseBody);

        // verify
        assertThat(addEntryPointEventCaptor.getValue().getKey().getID()).isEqualTo(entryPointKey);
        assertThat(addEntryPointEventCaptor.getValue().getConfiguration()).isEqualTo(entryPoint);
        verify(asyncResponse).resume(responseCaptor.capture());
        assertThat(responseCaptor.getValue()).isNotNull();
        assertThat(responseCaptor.getValue().getStatus()).isEqualTo(Status.CREATED.getStatusCode());
        assertThat(responseCaptor.getValue().getEntity()).isEqualTo(entryPoint);
    }

    @Test
    public void add_entry_point_should_call_resume_with_error_on_handler_error() throws Exception {
        // given
        EntryPointMappingJson entryPoint = mock(EntryPointMappingJson.class);
        AsyncResponse asyncResponse = mock(AsyncResponse.class);
        String entryPointKey = "entryPointKey";
        RuntimeException exception = new RuntimeException();

        // when
        entryPointResources.addEntryPoint(asyncResponse, entryPointKey, entryPoint);
        verify(addEntryPointSubscriber).onNext(addEntryPointEventCaptor.capture());
        addEntryPointEventCaptor.getValue().onError(exception);

        // then
        assertThat(addEntryPointEventCaptor.getValue().getKey().getID()).isEqualTo(entryPointKey);
        assertThat(addEntryPointEventCaptor.getValue().getConfiguration()).isEqualTo(entryPoint);
        verify(asyncResponse).resume(exception);
    }

    @Test
    public void update_entry_point_should_return_ok_response_on_handler_success() throws Exception {
        // given
        UpdatedEntryPointMappingJson updatedEntryPointMappingJson = mock(UpdatedEntryPointMappingJson.class);
        EntryPoint expected = mock(EntryPoint.class);
        AsyncResponse asyncResponse = mock(AsyncResponse.class);
        String entryPointKey = "entryPointKey";
        ArgumentCaptor<Response> responseCaptor = ArgumentCaptor.forClass(Response.class);
        UpdateEntryPointResponse responseBody = mock(UpdateEntryPointResponse.class);
        when(responseBody.getConfiguration()).thenReturn(expected);

        // when
        entryPointResources.updateEntryPoint(asyncResponse, entryPointKey, updatedEntryPointMappingJson);
        verify(updatedEntryPointSubscriber).onNext(updateEntryPointEventCaptor.capture());
        updateEntryPointEventCaptor.getValue().onSuccess(responseBody);

        // verify
        assertThat(updateEntryPointEventCaptor.getValue().getKey().getID()).isEqualTo(entryPointKey);
        assertThat(updateEntryPointEventCaptor.getValue().getUpdatedEntryPoint()).isEqualTo(updatedEntryPointMappingJson);
        verify(asyncResponse).resume(responseCaptor.capture());
        assertThat(responseCaptor.getValue()).isNotNull();
        assertThat(responseCaptor.getValue()).isEqualTo(expected);
    }

    @Test
    public void update_entry_point_should_call_resume_with_error_on_handler_error() throws Exception {
        // given
        UpdatedEntryPointMappingJson updatedEntryPointMappingJson = mock(UpdatedEntryPointMappingJson.class);
        AsyncResponse asyncResponse = mock(AsyncResponse.class);
        String entryPointKey = "entryPointKey";
        RuntimeException exception = new RuntimeException();

        // when
        entryPointResources.updateEntryPoint(asyncResponse, entryPointKey, updatedEntryPointMappingJson);
        verify(updatedEntryPointSubscriber).onNext(updateEntryPointEventCaptor.capture());
        updateEntryPointEventCaptor.getValue().onError(exception);

        // then
        assertThat(updateEntryPointEventCaptor.getValue().getKey().getID()).isEqualTo(entryPointKey);
        assertThat(updateEntryPointEventCaptor.getValue().getUpdatedEntryPoint()).isEqualTo(updatedEntryPointMappingJson);
        verify(asyncResponse).resume(exception);
    }

    @Test
    public void should_send_delete_entry_point_event_and_return_204_when_delete_an_entry_point() {
        // given
        when(entryPointRepository.removeEntrypoint(any(EntryPointKey.class))).thenReturn(Optional.of(Boolean.TRUE));
        when(entryPointRepository.getCurrentConfiguration(any(EntryPointKey.class))).thenReturn(Optional.of(new EntryPoint("default-name", "hapadm", "hapVersion", 0, new HashSet<>(), new HashSet<>(), new HashMap<>())));

        // test
        Response response = entryPointResources.deleteEntrypoint("MY_APP/MY_PLTF");

        // check
        assertThat(response.getStatus()).isEqualTo(OK.getStatusCode());
        verify(deleteEntryPointSubscriber).onNext(any(DeleteEntryPointEvent.class));
        verify(entryPointRepository, times(1)).getEntryPointsId();
    }

    @Test
    public void should_not_post_delete_event_and_return_404_when_delete_an_non_existing_entry_point() {
        // given
        when(entryPointRepository.getCurrentConfiguration(any(EntryPointKey.class))).thenReturn(Optional.of(new EntryPoint("default-name", "hapadm", "hapVersion", 0, new HashSet<>(), new HashSet<>(), new HashMap<>())));
        when(entryPointRepository.getCurrentConfiguration(any(EntryPointKey.class))).thenReturn(Optional.empty());

        // test
        Response response = entryPointResources.deleteEntrypoint("MY_APP/MY_PLTF");

        // check
        assertThat(response.getStatus()).isEqualTo(NOT_FOUND.getStatusCode());
        verify(deleteEntryPointSubscriber, never()).onNext(any(DeleteEntryPointEvent.class));
        verify(entryPointRepository, times(1)).getEntryPointsId();
    }

    @Test
    public void should_return_500_when_repository_cannot_remove_entry_point_delete_entry_point() {
        // given
        when(entryPointRepository.removeEntrypoint(any(EntryPointKey.class))).thenReturn(Optional.empty());
        when(entryPointRepository.getCurrentConfiguration(any(EntryPointKey.class))).thenReturn(Optional.of(new EntryPoint("default-name", "hapadm", "hapVersion", 0, new HashSet<>(), new HashSet<>(), new HashMap<>())));

        // test
        Response response = entryPointResources.deleteEntrypoint("MY_APP/MY_PLTF");

        // check
        assertThat(response.getStatus()).isEqualTo(INTERNAL_SERVER_ERROR.getStatusCode());
        verify(entryPointRepository, times(1)).getEntryPointsId();
    }

    @Test
    public void try_commit_pending_entry_point_should_send_event_to_subscriber() throws Exception {
        // given
        String entryPointKey = "entryPointKey";
        @SuppressWarnings("unchecked")
        ArgumentCaptor<TryCommitPendingConfigurationEvent> tryCommitPendingEntryPointCaptor = ArgumentCaptor.forClass(TryCommitPendingConfigurationEvent.class);


        // when
        String result = entryPointResources.tryCommitPending(entryPointKey);

        // then
        assertThat(result).isNotNull();
        verify(tryCommitPendingConfigurationSubscriber).onNext(tryCommitPendingEntryPointCaptor.capture());
        assertThat(tryCommitPendingEntryPointCaptor.getValue().getKey().getID()).isEqualTo(entryPointKey);
    }

    @Test
    public void try_commit_current_entry_point_should_send_event_to_subscriber() throws Exception {
        // given
        String entryPointKey = "entryPointKey";
        @SuppressWarnings("unchecked")
        ArgumentCaptor<TryCommitCurrentConfigurationEvent> tryCommitCurrentEntryPointCaptor = ArgumentCaptor.forClass(TryCommitCurrentConfigurationEvent.class);

        // when
        String result = entryPointResources.tryCommitCurrent(entryPointKey);

        // then
        assertThat(result).isNotNull();
        verify(tryCommitCurrentConfigurationSubscriber).onNext(tryCommitCurrentEntryPointCaptor.capture());
        assertThat(tryCommitCurrentEntryPointCaptor.getValue().getKey().getID()).isEqualTo(entryPointKey);
    }

    @Test
    public void register_server_should_send_event_to_subscriber() throws Exception {
        // given
        IncomingEntryPointBackendServerJsonRepresentation incomingEntryPoint = mock(IncomingEntryPointBackendServerJsonRepresentation.class);
        ArgumentCaptor<RegisterServerEvent> registerServerCaptor = ArgumentCaptor.forClass(RegisterServerEvent.class);

        // when
        entryPointResources.registerServer("id", "backend", incomingEntryPoint);

        // then
        verify(registerServerSubscriber).onNext(registerServerCaptor.capture());
        assertThat(registerServerCaptor.getValue()).isNotNull();
        assertThat(registerServerCaptor.getValue().getKey().getID()).isEqualTo("id");
        assertThat(registerServerCaptor.getValue().getBackend()).isEqualTo("backend");
        assertThat(registerServerCaptor.getValue().getServers()).containsExactly(incomingEntryPoint);
    }

    @Test
    public void send_commit_success_should_send_event_to_subscriber() throws Exception {
        // given
        ArgumentCaptor<CommitSuccessEvent> commitSuccessCaptor = ArgumentCaptor.forClass(CommitSuccessEvent.class);

        // when
        entryPointResources.sendCommitSuccess("id", "correlation-id");

        // then
        verify(commitSuccessSubscriber).onNext(commitSuccessCaptor.capture());
        assertThat(commitSuccessCaptor.getValue()).isNotNull();
        assertThat(commitSuccessCaptor.getValue().getKey().getID()).isEqualTo("id");
        assertThat(commitSuccessCaptor.getValue().getCorrelationId()).isEqualTo("correlation-id");
    }

    @Test
    public void send_commit_failure_should_send_event_to_subscriber() throws Exception {
        // given
        ArgumentCaptor<CommitFailureEvent> commitFailureCaptor = ArgumentCaptor.forClass(CommitFailureEvent.class);

        // when
        entryPointResources.sendCommitFailure("id", "correlation-id");

        // then
        verify(commitFailureSubscriber).onNext(commitFailureCaptor.capture());
        assertThat(commitFailureCaptor.getValue()).isNotNull();
        assertThat(commitFailureCaptor.getValue().getKey().getID()).isEqualTo("id");
        assertThat(commitFailureCaptor.getValue().getCorrelationId()).isEqualTo("correlation-id");
    }
}