package com.vsct.dt.strowgr.admin.core.entrypoint;

import com.vsct.dt.strowgr.admin.core.EntryPointKey;
import com.vsct.dt.strowgr.admin.core.EntryPointStateManager;
import com.vsct.dt.strowgr.admin.core.configuration.EntryPoint;
import com.vsct.dt.strowgr.admin.core.event.in.UpdatedEntryPoint;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import java.util.Optional;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

public class UpdateEntryPointSubscriberTest {

    private final EntryPointStateManager stateManager = mock(EntryPointStateManager.class);

    private final UpdateEntryPointSubscriber updateEntryPointSubscriber = new UpdateEntryPointSubscriber(stateManager);

    private final EntryPointKey entryPointKey = mock(EntryPointKey.class);

    private final UpdateEntryPointEvent event = mock(UpdateEntryPointEvent.class);

    private final UpdatedEntryPoint updatedEntryPoint = mock(UpdatedEntryPoint.class);

    @Before
    public void setUp() throws Exception {
        when(event.getKey()).thenReturn(entryPointKey);
        when(event.getUpdatedEntryPoint()).thenReturn(updatedEntryPoint);
    }

    @Test
    public void should_update_configuration_when_entry_point_has_pending_configuration() {
        // given
        EntryPoint pending = mock(EntryPoint.class);
        EntryPoint expected = mock(EntryPoint.class);
        when(pending.mergeWithUpdate(updatedEntryPoint)).thenReturn(expected);
        when(stateManager.lock(entryPointKey)).thenReturn(true);
        when(stateManager.getCommittingConfiguration(entryPointKey)).thenReturn(Optional.empty());
        when(stateManager.getPendingConfiguration(entryPointKey)).thenReturn(Optional.of(pending));
        when(stateManager.getCurrentConfiguration(entryPointKey)).thenReturn(Optional.empty());
        when(stateManager.prepare(entryPointKey, expected)).thenReturn(Optional.of(expected));

        // when
        updateEntryPointSubscriber.accept(event);

        // then
        verify(stateManager).prepare(entryPointKey, expected);
        ArgumentCaptor<UpdateEntryPointResponse> captor = ArgumentCaptor.forClass(UpdateEntryPointResponse.class);
        verify(event).onSuccess(captor.capture());
    }

    @Test
    public void should_update_configuration_when_entry_point_has_committing_configuration() {
        // given
        EntryPoint committing = mock(EntryPoint.class);
        EntryPoint expected = mock(EntryPoint.class);
        when(committing.mergeWithUpdate(updatedEntryPoint)).thenReturn(expected);
        when(stateManager.lock(entryPointKey)).thenReturn(true);
        when(stateManager.getCommittingConfiguration(entryPointKey)).thenReturn(Optional.of(committing));
        when(stateManager.getPendingConfiguration(entryPointKey)).thenReturn(Optional.empty());
        when(stateManager.getCurrentConfiguration(entryPointKey)).thenReturn(Optional.empty());
        when(stateManager.prepare(entryPointKey, expected)).thenReturn(Optional.of(expected));

        // when
        updateEntryPointSubscriber.accept(event);

        // then
        verify(stateManager).prepare(entryPointKey, expected);
        ArgumentCaptor<UpdateEntryPointResponse> captor = ArgumentCaptor.forClass(UpdateEntryPointResponse.class);
        verify(event).onSuccess(captor.capture());
    }

    @Test
    public void should_update_configuration_when_entry_point_has_no_pending_and_no_committing_configuration() {
        // given
        EntryPoint current = mock(EntryPoint.class);
        EntryPoint expected = mock(EntryPoint.class);
        when(current.mergeWithUpdate(updatedEntryPoint)).thenReturn(expected);
        when(stateManager.lock(entryPointKey)).thenReturn(true);
        when(stateManager.getCommittingConfiguration(entryPointKey)).thenReturn(Optional.empty());
        when(stateManager.getPendingConfiguration(entryPointKey)).thenReturn(Optional.empty());
        when(stateManager.getCurrentConfiguration(entryPointKey)).thenReturn(Optional.of(current));
        when(stateManager.prepare(entryPointKey, expected)).thenReturn(Optional.of(expected));

        // when
        updateEntryPointSubscriber.accept(event);

        // then
        verify(stateManager).prepare(entryPointKey, expected);
    }

    @Test
    public void should_call_on_error_when_entry_point_could_not_be_updated() {
        // given
        EntryPoint current = mock(EntryPoint.class);
        EntryPoint expected = mock(EntryPoint.class);
        when(stateManager.lock(entryPointKey)).thenReturn(true);
        when(stateManager.getCommittingConfiguration(entryPointKey)).thenReturn(Optional.empty());
        when(stateManager.getPendingConfiguration(entryPointKey)).thenReturn(Optional.empty());
        when(stateManager.getCurrentConfiguration(entryPointKey)).thenReturn(Optional.of(current));
        when(current.mergeWithUpdate(updatedEntryPoint)).thenReturn(expected);
        when(stateManager.prepare(entryPointKey, expected)).thenReturn(Optional.empty());

        // when
        updateEntryPointSubscriber.accept(event);

        // then
        verify(event).onError(any(IllegalStateException.class));
    }

    @Test
    public void should_call_on_error_when_entry_point_has_no_configuration_at_all() {
        // given
        when(stateManager.lock(entryPointKey)).thenReturn(true);
        when(stateManager.getCommittingConfiguration(entryPointKey)).thenReturn(Optional.empty());
        when(stateManager.getPendingConfiguration(entryPointKey)).thenReturn(Optional.empty());
        when(stateManager.getCurrentConfiguration(entryPointKey)).thenReturn(Optional.empty());

        // when
        updateEntryPointSubscriber.accept(event);

        // then
        verify(stateManager, never()).prepare(any(), any());
        verify(event).onError(any(IllegalStateException.class));
    }

    @Test
    public void should_call_on_error_when_lock_can_not_be_acquired() {
        // given
        when(stateManager.lock(entryPointKey)).thenReturn(false);

        // when
        updateEntryPointSubscriber.accept(event);

        // then
        verify(stateManager, never()).prepare(any(), any());
        verify(event).onError(any(IllegalStateException.class));
    }

}