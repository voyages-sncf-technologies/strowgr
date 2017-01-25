package com.vsct.dt.strowgr.admin.core;

import org.junit.Test;
import org.mockito.InOrder;
import org.mockito.Mockito;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

public class AutoReloadConfigSubscriberTest {

    private final EntryPointStateManager stateManager = mock(EntryPointStateManager.class);

    private final AutoReloadConfigSubscriber autoReloadEventConsumer = new AutoReloadConfigSubscriber(stateManager);

    private final EntryPointKey entryPointKey = new EntryPointKeyDefaultImpl("entryPointKey");

    @SuppressWarnings("unchecked")
    private final AutoReloadConfigEvent autoReloadRequestEventObserver = mock(AutoReloadConfigEvent.class);

    @Test
    public void should_call_on_success_when_swap_succeeds() throws Exception {
        // given
        when(autoReloadRequestEventObserver.getKey()).thenReturn(entryPointKey);
        when(stateManager.lock(entryPointKey)).thenReturn(true);
        when(stateManager.isAutoreloaded(entryPointKey)).thenReturn(true);

        // when
        autoReloadEventConsumer.accept(autoReloadRequestEventObserver);

        // then
        verify(autoReloadRequestEventObserver).onSuccess(any(AutoReloadConfigResponse.class));
        InOrder inOrder = Mockito.inOrder(stateManager);
        inOrder.verify(stateManager).lock(entryPointKey);
        inOrder.verify(stateManager).setAutoreload(entryPointKey, false);
        inOrder.verify(stateManager).release(entryPointKey);
    }

    @Test
    public void should_call_on_error_when_locking_is_not_acquired() throws Exception {
        // given
        when(autoReloadRequestEventObserver.getKey()).thenReturn(entryPointKey);
        when(stateManager.lock(entryPointKey)).thenReturn(false);

        // when
        autoReloadEventConsumer.accept(autoReloadRequestEventObserver);

        // then
        verify(autoReloadRequestEventObserver).onError(any(IllegalStateException.class));
        InOrder inOrder = Mockito.inOrder(stateManager);
        inOrder.verify(stateManager).lock(entryPointKey);
        inOrder.verify(stateManager, never()).setAutoreload(eq(entryPointKey), anyBoolean());
        inOrder.verify(stateManager).release(entryPointKey);
    }

    @Test
    public void should_call_on_error_when_swap_fails() throws Exception {
        // given
        when(autoReloadRequestEventObserver.getKey()).thenReturn(entryPointKey);
        when(stateManager.lock(entryPointKey)).thenReturn(true);
        RuntimeException runtimeException = new RuntimeException();
        when(stateManager.isAutoreloaded(entryPointKey)).thenThrow(runtimeException);

        // when
        autoReloadEventConsumer.accept(autoReloadRequestEventObserver);

        // then
        verify(autoReloadRequestEventObserver).onError(runtimeException);
        InOrder inOrder = Mockito.inOrder(stateManager);
        inOrder.verify(stateManager).lock(entryPointKey);
        inOrder.verify(stateManager, never()).setAutoreload(eq(entryPointKey), anyBoolean());
        inOrder.verify(stateManager).release(entryPointKey);
    }

}