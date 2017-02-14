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
package com.vsct.dt.strowgr.admin.gui.observable;

import com.vsct.dt.strowgr.admin.core.configuration.EntryPoint;
import com.vsct.dt.strowgr.admin.core.event.out.CommitRequestedEvent;
import com.vsct.dt.strowgr.admin.nsq.producer.NSQDispatcher;
import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;

import static org.mockito.Mockito.*;

public class CommitRequestedSubscriberTest {

    private final NSQDispatcher nsqDispatcher = mock(NSQDispatcher.class);

    private final CommitRequestedSubscriber commitRequestedSubscriber = new CommitRequestedSubscriber(nsqDispatcher);

    private final EntryPoint configuration = mock(EntryPoint.class);

    private final HashMap<String, String> context = new HashMap<String, String>() {{
        put("application", "app");
        put("platform", "ptf");
    }};

    private final CommitRequestedEvent commitRequestedEvent = mock(CommitRequestedEvent.class);

    @Before
    public void setUp() throws Exception {
        when(configuration.getContext()).thenReturn(context);
        when(configuration.getHaproxy()).thenReturn("hap");
        when(commitRequestedEvent.getConfiguration()).thenReturn(configuration);
        when(commitRequestedEvent.getBind()).thenReturn("bind");
    }

    @Test
    public void should_send_event_to_dispatcher() throws Exception {
        // when
        commitRequestedSubscriber.accept(commitRequestedEvent);

        // then
        verify(nsqDispatcher).sendCommitRequested(commitRequestedEvent, "hap", "app", "ptf", "bind");
    }

    @Test
    public void should_handle_exceptions_from_dispatcher() throws Exception {
        // given
        doThrow(new RuntimeException()).when(nsqDispatcher).sendCommitRequested(commitRequestedEvent, "hap", "app", "ptf", "bind");

        // when
        commitRequestedSubscriber.accept(commitRequestedEvent);

        // then no exception is thrown
    }

    @Test
    public void should_not_send_event_to_dispatcher_when_application_is_null() throws Exception {
        // given
        context.put("application", null);
        doThrow(new RuntimeException()).when(nsqDispatcher).sendCommitRequested(commitRequestedEvent, "hap", "app", "ptf", "bind");

        // when
        commitRequestedSubscriber.accept(commitRequestedEvent);

        // then no exception is thrown
        verify(nsqDispatcher, never()).sendCommitRequested(any(), anyString(), anyString(), anyString(), anyString());
    }

    @Test
    public void should_not_send_event_to_dispatcher_when_platform_is_null() throws Exception {
        // given
        context.put("platform", null);
        doThrow(new RuntimeException()).when(nsqDispatcher).sendCommitRequested(commitRequestedEvent, "hap", "app", "ptf", "bind");

        // when
        commitRequestedSubscriber.accept(commitRequestedEvent);

        // then no exception is thrown
        verify(nsqDispatcher, never()).sendCommitRequested(any(), anyString(), anyString(), anyString(), anyString());
    }
}