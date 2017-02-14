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
package com.vsct.dt.strowgr.admin.core;

import com.vsct.dt.strowgr.admin.core.entrypoint.AutoReloadConfigEvent;
import com.vsct.dt.strowgr.admin.core.entrypoint.AutoReloadConfigResponse;
import com.vsct.dt.strowgr.admin.core.entrypoint.AutoReloadConfigSubscriber;
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