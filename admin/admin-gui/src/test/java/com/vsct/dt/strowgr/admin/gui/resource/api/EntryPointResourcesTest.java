package com.vsct.dt.strowgr.admin.gui.resource.api;

import com.google.common.eventbus.EventBus;
import com.vsct.dt.strowgr.admin.core.*;
import com.vsct.dt.strowgr.admin.core.configuration.EntryPoint;
import com.vsct.dt.strowgr.admin.core.entrypoint.AddEntryPointEvent;
import com.vsct.dt.strowgr.admin.core.entrypoint.AddEntryPointResponse;
import com.vsct.dt.strowgr.admin.core.entrypoint.AutoReloadConfigEvent;
import com.vsct.dt.strowgr.admin.core.entrypoint.AutoReloadConfigResponse;
import com.vsct.dt.strowgr.admin.core.event.out.DeleteEntryPointEvent;
import com.vsct.dt.strowgr.admin.core.repository.EntryPointRepository;
import com.vsct.dt.strowgr.admin.gui.mapping.json.EntryPointMappingJson;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.reactivestreams.Subscriber;

import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.core.Response;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Optional;

import static javax.ws.rs.core.Response.Status.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

public class EntryPointResourcesTest {

    private final EntryPointRepository entryPointRepository = mock(EntryPointRepository.class);

    private final EventBus eventBus = mock(EventBus.class);

    @SuppressWarnings("unchecked")
    private final Subscriber<AutoReloadConfigEvent> autoReloadConfigSubscriber = mock(Subscriber.class);

    @SuppressWarnings("unchecked")
    private final Subscriber<AddEntryPointEvent> addEntryPointSubscriber = mock(Subscriber.class);

    @SuppressWarnings("unchecked")
    private ArgumentCaptor<AutoReloadConfigEvent> autoReloadEventCaptor = (ArgumentCaptor) ArgumentCaptor.forClass(AutoReloadConfigEvent.class);

    @SuppressWarnings("unchecked")
    private ArgumentCaptor<AddEntryPointEvent> addEntryPointEventCaptor = (ArgumentCaptor) ArgumentCaptor.forClass(AutoReloadConfigEvent.class);

    private final EntryPointResources entryPointResources = new EntryPointResources(eventBus, entryPointRepository, autoReloadConfigSubscriber, addEntryPointSubscriber);

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
        assertThat(responseCaptor.getValue().getStatus()).isEqualTo(Response.Status.PARTIAL_CONTENT.getStatusCode());
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
        assertThat(responseCaptor.getValue().getStatus()).isEqualTo(Response.Status.CREATED.getStatusCode());
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
    public void should_send_delete_entrypoint_event_and_return_204_when_delete_an_entrypoint() {
        // given
        when(entryPointRepository.removeEntrypoint(any(EntryPointKey.class))).thenReturn(Optional.of(Boolean.TRUE));
        when(entryPointRepository.getCurrentConfiguration(any(EntryPointKey.class))).thenReturn(Optional.of(new EntryPoint("default-name", "hapadm", "hapVersion", 0, new HashSet<>(), new HashSet<>(), new HashMap<>())));

        // test
        Response response = entryPointResources.deleteEntrypoint("MY_APP/MY_PLTF");

        // check
        assertThat(response.getStatus()).isEqualTo(OK.getStatusCode());
        verify(eventBus).post(any(DeleteEntryPointEvent.class));
        verify(entryPointRepository, times(1)).getEntryPointsId();
    }

    @Test
    public void should_not_post_delete_event_and_return_404_when_delete_an_non_existing_entrypoint() {
        // given
        when(entryPointRepository.getCurrentConfiguration(any(EntryPointKey.class))).thenReturn(Optional.of(new EntryPoint("default-name", "hapadm", "hapVersion", 0, new HashSet<>(), new HashSet<>(), new HashMap<>())));
        when(entryPointRepository.getCurrentConfiguration(any(EntryPointKey.class))).thenReturn(Optional.empty());

        // test
        Response response = entryPointResources.deleteEntrypoint("MY_APP/MY_PLTF");

        // check
        assertThat(response.getStatus()).isEqualTo(NOT_FOUND.getStatusCode());
        verify(eventBus, times(0)).post(any(DeleteEntryPointEvent.class));
        verify(entryPointRepository, times(1)).getEntryPointsId();
    }

    @Test
    public void should_return_500_when_repository_cannot_remove_entrypoint_delete_entrypoint() {
        // given
        when(entryPointRepository.removeEntrypoint(any(EntryPointKey.class))).thenReturn(Optional.empty());
        when(entryPointRepository.getCurrentConfiguration(any(EntryPointKey.class))).thenReturn(Optional.of(new EntryPoint("default-name", "hapadm", "hapVersion", 0, new HashSet<>(), new HashSet<>(), new HashMap<>())));

        // test
        Response response = entryPointResources.deleteEntrypoint("MY_APP/MY_PLTF");

        // check
        assertThat(response.getStatus()).isEqualTo(INTERNAL_SERVER_ERROR.getStatusCode());
        verify(entryPointRepository, times(1)).getEntryPointsId();
    }
}