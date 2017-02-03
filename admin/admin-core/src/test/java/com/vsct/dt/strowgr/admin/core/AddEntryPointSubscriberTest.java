package com.vsct.dt.strowgr.admin.core;

import com.vsct.dt.strowgr.admin.core.configuration.EntryPoint;
import com.vsct.dt.strowgr.admin.core.entrypoint.AddEntryPointEvent;
import com.vsct.dt.strowgr.admin.core.entrypoint.AddEntryPointResponse;
import com.vsct.dt.strowgr.admin.core.entrypoint.AddEntryPointSubscriber;
import com.vsct.dt.strowgr.admin.core.repository.HaproxyRepository;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

public class AddEntryPointSubscriberTest {

    private final EntryPointStateManager stateManager = mock(EntryPointStateManager.class);

    private final HaproxyRepository haproxyRepository = mock(HaproxyRepository.class);

    private final AddEntryPointSubscriber addEntryPointSubscriber = new AddEntryPointSubscriber(stateManager, haproxyRepository);

    private final AddEntryPointEvent event = mock(AddEntryPointEvent.class);

    private final EntryPointKey entryPointKey = mock(EntryPointKey.class);

    private final EntryPoint entryPoint = mock(EntryPoint.class);

    @Test
    public void should_prepare_configuration_if_no_current_or_committing_exists() {
        // given
        when(entryPoint.getHaproxy()).thenReturn("haproxy");
        when(event.getKey()).thenReturn(entryPointKey);
        when(event.getConfiguration()).thenReturn(entryPoint);
        when(haproxyRepository.getHaproxyProperty("haproxy", "platform")).thenReturn(Optional.of("test"));
        when(stateManager.lock(entryPointKey)).thenReturn(true);
        when(stateManager.getCommittingConfiguration(entryPointKey)).thenReturn(Optional.empty());
        when(stateManager.getCurrentConfiguration(entryPointKey)).thenReturn(Optional.empty());
        when(stateManager.prepare(entryPointKey, entryPoint)).thenReturn(Optional.of(entryPoint));

        // when
        addEntryPointSubscriber.accept(event);

        // then
        verify(stateManager).setAutoreload(entryPointKey, true);
        verify(stateManager).release(entryPointKey);
        ArgumentCaptor<AddEntryPointResponse> captor = ArgumentCaptor.forClass(AddEntryPointResponse.class);
        verify(event).onSuccess(captor.capture());
        AddEntryPointResponse addEntryPointResponse = captor.getValue();
        assertThat(addEntryPointResponse).isNotNull();
        assertThat(addEntryPointResponse.getKey()).isEqualTo(entryPointKey);
        assertThat(addEntryPointResponse.getCorrelationId()).isEqualTo(entryPointKey.getID());
        assertThat(addEntryPointResponse.getConfiguration()).isEqualTo(entryPoint);
    }

    @Test
    public void should_call_on_error_if_entry_point_could_not_be_prepared() {
        // given
        when(entryPoint.getHaproxy()).thenReturn("haproxy");
        when(event.getKey()).thenReturn(entryPointKey);
        when(event.getConfiguration()).thenReturn(entryPoint);
        when(haproxyRepository.getHaproxyProperty("haproxy", "platform")).thenReturn(Optional.of("test"));
        when(stateManager.lock(entryPointKey)).thenReturn(true);
        when(stateManager.getCommittingConfiguration(entryPointKey)).thenReturn(Optional.empty());
        when(stateManager.getCurrentConfiguration(entryPointKey)).thenReturn(Optional.empty());
        when(stateManager.prepare(entryPointKey, entryPoint)).thenReturn(Optional.empty());

        // when
        addEntryPointSubscriber.accept(event);

        // then
        verify(stateManager).release(entryPointKey);
        verify(event).onError(any(IllegalStateException.class));
    }

    @Test
    public void should_call_on_error_if_entry_point_committing_exists() {
        // given
        EntryPoint committing = mock(EntryPoint.class);
        when(entryPoint.getHaproxy()).thenReturn("haproxy");
        when(stateManager.lock(entryPointKey)).thenReturn(true);
        when(stateManager.getCommittingConfiguration(entryPointKey)).thenReturn(Optional.of(committing));
        when(stateManager.getCurrentConfiguration(entryPointKey)).thenReturn(Optional.empty());
        when(haproxyRepository.getHaproxyProperty("haproxy", "platform")).thenReturn(Optional.of("test"));

        // when
        addEntryPointSubscriber.accept(event);

        // then
        verify(stateManager, never()).prepare(any(), any());
        verify(event).onError(any(IllegalStateException.class));
    }

    @Test
    public void should_call_on_error_if_current_entry_point_exists() {
        // given
        EntryPoint current = mock(EntryPoint.class);
        when(entryPoint.getHaproxy()).thenReturn("haproxy");
        when(stateManager.lock(entryPointKey)).thenReturn(true);
        when(stateManager.getCommittingConfiguration(entryPointKey)).thenReturn(Optional.empty());
        when(stateManager.getCurrentConfiguration(entryPointKey)).thenReturn(Optional.of(current));
        when(haproxyRepository.getHaproxyProperty("haproxy", "platform")).thenReturn(Optional.of("test"));

        // when
        addEntryPointSubscriber.accept(event);

        // then
        verify(stateManager, never()).prepare(any(), any());
        verify(event).onError(any(IllegalStateException.class));
    }

    @Test
    public void should_call_on_error_if_lock_can_not_be_obtained() {
        // given
        when(entryPoint.getHaproxy()).thenReturn("haproxy");
        when(haproxyRepository.getHaproxyProperty("haproxy", "platform")).thenReturn(Optional.of("test"));
        when(stateManager.lock(entryPointKey)).thenReturn(false);

        // when
        addEntryPointSubscriber.accept(event);

        // then
        verify(stateManager, never()).prepare(any(), any());
        verify(event).onError(any(IllegalStateException.class));
    }

}